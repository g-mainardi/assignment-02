package pcd.ass02.second_point_reactive

import javafx.application.Application
import javafx.event.ActionEvent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import Analyzer.{PackageInfo, scanProject}

class GUIS extends Application {
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
          btnRun setOnAction {_ => scanProject(sel).subscribe{(pi: PackageInfo) => pi.log()}}
        case _ => throw IllegalArgumentException("File selection failed!")
    }
    primaryStage setScene Scene(root, WIDTH, HEIGHT)
    primaryStage setTitle "Dependency Analyzer"
    primaryStage.show()
}