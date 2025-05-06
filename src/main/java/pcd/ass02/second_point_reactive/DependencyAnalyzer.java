package pcd.ass02.second_point_reactive;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;

import java.nio.file.Path;
import java.util.List;

public class DependencyAnalyzer {
    private final CoreAnalyzer core = new CoreAnalyzer();

    // Subjects to bind to the GUI
    public final BehaviorSubject<Integer> classCount = BehaviorSubject.createDefault(0);
    public final BehaviorSubject<Integer> depCount   = BehaviorSubject.createDefault(0);
    public final PublishSubject<List<String>> logBatches = PublishSubject.create();

    /**
     * Kick off the reactive pipeline given a source folder
     */
    public void analyze(Path sourceRoot) {
        core.scanFolder(sourceRoot)
                .doOnNext(ci -> classCount.onNext(classCount.getValue() + 1))
                .flatMap(ci -> core.findDependencies(ci)
                        .doOnNext(d -> depCount.onNext(depCount.getValue() + 1)))
                .buffer(50, java.util.concurrent.TimeUnit.MILLISECONDS)
                .subscribe(
                        logBatches::onNext,
                        Throwable::printStackTrace,
                        logBatches::onComplete
                );
    }
}