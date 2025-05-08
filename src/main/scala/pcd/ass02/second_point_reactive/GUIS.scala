package pcd.ass02.second_point_reactive

import pcd.ass02.second_point_reactive.Analyzer.*

import java.io.File
import scala.swing.{BorderPanel, Button, Dimension, FileChooser, FlowPanel, Label, MainFrame, Panel, SimpleSwingApplication, event}
import java.util.concurrent.atomic.AtomicInteger

object Example extends SimpleSwingApplication {
  def top = new GUIS()
}

object MyGraph {
  opaque type Node = Any
  opaque type Edge = Any
  opaque type Graph = Any
  extension (g: Graph)
    private def addMyEdge(idFromTo: (String, ClassName | String, ClassName | String)): Edge = ???
    private def addMyEdge(fromTo: (ClassName | String, ClassName | String)): Edge = ???
    private def addMyNode(id: ClassName | String): Node = ???
    private def getNode(id: ClassName | String): Option[Node] = ???
    private def getEdge(id: ClassName | String): Option[Edge] = ???
    def clear(): Unit = ???
    def drawClassNode(ci: ClassInfo): Unit =
      val node = g addMyNode ci.name
      if (ci.packageName.nonEmpty)
        g getNode ci.packageName match
        case Some(n) => g addMyEdge (ci.packageName, ci.name)
        case None    => g addMyNode ci.packageName
    def drawDependency(from: ClassName, to: ClassName): Unit =
      g addMyNode to
      g addMyEdge(from, to)

  private def edgeIdFormat(from: ClassName | String, to: ClassName | String): String = s"$from->$to"
}

class GUIS extends MainFrame {
  size = new Dimension(800, 600)
  title = "Dependency Analyzer"
  private val lblDir      = Label("No folder selected")
  private val btnDir      = new Button("Select Source")
  private val btnRun      = new Button("Analyze")
  private val lblClasses  = Label("Classes: 0")
  private val lblDeps     = Label("Dependencies: 0")
  private val topBar      = new FlowPanel(FlowPanel.Alignment.Left)(btnDir, lblDir, btnRun, lblClasses, lblDeps)
  topBar.hGap = 10
  private val graphPane: Panel = new Panel {
    override def paintComponent(g: scala.swing.Graphics2D): Unit =
      super.paintComponent(g)
      ???
    preferredSize = new Dimension(size.width - 20, size.height - 100)
  }
  contents = new BorderPanel {
    layout(topBar) = BorderPanel.Position.North
    layout(graphPane) = BorderPanel.Position.Center
  }

  private val classCounter = AtomicInteger(0)
  private val depCounter   = AtomicInteger(0)
  import pcd.ass02.second_point_reactive.MyGraph.*
  private lazy val graph: Graph = ???
  private def drawPackageInfo(pi: PackageInfo): Unit =
    pi.log()
    pi.classInfos subscribe { (ci: ClassInfo) =>
      lblClasses.text = s"Classes: ${classCounter.incrementAndGet()}"
      lblDeps.text = s"Dependencies: ${depCounter addAndGet ci.dependencies.size}"
      graph drawClassNode ci
      ci.dependencies foreach{graph drawDependency(ci.name, _)}
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
      import FileChooser.*
      val chooser = new FileChooser(new File(".")) {
        title = "Seleziona una Directory"
        fileSelectionMode = SelectionMode.DirectoriesOnly
      }
      chooser.showOpenDialog(null) match
        case Result.Approve =>
          val selectedFile = chooser.selectedFile
          lblDir.text = s"Directory selezionata: ${selectedFile.getAbsolutePath}"
          initBtnRun(selectedFile)
        case _ => lblDir.text = "Selezione directory annullata"
  }
  open()
}