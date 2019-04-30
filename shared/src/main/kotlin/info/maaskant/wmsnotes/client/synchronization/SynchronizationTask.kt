package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.utilities.ApplicationService
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SynchronizationTask @Inject constructor(
        private val localEventImporter: LocalEventImporter,
        private val remoteEventImporter: RemoteEventImporter,
        private val synchronizer: Synchronizer
) : ApplicationService {

    private val logger by logger()

    private var isPaused: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)

    private var disposable: Disposable? = null

    private fun connect(): Disposable {
        return Observables.combineLatest(
                Observable.interval(0, 5, TimeUnit.SECONDS),
                isPaused
        )
                .observeOn(Schedulers.io())
                .map { (_, isPaused) -> isPaused }
                .filter { !it }
                .subscribeBy(
                        onNext = { synchronize() },
                        onError = { logger.warn("Error", it) }
                )
    }

    @Synchronized
    override fun start() {
        if (disposable == null) {
            logger.debug("Starting synchronization")
            disposable = connect()
        }
    }

    @Synchronized
    override fun shutdown() {
        disposable?.let {
            logger.debug("Stopping synchronization")
            it.dispose()
        }
        disposable = null
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
