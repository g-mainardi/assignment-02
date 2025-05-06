package pcd.ass02.second_point_reactive;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.reactivex.rxjava3.core.Observable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class CoreAnalyzer {
    private static final ParserConfiguration config = new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_14);
    private static final JavaParser parser = new JavaParser(config);

    /**
     * Represents minimal information for classes and interfaces.
     */
    public record ClassInfo(String name, String dependencies) {}
//    public record ClassInfo(String name, List<String> dependencies) {}
//    public record PackageInfo(String name, List<ClassInfo> classInfos) {}

    /**
     * Scans a source folder recursively and emits a ClassInfo for each class/interface found
     */
    public Observable<ClassInfo> scanPackage(Path packagePath) {
        return Observable.create( emitter -> {
            
                }
        );
    }
    public Observable<ClassInfo> scanFolder(Path sourceRoot) {
        return Observable.create(emitter -> {
            try (Stream<Path> files = Files.walk(sourceRoot)) {
                files
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        try {
                            CompilationUnit cu = parser.parse(p).getResult().orElseThrow(() -> new IllegalArgumentException("Failed to parse [" + p + "]"));
                            cu.findAll(ClassOrInterfaceDeclaration.class)
                                    .forEach(decl -> {
                                        String pkg = cu.getPackageDeclaration()
                                                .map(pd -> pd.getName().toString())
                                                .orElse("");
                                        emitter.onNext(new ClassInfo(pkg, decl.getNameAsString()));
                                    });
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                    });
                emitter.onComplete();
            }
        });
    }

    /**
     * Given a ClassInfo, emits each dependency as a simple string `from->to`
     */
    public Observable<String> findDependencies(ClassInfo ci) {
        // TODO: parse the same file or store AST in ClassInfo for full analysis
        return Observable.empty();
    }
}

