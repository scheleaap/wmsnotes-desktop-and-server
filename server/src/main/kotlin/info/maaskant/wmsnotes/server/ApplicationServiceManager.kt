package info.maaskant.wmsnotes.server;

import info.maaskant.wmsnotes.utilities.ApplicationService
import info.maaskant.wmsnotes.utilities.logger
import org.springframework.stereotype.Service
import javax.inject.Inject
import javax.inject.Singleton

@Service
@Singleton
data class ApplicationServiceManager @Inject constructor(
        private val services: List<ApplicationService>
) : ApplicationService {

    private val logger by logger()

    override fun start() {
        logger.debug("Starting")
        services.forEach(ApplicationService::start)
    }

    override fun shutdown() {
        logger.debug("Shutting down")
        services.reversed().forEach(ApplicationService::shutdown)
    }
}
