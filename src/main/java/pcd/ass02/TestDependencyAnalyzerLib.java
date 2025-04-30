package pcd.ass02;

import io.vertx.core.Future;

import java.io.File;

import static pcd.ass02.DependencyAnalyzerLib.*;

public class TestDependencyAnalyzerLib {
    public static void main(String[] args) throws Exception {
        testGetClassDependencies("");
        testGetClassDependencies("src/main/java/pcd/ass02/Main.java");
    }

    private static void testGetClassDependencies(final String filePath) {
        log("Doing the class dependencies async call... ");
        Future<ClassDepsReport> fut = getClassDependencies(new File(filePath));
        log("...called function...");
        fut
            .onSuccess((res) -> log("...here are the dependencies \n" + res.toString()))
            .onFailure((res) -> log("...failure with path [" + filePath + "]: \n" + res.toString()));
    }

    private static void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ " + Thread.currentThread() + " ] " + msg);
    }
}
