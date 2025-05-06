package pcd.ass02.second_point_reactive

import com.github.javaparser.{JavaParser, ParserConfiguration}
import io.reactivex.rxjava3.core.Observable

import java.io.{File, IOException}
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

object Analyzer {
  private val config = new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_14)
  private val parser = new JavaParser(config)
  opaque type ClassName = String
  /**
   * @param name full class name, e.g. pcd.ass02.MyClass
   * @param dependencies  list of fully-qualified imported names
   * @param packageName   package of the class (dot-separated)
   */
  case class ClassInfo(name: ClassName, dependencies: List[ClassName], packageName: String)
  case class PackageInfo(name: String, classInfos: Observable[ClassInfo]) {
    def log(): Unit =
      val id = PackageId.next()
      println(s"[$id] Package: $name")
      classInfos.subscribe { (ci: ClassInfo) => println(s"[$id]\t${ci.name} -> ${ci.dependencies}") }
      println()
  }
  private def isVisible(p: Path) = !(p.iterator.asScala map(_.toString) exists(n => n != "." && n.startsWith(".")))
  private def isJavaFile(file: File) = file.isFile && (file.getName endsWith ".java")
  private def isPackage(f: File): Boolean = f.isDirectory && f.listFiles(isJavaFile).nonEmpty
  object TestIsPackage {
    private def testIsPackage(s: String): Unit =
      println(s"Is $s a package? ${isPackage(File(s))}")

    testIsPackage(".")
    testIsPackage("src/main")
    testIsPackage("src/main/java")
    testIsPackage("src/main/java/pcd/ass02/first_point_asynch")
    testIsPackage("src/main/java/pcd/ass02/second_point_reactive")
  }

  private def getClassInfo(source: File): ClassInfo =
    val unit = parser.parse(source).getResult orElseThrow (() => IllegalArgumentException("Failed to parse [" + source + "]"))
    val pkgName: String = Option(unit.getPackageDeclaration.get()) match
      case Some(decl) => decl.getNameAsString
      case _          => ""
    val qualified: ClassName = s"$pkgName.${source.getName stripSuffix ".java"}"
    val deps: List[ClassName] = unit.getImports.asScala.toList map(_.getNameAsString)
    ClassInfo(qualified, deps, pkgName)

  private def scanPackage(source: File): Observable[ClassInfo] =
    Observable.create { emitter => Option(source.listFiles) match
        case Some(files) if files.nonEmpty =>
          files filter isJavaFile foreach { emitter onNext getClassInfo(_) }
          emitter.onComplete()
        case _ => emitter onError IOException(s"No Java files in ${source.getAbsolutePath}")
    }

  def scanProject(source: File): Observable[PackageInfo] =
    Observable create { emitter => source match
      case s if s.isDirectory =>
        val stream = Files.walk(s.toPath)
        try
          val dirs = (stream.iterator().asScala filter isVisible map (_.toFile) filter isPackage).toSet
          dirs foreach {dir => emitter onNext PackageInfo(dir.getName, scanPackage(dir))}
        finally
          stream.close()
          emitter.onComplete()
      case _ => emitter onError IllegalArgumentException("Not a dir")
    }

  private object PackageId {
    private var count: Int = 0
    def next(): Int =
      val c = count
      count += 1; c
    def reset(): Unit = count = 0
  }

  def main(args: Array[String]): Unit =
    scanProject(File(".")) subscribe((pi: PackageInfo) => pi.log())
    PackageId.reset()
}

