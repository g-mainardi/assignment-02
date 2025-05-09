package pcd.ass02.second_point_reactive

import Analyzer.{ClassInfo, ClassName, PackageInfo, scanProject}
import com.brunomnsilva.smartgraph.graph.{Edge, Graph, GraphEdgeList, Vertex}
import com.brunomnsilva.smartgraph.graphview.{SmartCircularSortedPlacementStrategy, SmartGraphPanel}
import io.reactivex.rxjava3.core.{Observable, Scheduler}
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

  private lazy val fxScheduler: Scheduler = Schedulers.from(Platform.runLater(_))

  override def start(primaryStage: Stage): Unit =
    val btnDir     = Button("Select Source")
    val lblDir     = Label("No folder selected")
    val btnRun     = Button("Analyze")
    val btnOnOff   = Button("Start/Stop")
    val lblClasses = Label("Classes: 0")
    val lblDeps    = Label("Dependencies: 0")
    val topBar = HBox(hSpacing, btnDir, lblDir, btnRun, lblClasses, lblDeps, btnOnOff)
    val root = BorderPane()
    val graphPane: SmartGraphPanel[String, String] = createGraphPane()
    root setTop topBar; root setCenter graphPane

    def drawPackageInfo(pi: PackageInfo): Unit =
      println(s"Package: ${pi.name}")
      graph addMyNode pi.name
      graphPane.update()
      pi.classInfos subscribe (
        (ci: ClassInfo) =>
          ci.log()
          Platform.runLater { () =>
            lblClasses setText s"Classes: ${classCounter.incrementAndGet()}"
            lblDeps setText s"Dependencies: ${depCounter addAndGet ci.dependencies.size}"
          }
          drawClassNode(ci, pi.name)
          ci.dependencies foreach{drawDependency(ci.name, _)}
          graphPane.update(),
        (err: Throwable) => println(s"CI draw: ${Thread.currentThread().getName} caught ${err.getMessage}"),
        () => ()
      )

    def reset(): Unit =
//      graph.clear()
      classCounter set 0
      depCounter set 0
    btnOnOff setOnAction {_ =>
      val newValue = !graphPane.automaticLayoutProperty.get()
      println(s"Setting automatic layout to $newValue")
      graphPane setAutomaticLayout newValue
    }

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
    val file = File("INSERT_PATH")
    Observable
      .fromCallable(() => primaryStage.show())
      .doOnNext(_ => println("Stage prepared"))
      .map(_ => graphPane.init())
      .doOnNext(_ => println("Graph initialized"))
      .observeOn(Schedulers.io)
      .subscribe {_ =>
        scanProject(file)
          .subscribe(
            (pi: PackageInfo) => drawPackageInfo(pi),
            (err: Throwable) => println(s"PI draw: ${Thread.currentThread().getName} caught ${err.getMessage}"),
            () => ()
          )
      }
}