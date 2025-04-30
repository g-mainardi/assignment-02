package pcd.ass02;

import io.vertx.core.Future;

import java.io.File;

import static pcd.ass02.DependencyAnalyzerLib.*;

public class TestDependencyAnalyzerLib {
    public static void main(String[] args) throws Exception {
        testGetClassDependencies("");
        testGetClassDependencies("src/main/java/pcd/ass02/foopack/B.java");
        testGetPackageDependencies("src/main/java/pcd/ass02/foopack/");
        testGetPackageDependencies("src/main/java/pcd/ass02/foopack2/");
        testGetProjectDependencies(".");
        testGetProjectDependencies("src/main/java/pcd/ass02/foopack/");
    }

    private static void testGetClassDependencies(final String filePath) {
        log("Doing the class dependencies async call... ");
        Future<ClassDepsReport> fut = getClassDependencies(new File(filePath));
        log("...called function...");
        fut
            .onSuccess((res) -> log("...here are the dependencies \n" + res.toString()))
            .onFailure((err) -> log("...failure with path [" + filePath + "]: \n" + err.toString()));
    }

    private static void testGetPackageDependencies(final String packagePath) {
        log("Doing the package dependencies async call... ");
        Future<PackageDepsReport> fut = getPackageDependencies(new File(packagePath));
        log("...called function...");
        fut
            .onSuccess((res) -> log("...here are the dependencies \n" + res.toString()))
            .onFailure((res) -> log("...failure with path [" + packagePath + "]: \n" + res.toString()));
    }

    private static void testGetProjectDependencies(final String projectPath) {
        log("Doing the project dependencies async call... ");
        Future<ProjectDepsReport> fut = getProjectDependencies(new File(projectPath));
        log("...called function...");
        fut
                .onSuccess((res) -> log("...here are the dependencies \n" + res.toString()))
                .onFailure((res) -> log("...failure with path [" + projectPath + "]: \n" + res.toString()));
    }

    private static void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ " + Thread.currentThread() + " ] " + msg);
    }
}
