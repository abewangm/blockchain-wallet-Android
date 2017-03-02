package piuk.blockchain.android.data.rxjava;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * A class that allows callers to register {@link PublishSubject} objects by passing in the class
 * type that you wish to emit as an event.
 */
public class RxBus {

    private static RxBus instance;

    /**
     * A threadsafe map of lists of {@link PublishSubject} objects, where their type is used as the
     * key for lookups.
     */
    private ConcurrentHashMap<Object, List<Subject>> subjectsMap = new ConcurrentHashMap<>();

    private RxBus() {
        // Constructor intentionally private
    }

    public static RxBus getInstance() {
        if (instance == null)
            instance = new RxBus();
        return instance;
    }

    /**
     * Registers a new {@link PublishSubject} whose type matches the class {@code type} passed to
     * the method. Returns the PublishSubject so it can be subscribed to, events acted upon and
     * threading applied.
     *
     * @param type The class type of the events you wish to emit
     * @return A {@link PublishSubject} with type {@code type}
     */
    public <T> Observable<T> register(@NonNull Class<T> type) {
        List<Subject> subjects = subjectsMap.get(type);
        if (subjects == null) {
            subjects = new ArrayList<>();
            subjectsMap.put(type, subjects);
        }

        Subject<T> subject = PublishSubject.create();
        subjects.add(subject);

        return subject;
    }

    /**
     * Removes a {@link PublishSubject} of type {@code type} from the list of registered
     * PublishSubjects.
     *
     * @param type       The class type of the {@link PublishSubject} to be removed
     * @param observable An {@link Observable} of type {@code type} which is currently subscribed to
     *                   it's associated {@link PublishSubject}
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    public void unregister(@NonNull Class type, @NonNull Observable observable) {
        List<Subject> subjects = subjectsMap.get(type);
        if (subjects != null) {
            subjects.remove(observable);

            if (subjects.isEmpty()) {
                subjectsMap.remove(type);
            }
        }
    }

    /**
     * Emits an event of type {@code type} to any registered {@link PublishSubject} objects with a
     * matching type.
     *
     * @param type    The class type of object to be emitted and of the {@link PublishSubject} to be
     *                emitted from
     * @param content The actual object to be emitted
     */
    @SuppressWarnings("unchecked")
    public void emitEvent(@NonNull Class type, @NonNull Object content) {
        List<Subject> subjects = subjectsMap.get(type);
        if (subjects != null && !subjects.isEmpty()) {
            for (Subject subject : subjects) {
                subject.onNext(content);
            }
        }
    }
}
