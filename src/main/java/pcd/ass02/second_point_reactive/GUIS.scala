package pcd.ass02.second_point_reactive

import Analyzer.{ClassInfo, PackageInfo, scanProject}
import javafx.application.Application
import javafx.event.ActionEvent
import javafx.stage.{DirectoryChooser, Stage}
import javafx.scene.{Scene, control, layout, shape, input}
import input.ScrollEvent.SCROLL
import control.*
import layout.{BorderPane, HBox, Pane}
import shape.Circle

class GUIS extends Application {
  val WIDTH = 800
  val HEIGHT = 600
  val hSpacing = 10

  private lazy val graphPane = createGraphPane()
  private var dragStartX = 0.0
  private var dragStartY = 0.0
  private def createGraphPane(): Pane =
    val pane = Pane()
    pane addEventFilter (SCROLL, e =>
      val zoomFactor = if (e.getDeltaY > 0) 1.1 else 0.9
      pane setScaleX(pane.getScaleX * zoomFactor)
      pane setScaleY(pane.getScaleY * zoomFactor)
      e.consume()
    )
    pane.setOnMousePressed {e =>
      dragStartX = e.getSceneX - pane.getTranslateX
      dragStartY = e.getSceneY - pane.getTranslateY
    }
    pane setOnMouseDragged {e =>
      pane setTranslateX(e.getSceneX - dragStartX)
      pane setTranslateY(e.getSceneY - dragStartY)
    }
    pane

  private val radius = 30
  private val pos_x, pos_y = 100
  private def drawClassNode(ci: ClassInfo, index: Int): Unit =
    val circle = Circle(radius)
    circle setLayoutX {pos_x + index * pos_x}
    circle setLayoutY pos_y
    circle setStyle "-fx-fill: lightblue;"
    val label = Label(ci.name)
    label setLayoutX {circle.getLayoutX - 20}
    label setLayoutY {circle.getLayoutY - 10}
    graphPane.getChildren addAll(circle, label)
  override def start(primaryStage: Stage): Unit =
    val btnDir = Button("Select Source")
    val lblDir = Label("No folder selected")
    val btnRun = Button("Analyze")
    val lblClasses = Label("Classes: 0")
    val lblDeps = Label("Dependencies: 0")
    val topBar = HBox(hSpacing, btnDir, lblDir, btnRun, lblClasses, lblDeps)
    val root = BorderPane()
    root setTop topBar
    root setCenter graphPane

    def drawPackageInfo(pi: PackageInfo): Unit = pi.log() //todo Implement real drawing

    def drawPackageInfo(pi: PackageInfo): Unit =
      pi.log()
      pi.classInfos subscribe { ci => drawClassNode(ci, index = 0)}
    btnDir setOnAction {_ =>
      Option(DirectoryChooser() showDialog primaryStage) match
        case Some(sel) =>
          lblDir setText sel.getAbsolutePath
          btnRun setOnAction {_ => scanProject(sel) subscribe drawPackageInfo}
        case _ => throw IllegalArgumentException("File selection failed!")
    }
    primaryStage setScene Scene(root, WIDTH, HEIGHT)
    primaryStage setTitle "Dependency Analyzer"
    primaryStage.show()
}