package pcd.ass02.second_point_reactive

import Analyzer.{ClassInfo, ClassName, PackageInfo, scanProject}
import com.brunomnsilva.smartgraph.graph.{Digraph, DigraphEdgeList, Edge}
import com.brunomnsilva.smartgraph.graphview.{SmartCircularSortedPlacementStrategy, SmartGraphPanel, SmartRadiusProvider, SmartShapeTypeProvider}
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import javafx.application.Platform.runLater
import javafx.application.{Application, Platform}
import javafx.scene.control.*
import javafx.scene.layout.{BorderPane, HBox}
import javafx.scene.{Scene, control, layout}
import javafx.stage.{DirectoryChooser, Stage}

import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import scala.jdk.CollectionConverters.*

/**
 * Add methods, styles and definitions to the smartgraph Library Class.
 */
object GraphUtils {
  enum VertexType(val shape: String, val radius: Double) {
    case PACKAGE    extends VertexType("SQUARE",   9.0)
    case CLASS      extends VertexType("CIRCLE",   6.0)
    case DEPENDENCY extends VertexType("TRIANGLE", 4.5)
  }
  import VertexType.*
  case class MyNode(id: String, vertexType: VertexType) {
    override def toString: String = vertexType match
    case CLASS => (id split "\\.").lastOption getOrElse id
    case _     => id
  }
  private def edgeIdFormat(from: ClassName | String, to: ClassName | String): String = s"$from->$to"
  extension (g: Digraph[MyNode, String])
    private def getVertex(e: MyNode): Option[MyNode] = g.vertices().asScala.map(_.element()).toSet find (e equals _)
    private def getEdge(e: String): Option[Edge[String, MyNode]] = g.edges().asScala find (_.element() equals e)
    private def addMyEdge(id: String, from: MyNode, to: MyNode): MyNode = getEdge(id) match
      case Some(e) => to
      case _       => g.insertEdge(from, to, id); to
    private def addMyNode(name: String, vType: VertexType): MyNode =
      val node = MyNode(name, vType)
      getVertex(node) match
        case Some(n) => n
        case _ => (g insertVertex node).element()
    /**
     * Create the Edge between the two Nodes and returns the destination one.
     */
    def addMyEdge(from: MyNode, to: MyNode): MyNode = g.addMyEdge(edgeIdFormat(from.id, to.id), from, to)
    /**
     * Add a Node of Package type and returns it, if it already exists, then it's yield.
     */
    def addPackage(name: String): MyNode = addMyNode(name, PACKAGE)
    /**
     * Add a Node of Class type and returns it, if it already exists, then it's yield.
     */
    def addClass(name: ClassName): MyNode = addMyNode(name.toString, CLASS)
    /**
     * Add a Node of Dependency type and returns it, if it already exists, then it's yield.
     */
    def addDependency(name: ClassName): MyNode = addMyNode(name.toString, DEPENDENCY)
    /**
     * Create a new dynamic Panel View from the Graph with automatic layout and configuration for the nodes
     *  and returns it.
     */
    def createGraphPane: SmartGraphPanel[MyNode, String] =
      val graphView: SmartGraphPanel[MyNode, String] = SmartGraphPanel(g, SmartCircularSortedPlacementStrategy())
      graphView setVertexShapeTypeProvider ((n: MyNode) => n.vertexType.shape)
      graphView setVertexRadiusProvider ((n: MyNode) => n.vertexType.radius)
      graphView
    /**
     * Removes all the Nodes and Edges from the Graph.
     */
    def clear(): Unit =
      g.vertices forEach{g removeVertex _}
      g.edges    forEach{g removeEdge  _}
}
class GUIS extends Application {
  val WIDTH = 800; val HEIGHT = 600; val hSpacing = 10

  private val classCounter = AtomicInteger(0)
  private val depCounter   = AtomicInteger(0)

  private var selectedFile: Option[File] = None
  import GraphUtils.MyNode
  private lazy val graph: Digraph[MyNode, String] = DigraphEdgeList()

  /**
   * Add a Class Node to the Graph, or yield it if it already exists, and link it to the parent Package.
   */
  private def drawClassNode(pkg: MyNode, c: ClassName): MyNode = graph.addMyEdge(pkg, graph addClass c)
  /**
   * Add a Dependency Node to the Graph, or yield it if it already exists, and link it to the parent Class.
   */
  private def drawDependency(from: MyNode, to: ClassName): MyNode = graph.addMyEdge(from, graph addDependency to)

