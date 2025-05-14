package pcd.ass02.first_point_asynch

import io.vertx.core.Future
import pcd.ass02.first_point_asynch.DependencyAnalyzerLibScala.*
import pcd.ass02.first_point_asynch.TestDependencyAnalyzerLibScala.Analyzer.{CLASS, PACKAGE, PROJECT}

import java.io.File

object TestDependencyAnalyzerLibScala { @throws[Exception]
  enum Analyzer{case CLASS; case PACKAGE; case PROJECT}

  def main(args: Array[String]): Unit =
    test(CLASS, "") // should launch exception
    test(CLASS, "src/main/java/pcd/ass02/first_point_asynch/foopack/B.java")
    test(PACKAGE, "src/main/java/pcd/ass02/first_point_asynch/foopack/")
    test(PACKAGE, "src/main/java/pcd/ass02/first_point_asynch/foopack2/")
    test(PROJECT, ".")
    test(PROJECT, "src/main/java/pcd/ass02/first_point_asynch/foopack/")

  def log(msg: String): Unit = println(s"[ ${System.currentTimeMillis} ][ ${Thread.currentThread} ] $msg")

  private def test(analyzeOn: Analyzer, path: String): Unit =
    log(s"Doing the $analyzeOn dependencies async call... ")
    val futureReport = (analyzeOn match {
      case CLASS   => getClassDependencies
      case PACKAGE => getPackageDependencies
      case PROJECT => getProjectDependencies
    })(File(path))
    log("...called function...")
    futureReport
      .onSuccess(report => log(s"...here are the dependencies \n\t$report"))
      .onFailure((err: Throwable) => log(s"...failure with path [$path]: \n\t${err.getMessage}"))
}