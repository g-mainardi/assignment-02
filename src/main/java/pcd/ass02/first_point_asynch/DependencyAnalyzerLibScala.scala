package pcd.ass02.first_point_asynch

import com.github.javaparser.*
import io.vertx.core.{CompositeFuture, Future, Vertx}

import java.io.{File, IOException}
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

object DependencyAnalyzerLibScala {
  private val vertx = Vertx.vertx
  private val config = new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_14)
  private val parser = new JavaParser(config)

  opaque type Dependency = String
  opaque type ClassDepsReport = List[Dependency]
  opaque type PackageDepsReport = List[ClassDepsReport]
  opaque type ProjectDepsReport = List[PackageDepsReport]

  private def isVisible(p: Path) = !(p.iterator.asScala map(_.toString) exists(n => n != "." && n.startsWith(".")))
  private def isJavaFile(file: File) = file.getName.endsWith(".java")

  def getClassDependencies(source: File): Future[ClassDepsReport] = vertx.executeBlocking{() =>
    val unit = parser.parse(source).getResult orElseThrow(() => new IllegalArgumentException("Failed to parse [" + source + "]"))
    unit.getImports.asScala.toList map(_.getName) map(_.toString)
  }

  def getPackageDependencies(source: File): Future[PackageDepsReport] =
    vertx executeBlocking { () =>
      source match
        case s if !s.isDirectory => throw new IllegalArgumentException("This [" + source.toPath + "] is not a package")
        case s => Option(s.listFiles) getOrElse (throw new IOException("List files returned null"))
    } compose { files =>
      val classReports = files filter (_.isFile) filter isJavaFile map getClassDependencies
      Future all classReports.toList.asJava
    } map {_.list.asScala.toList filter ((s: ClassDepsReport) => s.nonEmpty)}

  def getProjectDependencies(source: File): Future[ProjectDepsReport] = vertx executeBlocking { () => source match
    case s if !s.isDirectory => throw new IllegalArgumentException("Not a dir")
    case s =>
      val stream = Files.walk(s.toPath)
      try (stream.iterator().asScala filter isVisible map(_.toFile) filter(_.isDirectory)).toSet
      finally stream.close()
  } compose { (dirs: Set[File]) =>
    val pkgFutures: List[Future[PackageDepsReport]] = dirs.map(getPackageDependencies).toList
    Future all pkgFutures.asJava
  } map { (composite: CompositeFuture) => composite.list[PackageDepsReport]().asScala.toList filter(_.nonEmpty) }
}
