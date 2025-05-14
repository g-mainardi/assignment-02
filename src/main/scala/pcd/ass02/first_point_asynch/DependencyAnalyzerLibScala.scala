package pcd.ass02.first_point_asynch

import io.vertx.core.{CompositeFuture, Future, Vertx}
import pcd.ass02.Utils.*

import java.io.{File, IOException}
import java.nio.file.Files
import scala.jdk.CollectionConverters.*

object DependencyAnalyzerLibScala {
  private val vertx = Vertx.vertx

  opaque type ClassDepsReport   = List[ClassName]
  opaque type PackageDepsReport = List[ClassDepsReport]
  opaque type ProjectDepsReport = List[PackageDepsReport]

  def getClassDependencies(source: File): Future[ClassDepsReport] = vertx executeBlocking { () =>
    val unit = parser.parse(source).getResult orElseThrow(() => IllegalArgumentException(s"Failed to parse [$source]"))
    getDependencies(unit)
  }

  def getPackageDependencies(source: File): Future[PackageDepsReport] = vertx executeBlocking { () => source match
    case s if !s.isDirectory => throw IllegalArgumentException(s"This [${s.toPath}] is not a package")
    case s                   => Option(s.listFiles) getOrElse (throw IOException("List files returned null"))
  } compose { files =>
    Future all (files filter isJavaFile map getClassDependencies).toList.asJava
  } map { _.list[ClassDepsReport].asScala.toList filter (_.nonEmpty) }

  def getProjectDependencies(source: File): Future[ProjectDepsReport] = vertx executeBlocking { () => source match
    case s if !s.isDirectory => throw IllegalArgumentException(s"This [${s.toPath}] is not a directory")
    case s                   =>
      val stream = Files walk s.toPath
      try getPackagesPath(stream.iterator.asScala)
      finally stream.close()
  } compose { dirs => 
    Future all (dirs map getPackageDependencies).toList.asJava
  } map { _.list[PackageDepsReport].asScala.toList filter(_.nonEmpty) }
}
