package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SynchronizationTask @Inject constructor(
        private val localEventImporter: LocalEventImporter,
        private val remoteEventImporter: RemoteEventImporter,
        private val synchronizer: Synchronizer
) {

    private val logger by logger()

    private var isPaused: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)

    private var timerDisposable: Disposable? = null

    @Synchronized
    fun start() {
        if (timerDisposable == null) {
            logger.debug("Starting synchronization")
            timerDisposable =
                    Observables.combineLatest(
                            Observable.interval(0, 5, TimeUnit.SECONDS),
                            isPaused
                    )
                            .observeOn(Schedulers.io())
                            .map { (_, isPaused) -> isPaused }
                            .filter { !it }
                            .subscribe {
                                synchronize()
                            }
        }
    }

    fun shutdown() {
        if (timerDisposable != null) logger.debug("Stopping synchronization")
        timerDisposable?.dispose()
    }

    @Synchronized
    fun isPaused(): Observable<Boolean> {
        return isPaused
    }

    @Synchronized
    fun pause() {
        if (isPaused.value == false) {
            logger.debug("Pausing synchronization")
            isPaused.onNext(true)
        }
    }

    @Synchronized
    fun unpause() {
        if (isPaused.value == true) {
            logger.debug("Resuming synchronization")
            isPaused.onNext(false)
        }
    }

    private fun synchronize() {
        logger.debug("Synchronizing")
        localEventImporter.loadAndStoreLocalEvents()
        remoteEventImporter.loadAndStoreRemoteEvents()
        synchronizer.synchronize()
    }
}
