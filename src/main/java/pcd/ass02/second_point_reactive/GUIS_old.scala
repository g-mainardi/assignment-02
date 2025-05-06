package pcd.ass02.second_point_reactive

import javafx.application.Application
import javafx.event.ActionEvent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.{BorderPane, HBox}
import javafx.stage.{DirectoryChooser, Stage}
import pcd.ass02.second_point_reactive.Analyzer.{PackageInfo, scanProject}
import java.util
import scala.jdk.CollectionConverters.*

class GUIS_old extends Application {
  final private val engine = DependencyAnalyzer()

  override def start(primaryStage: Stage): Unit =
    val WIDTH = 800
    val HEIGHT = 600
    val hSpacing = 10
    val btnDir = Button("Select Source")
    val lblDir = Label("No folder selected")
    val btnRun = Button("Analyze")
    val lblClasses = Label("Classes: 0")
    val lblDeps = Label("Dependencies: 0")
    val logArea = TextArea()
    logArea setEditable false
    val topBar = HBox(hSpacing, btnDir, lblDir, btnRun, lblClasses, lblDeps)
    val root = BorderPane()
    root setTop topBar
    root setCenter logArea
    btnDir setOnAction {_ =>
      Option(DirectoryChooser() showDialog primaryStage) match
        case Some(sel) =>
          lblDir setText sel.getAbsolutePath
          engine.classCount subscribe((cnt: Integer) => lblClasses.setText("Classes: " + cnt))
          engine.depCount subscribe((cnt: Integer) => lblDeps.setText("Dependencies: " + cnt))
          engine.logBatches subscribe((batch: util.List[String]) => batch.asScala.foreach{logArea.appendText})
          btnRun setOnAction {_ => engine analyze sel.toPath}
          btnRun setOnAction {_ => scanProject(sel) subscribe{(pi: PackageInfo) => pi.log()}}
        case _ => throw IllegalArgumentException("File selection failed!")
    }
    primaryStage setScene Scene(root, WIDTH, HEIGHT)
    primaryStage setTitle "Dependency Analyzer"
    primaryStage.show()
}