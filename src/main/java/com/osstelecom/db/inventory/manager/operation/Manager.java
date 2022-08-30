package com.osstelecom.db.inventory.manager.operation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.osstelecom.db.inventory.manager.configuration.ConfigurationManager;
import com.osstelecom.db.inventory.manager.dto.TimerDto;

public class Manager {
    
    private Map<String, TimerDto> timers = new ConcurrentHashMap<>();

    @Autowired
    private ConfigurationManager configurationManager;
    
    private Logger logger = LoggerFactory.getLogger(DomainManager.class);

    /**
     * Timer Util
     *
     * @param operation
     * @return
     */
    public String startTimer(String operation) {
        String uid = UUID.randomUUID().toString();
        timers.put(uid, new TimerDto(uid, operation, System.currentTimeMillis()));
        return uid;
    }

    /**
     * Time Util, compute the time, return -1 if invalid
     *
     * @param uid
     */
    public Long endTimer(String uid) {
        Long endTimer = System.currentTimeMillis();
        if (timers.containsKey(uid)) {
            TimerDto timer = timers.remove(uid);
            Long tookTimer = endTimer - timer.getStartTimer();
            if (configurationManager.loadConfiguration().getTrackTimers()) {
                logger.debug("Timer: [{}] Operation:[{}] Took: {} ms", timer.getUid(), timer.getOperation(), tookTimer);
            } else {
                if (tookTimer > 100) {
                    //
                    // Slow Operation Detected
                    //
                    logger.warn("Timer: [{}] Operation:[{}] Took: {} ms (>100ms)", timer.getUid(), timer.getOperation(), tookTimer);
                }
            }
            return tookTimer;
        }
        return -1L;
    }

    public String getUUID() {
        return UUID.randomUUID().toString();
    }
    
}