  private trait MyButton extends Button {
    def hide(): Unit = this setVisible false
    def show(): Unit = this setVisible true
  }
  private object RunButton extends MyButton {
    def analyzeLabelAndShow(): Unit = {this setText "Analyze";show()}
    def resetLabelAndShow(): Unit   = {this setText "Reset";show()}
    hide()
  }
  private object LayoutButton extends MyButton {
    def stopLabelAndShow(): Unit = {stopLabel(); show()}
    def startLabel(): Unit = this setText "Start automatic layout"
    def stopLabel(): Unit  = this setText "Stop automatic layout"
    hide()
  }
  private object DirButton extends MyButton {
    this setText "Select Source"
  }

  private def showCompletionAlert(): Unit =
    val alert = new Alert(Alert.AlertType.INFORMATION)
    alert setTitle "Analysis Complete"
    alert setHeaderText "Project scanned successfully!"
    alert setContentText s"Found:\n- ${classCounter.get()} classes\n- ${depCounter.get()} dependencies"
    alert.showAndWait()

  override def start(primaryStage: Stage): Unit =
    val btnDir     = DirButton
    val lblDir     = Label("No folder selected")
    val btnRun     = RunButton
    val btnOnOff   = LayoutButton
    val lblClasses = Label("Classes: 0")
    val lblDeps    = Label("Dependencies: 0")
    val topBar = HBox(hSpacing, btnDir, lblDir, btnRun, lblClasses, lblDeps, btnOnOff)
    val root = BorderPane()
    val graphPane: SmartGraphPanel[MyNode, String] = graph.createGraphPane
    root setTop topBar; root setCenter graphPane

    /**
     * Draw the Package Node and its children (reactively) in the Graph and updates the view.
     */
    def drawPackageInfo(pi: PackageInfo): Unit =
      println(s"Package: ${pi.name}")
      val pkgNode = graph addPackage pi.name
      graphPane.updateAndWait()
      graphPane getStylableVertex pkgNode addStyleClass "packageVertex"
      pi.classInfos subscribe (
        (ci: ClassInfo) =>
          ci.log()
          Platform.runLater { () =>
            lblClasses setText s"Classes: ${classCounter.incrementAndGet()}"
            lblDeps setText s"Dependencies: ${depCounter addAndGet ci.dependencies.size}"
          }
          val cNode = drawClassNode(pkgNode, ci.name)
          ci.dependencies foreach{drawDependency(cNode, _)}
          graphPane.updateAndWait()
          graphPane getStylableVertex cNode addStyleClass "classVertex",
        (err: Throwable) => println(s"CI draw: ${Thread.currentThread().getName} caught ${err.getMessage}"),
        () => ()
      )

    /**
     * Reset the Graph and the counters.
     */
    def reset(): Unit =
      graph.clear()
      graphPane setAutomaticLayout true
      graphPane.update()
      classCounter set 0
      depCounter set 0

    btnOnOff setOnAction {_ =>
      val wasAutomaticLayout = graphPane.automaticLayoutProperty.get()
      graphPane setAutomaticLayout (!wasAutomaticLayout)
      if wasAutomaticLayout
      then btnOnOff.startLabel()
      else btnOnOff.stopLabel()
    }

    btnDir setOnAction {_ =>
      Option(DirectoryChooser() showDialog primaryStage) match
        case Some(sel) =>
          selectedFile = Some(sel)
          btnRun.analyzeLabelAndShow()
          lblDir setText sel.getAbsolutePath
          btnRun setOnAction {_ =>
            btnRun.hide()
            btnDir.hide()
            btnOnOff.stopLabelAndShow()
            reset()
            scanProject(sel) subscribeOn Schedulers.io subscribe (
                (pi: PackageInfo) => drawPackageInfo(pi),
                (err: Throwable) => println(s"PI draw: ${Thread.currentThread().getName} caught ${err.getMessage}"),
                () =>
                  println(s"Scan project of [$sel] successfully completed!")
                  Platform runLater {() => btnRun.resetLabelAndShow(); btnDir.show(); showCompletionAlert()}
              )
          }
        case _ if selectedFile.isEmpty =>
          btnRun.hide()
          btnOnOff.hide()
        case _ => ()
    }
    primaryStage setScene Scene(root, WIDTH, HEIGHT)
    primaryStage setTitle "Dependency Analyzer"
    Observable
      .fromCallable(() => primaryStage.show())
      .doOnNext(_ => println("Stage prepared"))
      .map(_ => graphPane.init())
      .subscribe(_ => println("Graph initialized"))
}