package pcd.ass02.first_point_asynch

import com.github.javaparser.JavaParser
import com.github.javaparser.*
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import io.vertx.core.Future
import io.vertx.core.Vertx

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

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

  def getClassDependencies(source: File): Future[ClassDepsReport] = ???

  def getPackageDependencies(source: File): Future[DependencyAnalyzerLib.PackageDepsReport] = ???

  def getProjectDependencies(source: File): Future[DependencyAnalyzerLib.ProjectDepsReport] = ???
}
