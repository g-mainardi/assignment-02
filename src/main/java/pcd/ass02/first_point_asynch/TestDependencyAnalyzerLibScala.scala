package pcd.ass02.first_point_asynch

import java.io.File
import pcd.ass02.first_point_asynch.DependencyAnalyzerLibScala._

object TestDependencyAnalyzerLibScala { @throws[Exception]
 def main(args: Array[String]): Unit =
  testGetClassDependencies("")
  testGetClassDependencies("src/main/java/pcd/ass02/first_point_asynch/foopack/B.java")
  testGetPackageDependencies("src/main/java/pcd/ass02/first_point_asynch/foopack/")
  testGetPackageDependencies("src/main/java/pcd/ass02/first_point_asynch/foopack2/")
  testGetProjectDependencies(".")
  testGetProjectDependencies("src/main/java/pcd/ass02/first_point_asynch/foopack/")

  def testGetClassDependencies(filePath: String): Unit =
   log("Doing the class dependencies async call... ")
   val fut = getClassDependencies(new File(filePath))
   log("...called function...")
   fut
     .onSuccess((res: ClassDepsReport) => log("...here are the dependencies \n" + res.toString))
     .onFailure((err: Throwable) => log("...failure with path [" + filePath + "]: \n" + err.toString))

  def testGetPackageDependencies(packagePath: String): Unit =
   log("Doing the package dependencies async call... ")
   val fut = getPackageDependencies(new File(packagePath))
   log("...called function...")
   fut
     .onSuccess((res: PackageDepsReport) => log("...here are the dependencies \n" + res.toString))
     .onFailure((res: Throwable) => log("...failure with path [" + packagePath + "]: \n" + res.toString))

  def testGetProjectDependencies(projectPath: String): Unit =
   log("Doing the project dependencies async call... ")
   val fut = getProjectDependencies(new File(projectPath))
   log("...called function...")
   fut
     .onSuccess((res: ProjectDepsReport) => log("...here are the dependencies \n" + res.toString))
     .onFailure((res: Throwable) => log("...failure with path [" + projectPath + "]: \n" + res.toString))

  def log(msg: String): Unit =
   println("[ " + System.currentTimeMillis + " ][ " + Thread.currentThread + " ] " + msg)
}