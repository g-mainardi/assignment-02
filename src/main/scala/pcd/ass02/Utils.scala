package pcd.ass02

import com.github.javaparser.{JavaParser, ParserConfiguration}
import ParserConfiguration.LanguageLevel.JAVA_14
import com.github.javaparser.ast.CompilationUnit

import java.io.File
import java.nio.file.Path
import scala.jdk.CollectionConverters.*

object Utils {
  private val config: ParserConfiguration = new ParserConfiguration() setLanguageLevel JAVA_14
  val parser = new JavaParser(config)

  type ClassName = String

  def getClassName(f: File): ClassName = f.getName stripSuffix ".java"
  def getDependencies(unit: CompilationUnit): List[ClassName] = unit.getImports.asScala.toList map (_.getNameAsString)
  def isVisible(p: Path): Boolean = !(p.iterator.asScala map (_.toString) exists hasPoints)
  def isJavaFile(file: File): Boolean = file.isFile && (file.getName endsWith ".java")
  def getPackagesPath(it: Iterator[Path]): Set[File] = (it filter isVisible map (_.toFile) filter isPackage).toSet

  private def hasPoints(s: String) = (s != ".") && (s startsWith ".")
  private def isPackage(f: File): Boolean = f.isDirectory && (f listFiles isJavaFile).nonEmpty
}
