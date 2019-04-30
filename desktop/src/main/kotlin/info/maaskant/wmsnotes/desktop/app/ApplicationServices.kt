package info.maaskant.wmsnotes.desktop.app;

import info.maaskant.wmsnotes.client.synchronization.SynchronizationTask
import info.maaskant.wmsnotes.utilities.ApplicationService
import info.maaskant.wmsnotes.utilities.logger
import org.springframework.stereotype.Service
import javax.inject.Inject
import javax.inject.Singleton

@Service
@Singleton
class ApplicationServiceManager @Inject constructor(
        private val services: List<ApplicationService>,
        private val synchronizationTask: SynchronizationTask
) : ApplicationService {

    private val logger by logger()

    override fun start() {
        logger.debug("Starting")
        services.forEach(ApplicationService::start)
        synchronizationTask.pause()
        synchronizationTask.start()
    }

    override fun shutdown() {
        logger.debug("Shutting down")
        synchronizationTask.shutdown()
        services.reversed().forEach(ApplicationService::shutdown)
    }
}
