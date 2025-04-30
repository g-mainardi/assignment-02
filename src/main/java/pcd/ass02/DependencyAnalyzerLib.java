package pcd.ass02;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DependencyAnalyzerLib {

    public record ClassDepsReport(List<String> dependencies){}
    private static final Vertx vertx = Vertx.vertx();

    public static Future<ClassDepsReport> getClassDependencies(File source) {
        return vertx.executeBlocking(() -> {
            CompilationUnit unit = StaticJavaParser.parse(source);
            List<String> imports = unit.getImports().stream()
                    .map(ImportDeclaration::getName)
                    .map(Objects::toString)
                    .collect(Collectors.toList());
            return new ClassDepsReport(imports);
        });
    }

    public record PackageDepsReport(List<ClassDepsReport> classReports){}

    public static Future<PackageDepsReport> getPackageDependencies(File source) {
        Promise<PackageDepsReport> promise = Promise.promise();
        vertx.executeBlocking(() -> {
                    if (!source.isDirectory()) {
                        throw new IllegalArgumentException("Not dir");
                    }
                    File[] files = source.listFiles();
                    if (files == null) {
                        throw new IOException("List files returned null");
                    }
                    List<Future<ClassDepsReport>> classReports = Arrays.stream(files)
                            .filter(File::isFile)
                            .map(DependencyAnalyzerLib::getClassDependencies)
                            .toList();
                    return Future.all(classReports);
                })
                .onSuccess(compositeFuture -> promise.complete(new PackageDepsReport(compositeFuture.list())));
        return promise.future();
    }

    public static Future<PackageDepsReport> getPackageDependencies(String source) {
        FileSystem fileSystem = Vertx.vertx().fileSystem();
        return fileSystem
                .exists(source)
                .compose(exist -> {
                    if (exist) {
                        return fileSystem.props(source);
                    } else {
                        return Future.failedFuture(new IllegalArgumentException("File doesnt exist"));
                    }
                })
                .compose(fileProps -> {
                    if (fileProps.isDirectory()) {
                        return fileSystem.readDir(source);
                    } else {
                        return Future.failedFuture(new IllegalArgumentException("File is not a directory"));
                    }
                })
                .compose(paths -> {
                    List<Future<ClassDepsReport>> classReports = paths.stream()
                            .map(File::new)
                            .map(DependencyAnalyzerLib::getClassDependencies)
                            .collect(Collectors.toList());
                    return Future.all(classReports);
                })
                .map(compositeFuture -> new PackageDepsReport(compositeFuture.list()));
    }
}
