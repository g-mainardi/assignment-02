package pcd.ass02.first_point_asynch;

import com.github.javaparser.JavaParser;
import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyAnalyzerLib {
    private static final Vertx vertx = Vertx.vertx();
    static ParserConfiguration config = new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_14);

    private static final JavaParser parser = new JavaParser(config);

    public record ClassDepsReport(List<String> dependencies) {
        public boolean hasDependencies(){
            return !dependencies.isEmpty();
        }
    }
    public record PackageDepsReport(List<ClassDepsReport> classReports) {
        public PackageDepsReport(Stream<ClassDepsReport> stream){
            this(stream.toList());
        }
        public boolean hasDependencies(){
            return !classReports.isEmpty();
        }
    }
    public record ProjectDepsReport(List<PackageDepsReport> classReports) {
        public ProjectDepsReport(Stream<PackageDepsReport> stream){
            this(stream.toList());
        }
    }

    private static boolean isVisible(Path path) {
        for (Path part : path) {
            String name = part.toString();
            if (!name.equals(".") && name.startsWith(".")) {
                return false;
            }
        }
        return true;
    }

    private static boolean isJavaFile(File file) {
        return file.getName().endsWith(".java");
    }

    public static Future<ClassDepsReport> getClassDependencies(File source) {
        return vertx.executeBlocking(() -> {
            Optional<CompilationUnit> unit = parser.parse(source).getResult();
            if (unit.isEmpty()) {
                throw new IllegalArgumentException("Failed to parse [" + source + "]");
            }
            List<String> imports = unit.get().getImports().stream()
                    .map(ImportDeclaration::getName)
                    .map(Objects::toString)
                    .collect(Collectors.toList());
            return new ClassDepsReport(imports);
        });
    }

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
                            .filter(DependencyAnalyzerLib::isJavaFile)
                            .map(DependencyAnalyzerLib::getClassDependencies)
                            .toList();
                    return Future.all(classReports);
                })
                .map(res -> {
                    List<ClassDepsReport> reports = res.list();
                    return new PackageDepsReport(reports.stream().filter(ClassDepsReport::hasDependencies));
                });
    }

    public static Future<ProjectDepsReport> getProjectDependencies(File source) {
        return vertx.executeBlocking(() -> {
                    if (!source.isDirectory()) {
                        throw new IllegalArgumentException("Not a dir");
                    }
                    try (Stream<Path> pathStream = Files.walk(source.toPath())) {
                        return pathStream
                                .filter(DependencyAnalyzerLib::isVisible)
                                .map(Path::toFile)
                                .filter(File::isDirectory)
                                .collect(Collectors.toSet());
                    }
                })
                .compose(directories -> {
                    List<Future<PackageDepsReport>> packageReports = directories.stream()
                            .map(DependencyAnalyzerLib::getPackageDependencies)
                            .collect(Collectors.toList());
                    return Future.all(packageReports);
                })
                .map(future -> {
                    List<PackageDepsReport> reports = future.list();
                    return new ProjectDepsReport(reports.stream().filter(PackageDepsReport::hasDependencies));
                });
    }

}
