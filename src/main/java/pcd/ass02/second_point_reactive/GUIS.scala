package pcd.ass02.second_point_reactive

import Analyzer.{ClassInfo, ClassName, PackageInfo, scanProject}
import javafx.application.{Application, Platform}
import javafx.stage.{DirectoryChooser, Stage}
import javafx.scene.{Scene, control, layout}
import control.*
import layout.{BorderPane, HBox}
import org.graphstream.graph.{Edge, Node, Graph}
import org.graphstream.graph.implementations.SingleGraph
import org.graphstream.ui.fx_viewer.{FxViewPanel, FxViewer}
import org.graphstream.ui.view.Viewer

import java.util.concurrent.atomic.AtomicInteger

class GUIS extends Application {
  val WIDTH = 800; val HEIGHT = 600; val hSpacing = 10

  private lazy val graph = SingleGraph("Class Dependencies")
  private lazy val graphPane = createGraphPane()
  private def createGraphPane(): FxViewPanel =
    graph setAutoCreate true
    graph setStrict false
    val viewer: Viewer = FxViewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD)
    viewer.enableAutoLayout()
    viewer.addDefaultView(false).asInstanceOf[FxViewPanel]

  private val classCounter = AtomicInteger(0)
  private val depCounter   = AtomicInteger(0)
  extension (g: Graph)
    private def addMyEdge(idFromTo: (String, ClassName | String, ClassName | String)): Edge =
      g addEdge (idFromTo._1, idFromTo._2.toString, idFromTo._3.toString, true)
    private def addMyEdge(fromTo: (ClassName | String, ClassName | String)): Edge =
      g addMyEdge (edgeIdFormat(fromTo._1, fromTo._2), fromTo._1, fromTo._2)
    private def addMyNode(id: ClassName | String): Node = g addNode id.toString

  private def edgeIdFormat(from: ClassName | String, to: ClassName | String): String = s"$from->$to"
  private def drawClassNode(ci: ClassInfo): Unit =
    val node = graph addMyNode ci.name
    node setAttribute("ui.label", ci.name)
    if (ci.packageName.nonEmpty)
      val pkgClass = ci.packageName replace('.', '_')
      node setAttribute("ui.class", pkgClass)
      Option(graph getNode pkgClass) match
        case Some(n) => graph addMyEdge (ci.packageName, ci.name)
        case None    =>
          val pkgNode = graph addMyNode pkgClass
          pkgNode setAttribute("ui.label", ci.packageName)
          pkgNode setAttribute("ui.class", pkgClass)

  private def drawDependency(from: ClassName, to: ClassName): Unit =
    graph addNode to.toString setAttribute("ui.label", to)
    val edgeId: String = edgeIdFormat(from, to)
    Option(graph getEdge edgeId) match
      case None => graph addMyEdge (edgeId, from, to)
      case _    => ()

  override def start(primaryStage: Stage): Unit =
    val btnDir     = Button("Select Source")
    val lblDir     = Label("No folder selected")
    val btnRun     = Button("Analyze")
    val lblClasses = Label("Classes: 0")
    val lblDeps    = Label("Dependencies: 0")
    val topBar = HBox(hSpacing, btnDir, lblDir, btnRun, lblClasses, lblDeps)
    val root = BorderPane()
    root setTop topBar; root setCenter graphPane

    def drawPackageInfo(pi: PackageInfo): Unit =
      pi.log()
      pi.classInfos subscribe { (ci: ClassInfo) =>
        Platform.runLater { () =>
          lblClasses setText s"Classes: ${classCounter.incrementAndGet()}"
          lblDeps setText s"Dependencies: ${depCounter addAndGet ci.dependencies.size}"
          drawClassNode(ci)
          ci.dependencies foreach{drawDependency(ci.name, _)}
        }
      }

    def reset(): Unit =
      graph.clear()
      classCounter set 0
      depCounter set 0

    btnDir setOnAction {_ =>
      Option(DirectoryChooser() showDialog primaryStage) match
        case Some(sel) =>
          lblDir setText sel.getAbsolutePath
          btnRun setOnAction {_ => reset(); scanProject(sel) subscribe drawPackageInfo}
        case _ => throw IllegalArgumentException("Directory selection failed!")
    }
    primaryStage setScene Scene(root, WIDTH, HEIGHT)
    primaryStage setTitle "Dependency Analyzer"
    primaryStage.show()
}