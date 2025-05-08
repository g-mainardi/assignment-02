package pcd.ass02.second_point_reactive

import Analyzer.{ClassInfo, ClassName, PackageInfo, scanProject}
import javafx.application.{Application, Platform}
import javafx.stage.{DirectoryChooser, Stage}
import javafx.scene.{Scene, control, layout}
import control.*
import layout.{BorderPane, HBox}
import com.brunomnsilva.smartgraph.graphview.SmartGraphPanel
import com.brunomnsilva.smartgraph.graphview.SmartCircularSortedPlacementStrategy
import com.brunomnsilva.smartgraph.graph.{Edge, Graph, GraphEdgeList, InvalidVertexException, Vertex}

import java.util.concurrent.atomic.AtomicInteger
import scala.jdk.CollectionConverters.*

class GUIS extends Application {
  val WIDTH = 800; val HEIGHT = 600; val hSpacing = 10

  private val classCounter = AtomicInteger(0)
  private val depCounter   = AtomicInteger(0)
  private lazy val graph: Graph[String, String] = GraphEdgeList()
  extension[A] (g: Graph[A, String])
    private def containsVertex(e: A): Boolean = g.vertices().asScala.map(_.element()).toSet.contains(e)
    private def containsEdge(e: String): Boolean = g.edges().asScala.map(_.element()).toSet.contains(e)
    private def addMyEdge(id: String, from: A, to: A): Option[Edge[String, A]] =
      if g.containsEdge(id)
      then None
      else Some(g.insertEdge(from, to, id))
    private def addMyEdge(from: A, to: A): Option[Edge[String, A]] =
      g.addMyEdge(edgeIdFormat(from.toString, to.toString), from, to)
    private def addMyNode(name: A): Option[Vertex[A]] =
      if g.containsVertex(name)
      then None
      else Some(g.insertVertex(name))

  private def edgeIdFormat(from: ClassName | String, to: ClassName | String): String = s"$from->$to"
  private def drawClassNode(ci: ClassInfo, pkg: String): Unit =
    graph addMyNode ci.name.toString match
      case Some(node) => graph.addMyEdge(pkg, ci.name.toString)
      case _          => println(s"Node already present! [${ci.name}]")

  private def drawDependency(from: ClassName, to: ClassName): Unit =
    graph addMyNode to.toString //setAttribute("ui.label", to)
    graph.addMyEdge(from.toString, to.toString)

  private def createGraphPane(): SmartGraphPanel[String, String] =
    val graphView: SmartGraphPanel[String, String] = SmartGraphPanel(graph, SmartCircularSortedPlacementStrategy()) // layout circolare
    graphView.setAutomaticLayout(true); // auto-layout force-directed
    graphView

  override def start(primaryStage: Stage): Unit =
    val btnDir     = Button("Select Source")
    val lblDir     = Label("No folder selected")
    val btnRun     = Button("Analyze")
    val lblClasses = Label("Classes: 0")
    val lblDeps    = Label("Dependencies: 0")
    val topBar = HBox(hSpacing, btnDir, lblDir, btnRun, lblClasses, lblDeps)
    val root = BorderPane()
    val graphPane: SmartGraphPanel[String, String] = createGraphPane()
    root setTop topBar; root setCenter graphPane

    def drawPackageInfo(pi: PackageInfo): Unit =
      pi.log()
      pi.classInfos subscribe { (ci: ClassInfo) =>
        Platform.runLater { () =>
          lblClasses setText s"Classes: ${classCounter.incrementAndGet()}"
          lblDeps setText s"Dependencies: ${depCounter addAndGet ci.dependencies.size}"
      graph addMyNode pi.name
      graphPane.update()
          drawClassNode(ci, pi.name)
          ci.dependencies foreach{drawDependency(ci.name, _)}
          graphPane.update()
        }
      }

    def reset(): Unit =
//      graph.clear()
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
    graphPane.init()

}