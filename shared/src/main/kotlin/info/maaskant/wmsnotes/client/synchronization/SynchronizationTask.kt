package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.utilities.ApplicationService
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
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

    private var isPaused: Subject<Boolean> = BehaviorSubject.createDefault(false).toSerialized()
    private var synchronizationResult: Subject<SynchronizationResult> = BehaviorSubject.create<SynchronizationResult>().toSerialized()

    private var disposable: Disposable? = null

    private fun connect(): Disposable {
        val disposable1 = Observables.combineLatest(
                Observable.interval(0, 5, TimeUnit.SECONDS),
                isPaused()
        )
                .observeOn(Schedulers.io())
                .map { (_, isPaused) -> isPaused }
                .filter { !it }
                .subscribeBy(
                        onNext = { synchronize() },
                        onError = { logger.error("Error", it) }
                )
        val disposable2 = isPaused().observeOn(Schedulers.io())
                .subscribeBy {
                    logger.debug(if (!it) {
                        "Resuming synchronization"
                    } else {
                        "Pausing synchronization"
                    })
                }
        return CompositeDisposable(disposable1, disposable2)
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
        return isPaused.distinctUntilChanged()
    }

    @Synchronized
    fun pause() {
        isPaused.onNext(true)
    }

    @Synchronized
    fun unpause() {
        isPaused.onNext(false)
    }

    @Suppress("unused")
    fun getSynchronizationResult(): Observable<SynchronizationResult> = synchronizationResult

    @Suppress("MemberVisibilityCanBePrivate")
    fun synchronize() {
        logger.debug("Synchronizing")
        localEventImporter.loadAndStoreLocalEvents()
        remoteEventImporter.loadAndStoreRemoteEvents()
        val result = synchronizer.synchronize()
        synchronizationResult.onNext(result)
    }
}
