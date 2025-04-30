package pcd.ass02;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
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
        return vertx.executeBlocking(() -> {
                    if (!source.isDirectory()) {
                        throw new IllegalArgumentException("This [" + source.toPath() + "] is not a package");
                    }
                    File[] files = source.listFiles();
                    if (files == null) {
                        throw new IOException("List files returned null");
                    }
                    return List.of(files);
                })
                .compose(files -> {
                    List<Future<ClassDepsReport>> classReports = files.stream()
                            .filter(File::isFile)
                            .map(DependencyAnalyzerLib::getClassDependencies)
                            .toList();
                    return Future.all(classReports);
                })
                .map(res -> new PackageDepsReport(res.list()));
    }

    public static Future<PackageDepsReport> getPackageDependencies(String source) {
        FileSystem fileSystem = vertx.fileSystem();
        return fileSystem
                .exists(source)
                .compose(exist -> {
                    if (!exist) {
                        return Future.failedFuture(new IllegalArgumentException("File doesn't exist"));
                    }
                    return fileSystem.props(source);
                })
                .compose(fileProps -> {
                    if (!fileProps.isDirectory()) {
                        return Future.failedFuture(new IllegalArgumentException("File is not a directory"));
                    }
                    return fileSystem.readDir(source);
                })
                .compose(paths -> {
                    List<Future<ClassDepsReport>> classReports = paths.stream()
                            .map(File::new)
                            .map(DependencyAnalyzerLib::getClassDependencies)
                            .collect(Collectors.toList());
                    return Future.all(classReports);
                })
                .map(results -> new PackageDepsReport(results.list()));
    }
}
