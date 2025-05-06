package pcd.ass02.second_point_reactive

import com.github.javaparser.{JavaParser, ParserConfiguration}
import io.reactivex.rxjava3.core.Observable

import java.io.{File, IOException}
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

object Analyzer {
  private val config = new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_14)
  private val parser = new JavaParser(config)
  opaque type Dependency = String
  case class ClassInfo(name: String, dependencies: List[Dependency])
  case class PackageInfo(name: String, classInfos: Observable[ClassInfo]) {
    def log(): Unit =
      val id = PackageId.next()
      println(s"Package: ${this.name}")
      this.classInfos subscribe { (ci: ClassInfo) =>
        println(s"\tClass: ${ci.name.replace(".java", "")}")
        ci.dependencies.foreach(d => println(s"\t\t${d}"))
      }
      println("")
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
    ClassInfo(source.getName, unit.getImports.asScala.toList map (_.getName) map (_.toString))

  private def scanPackage(source: File): Observable[ClassInfo] =
    Observable.create { emitter => Option(source.listFiles) match
        case Some(files) => files.toList filter isJavaFile match
          case filteredFiles if filteredFiles.nonEmpty =>
            filteredFiles foreach { emitter onNext getClassInfo(_) }
            emitter.onComplete()
          case _ => emitter onError IOException("List files empty")
        case _ => emitter onError IOException("List files returned null")
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
      case _ => throw IllegalArgumentException("Not a dir")
    }

  private object PackageId {
    private var count: Int = 0
    def next(): Int =
      val ret = count
      count += 1
      ret
    def reset(): Unit = count = 0
  }
  def main(args: Array[String]): Unit = {
    scanProject(File(".")).subscribe {(pi: PackageInfo) => pi.log()}
    PackageId.reset()
  }
}

