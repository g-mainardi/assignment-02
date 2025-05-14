package pcd.ass02.second_point_reactive

import io.reactivex.rxjava3.core.Observable
import pcd.ass02.Utils.*

import java.io.{File, IOException}
import java.nio.file.Files
import scala.jdk.CollectionConverters.*

object Analyzer {
  /**
   * @param name          full class name, e.g. pcd.ass02.MyClass
   * @param dependencies  list of fully-qualified imported names
   */
  case class ClassInfo(name: ClassName, dependencies: List[ClassName]) {
    def log(): Unit = {println(s"\tClass: $name"); dependencies foreach {d => println(s"\t\t$d")}}
  }

  /**
   * @param name       full package name, e.g. pcd.ass02.scala.second_point_reactive
   * @param classInfos observable of the classes inside the package
   */
  case class PackageInfo(name: String, classInfos: Observable[ClassInfo]) {
    def log(): Unit =
      println(s"Package: $name")
      classInfos subscribe (
        (ci: ClassInfo) => ci.log(),
        (err: Throwable) => println(s"PI log: ${Thread.currentThread} caught ${err.getMessage}"),
        () => ()
      );println()
  }

  /**
   * @param source path to the folder to be analyzed
   * @return observable of the packages inside the folder
   */
  def scanProject(source: File): Observable[PackageInfo] =
    Observable create { emitter =>
      source match
        case s if s.isDirectory =>
          val stream = Files walk s.toPath
          try
            getPackagesPath(stream.iterator.asScala)
              .foreach { dir => emitter onNext PackageInfo(dir.getName, scanPackage(dir)) }
          finally
            stream.close(); emitter.onComplete()
        case _ => emitter onError IllegalArgumentException(s"This [${source.toPath}] is not a directory")
    }

  private def getClassInfo(source: File): ClassInfo =
    val unit = parser.parse(source).getResult orElseThrow (() => IllegalArgumentException(s"Failed to parse [$source]"))
    val pkgName: String =
      try unit.getPackageDeclaration.get.getNameAsString
      catch case e: NoSuchElementException => throw IllegalArgumentException(s"No package declaration in [$source]")
    ClassInfo(s"$pkgName.${getClassName(source)}", getDependencies(unit))

  private def scanPackage(source: File): Observable[ClassInfo] =
    Observable.create { emitter => Option(source.listFiles) match
      case Some(files) if files.nonEmpty =>
        files filter isJavaFile foreach { emitter onNext getClassInfo(_) }
        emitter.onComplete()
      case _ => emitter onError IOException(s"No Java files in ${source.getAbsolutePath}")
    }

  def main(args: Array[String]): Unit =
    println("Test Analyzer in this project")
    scanProject(File(".")) subscribe((pi: PackageInfo) => pi.log())
}

