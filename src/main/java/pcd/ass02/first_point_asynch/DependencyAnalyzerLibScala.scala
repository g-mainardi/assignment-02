package pcd.ass02.first_point_asynch

import com.github.javaparser.*
import io.vertx.core.{Future, Vertx}

import java.io.File
import java.nio.file.Path
import scala.jdk.CollectionConverters.*

object DependencyAnalyzerLibScala {
  private val vertx = Vertx.vertx
  private val config = new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_14)
  private val parser = new JavaParser(config)

  opaque type Dependency = String
  opaque type ClassDepsReport = List[Dependency]
  opaque type PackageDepsReport = List[ClassDepsReport]
  opaque type ProjectDepsReport = List[PackageDepsReport]

  private def isVisible(path: Path): Boolean =
    path.forEach { part =>
      val name = part.toString
      if (!(name == ".") && name.startsWith(".")) return false
    }
    true

  private def isJavaFile(file: File) = file.getName.endsWith(".java")

  def getClassDependencies(source: File): Future[ClassDepsReport] = vertx.executeBlocking(() => {
    val unit = parser.parse(source).getResult orElseThrow(() => new IllegalArgumentException("Failed to parse [" + source + "]"))
    unit.getImports.asScala.toList.map(_.getName).map(_.toString)
  })

  def getPackageDependencies(source: File): Future[DependencyAnalyzerLib.PackageDepsReport] = ???
  def getPackageDependencies(source: File): Future[PackageDepsReport] = ???

  def getProjectDependencies(source: File): Future[DependencyAnalyzerLib.ProjectDepsReport] = ???
  def getProjectDependencies(source: File): Future[ProjectDepsReport] = ???
}
