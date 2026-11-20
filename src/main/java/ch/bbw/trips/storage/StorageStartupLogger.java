package ch.bbw.trips.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Logs which photo storage is active, so a deployment can be verified in CloudWatch Logs. */
@Component
class StorageStartupLogger implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StorageStartupLogger.class);
    private final PhotoStorage storage;

    StorageStartupLogger(PhotoStorage storage) {
        this.storage = storage;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Photo storage: {}", storage.describe());
    }
}
