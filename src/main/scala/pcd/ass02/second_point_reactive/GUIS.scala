package pcd.ass02.second_point_reactive

import pcd.ass02.second_point_reactive.Analyzer.*

import java.io.File
import scala.swing.{BorderPanel, Button, Dimension, FileChooser, FlowPanel, Label, MainFrame, Panel, SimpleSwingApplication, event}
import java.util.concurrent.atomic.AtomicInteger

object Example extends SimpleSwingApplication {
  def top = new GUIS()
}

class GUIS extends MainFrame {
  val WIDTH = 800; val HEIGHT = 600; private val hSpacing = 10

  private val classCounter = AtomicInteger(0)
  private val depCounter   = AtomicInteger(0)

  type Node = Any
  type Edge = Any
  type Graph = Any
  private lazy val graph: Graph = ???
  extension (g: Graph)
    private def addMyEdge(idFromTo: (String, ClassName | String, ClassName | String)): Edge = ???
    private def addMyEdge(fromTo: (ClassName | String, ClassName | String)): Edge = ???
    private def addMyNode(id: ClassName | String): Node = ???
    private def getNode(id: ClassName | String): Option[Node] = ???
    private def getEdge(id: ClassName | String): Option[Edge] = ???
    private def clear(): Unit = ???

  private def edgeIdFormat(from: ClassName | String, to: ClassName | String): String = s"$from->$to"
  private def drawClassNode(ci: ClassInfo): Unit =
    val node = graph addMyNode ci.name
    if (ci.packageName.nonEmpty)
      graph getNode ci.packageName match
        case Some(n) => graph addMyEdge (ci.packageName, ci.name)
        case None    => graph addMyNode ci.packageName
  private def drawDependency(from: ClassName, to: ClassName): Unit =
    graph addMyNode to
    val edgeId: String = edgeIdFormat(from, to)
    graph getEdge edgeId match
      case None => graph addMyEdge (edgeId, from, to)
      case _    => ()
  private def createGraphPane(): Panel =
    new Panel {
      override def paintComponent(g: scala.swing.Graphics2D): Unit =
        super.paintComponent(g)
        ???
      preferredSize = new Dimension(WIDTH - 20, HEIGHT - 100)
    }
  val lblDir      = Label("No folder selected")
  val btnDir      = new Button("Select Source")
  val btnRun      = new Button("Analyze")
  val lblClasses  = Label("Classes: 0")
  val lblDeps     = Label("Dependencies: 0")
  val topBar      = new FlowPanel(FlowPanel.Alignment.Left)(btnDir, lblDir, btnRun, lblClasses, lblDeps)
  topBar.hGap = hSpacing

  val graphPane: Panel = createGraphPane()

  val root: BorderPanel = new BorderPanel {
    layout(topBar) = BorderPanel.Position.North
    layout(graphPane) = BorderPanel.Position.Center
  }

  contents = root

  private def drawPackageInfo(pi: PackageInfo): Unit =
    pi.log()
    pi.classInfos subscribe { (ci: ClassInfo) =>
      lblClasses.text = s"Classes: ${classCounter.incrementAndGet()}"
      lblDeps.text = s"Dependencies: ${depCounter addAndGet ci.dependencies.size}"
      drawClassNode(ci)
      ci.dependencies foreach{drawDependency(ci.name, _)}
      graphPane.repaint()
    }

  def reset(): Unit =
    graph.clear()
    classCounter set 0
    depCounter set 0
    graphPane.repaint()

  private def initBtnRun(file: File): Unit =
    btnRun.reactions += {
      case event.ButtonClicked(_) =>
        reset()
        scanProject(file) subscribe drawPackageInfo
    }

  btnDir.reactions += {
    case event.ButtonClicked(_) =>
      val chooser = new FileChooser(new File(".")) {
        title = "Seleziona una Directory"
        fileSelectionMode = FileChooser.SelectionMode.DirectoriesOnly
      }
      chooser.showOpenDialog(null) match
        case FileChooser.Result.Approve =>
          val selectedFile = chooser.selectedFile
          lblDir.text = s"Directory selezionata: ${selectedFile.getAbsolutePath}"
          initBtnRun(selectedFile)
        case _ => lblDir.text = "Selezione directory annullata"
  }

  size = new Dimension(WIDTH, HEIGHT)
  title = "Dependency Analyzer"
  open()
}