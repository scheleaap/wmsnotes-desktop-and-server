package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
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

    private var paused: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)

    private var timerDisposable: Disposable? = null

    fun start() {
        if (timerDisposable == null) {
            logger.debug("Starting synchronization")
            timerDisposable = Observable
                    .interval(0, 5, TimeUnit.SECONDS)
                    .filter { paused.value == false }
                    .observeOn(Schedulers.io())
                    .subscribe {
                        synchronize()
                    }
        }
    }

    fun shutdown() {
        if (timerDisposable != null) logger.debug("Stopping synchronization")
        timerDisposable?.dispose()
    }

    fun isPaused(): Observable<Boolean> {
        return paused
    }

    fun pause() {
        if (paused.value == false) {
            logger.debug("Pausing synchronization")
            paused.onNext(true)
        }
    }

    fun unpause() {
        if (paused.value == true) {
            logger.debug("Resuming synchronization")
            paused.onNext(false)
        }
    }


    private fun synchronize() {
        logger.debug("Synchronizing")
        localEventImporter.loadAndStoreLocalEvents()
        remoteEventImporter.loadAndStoreRemoteEvents()
        synchronizer.synchronize()
    }

}
