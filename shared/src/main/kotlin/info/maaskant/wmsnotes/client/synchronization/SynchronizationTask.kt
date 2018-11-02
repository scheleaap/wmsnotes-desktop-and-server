package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SynchronizationTask @Inject constructor(
    private val localEventImporter: LocalEventImporter,
    private val remoteEventImporter: RemoteEventImporter,
    private val synchronizer: Synchronizer
) {

    private val logger by logger()

    private var paused: Boolean = false

    private var timerDisposable: Disposable? = null

    fun start() {
        if (timerDisposable == null) {
            logger.debug("Starting synchronization")
            timerDisposable = Observable
                .interval(0, 5, TimeUnit.SECONDS)
                .filter { !paused }
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

    fun pause() {
        if (!paused) logger.debug("Pausing synchronization")
        paused = true
    }

    fun unpause() {
        if (paused) logger.debug("Resuming synchronization")
        paused = false
    }

    private fun synchronize() {
        logger.debug("Synchronizing")
        localEventImporter.loadAndStoreLocalEvents()
        remoteEventImporter.loadAndStoreRemoteEvents()
        synchronizer.synchronize()
    }

}
