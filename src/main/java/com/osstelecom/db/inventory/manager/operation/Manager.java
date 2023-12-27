/*
 * Copyright (C) 2022 Lucas Nishimura <lucas.nishimura@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.osstelecom.db.inventory.manager.operation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.osstelecom.db.inventory.manager.configuration.ConfigurationManager;
import com.osstelecom.db.inventory.manager.dto.TimerDTO;

/**
 *
 * @author Lucas Nishimura
 * @created 30.08.2022n
 */
public abstract class Manager {

    private Map<String, TimerDTO> timers = new ConcurrentHashMap<>();

    @Autowired
    private ConfigurationManager configurationManager;

    private Logger logger = LoggerFactory.getLogger(Manager.class);

    /**
     * Timer Util
     *
     * @param operation
     * @return
     */
    public String startTimer(String operation) {
        String uid = UUID.randomUUID().toString();
        timers.put(uid, new TimerDTO(uid, operation, System.currentTimeMillis()));
        return uid;
    }

    public String startTimer(String operation, String id) {
        String uid = UUID.randomUUID().toString();
        timers.put(uid, new TimerDTO(uid, id, operation, System.currentTimeMillis()));
        return uid;
    }

    /**
     * Time Util, compute the time, return -1 if invalid
     *
     * @param uid
     */
    public Long endTimer(String key) {
        Long endTimer = System.currentTimeMillis();
        if (timers.containsKey(key)) {
            TimerDTO timer = timers.remove(key);
            Long tookTimer = endTimer - timer.getStartTimer();
            if (configurationManager.loadConfiguration().getTrackTimers()) {
                if (timer.getObjectId() != null) {
                    logger.debug("Timer: [{}] Operation:[{}] on Object:[{}] Took: {} ms (>100ms)", timer.getKey(), timer.getOperation(), timer.getObjectId(), tookTimer);
                } else {
                    logger.debug("Timer: [{}] Operation:[{}] Took: {} ms", timer.getKey(), timer.getOperation(), tookTimer);
                }
            } else {
                if (tookTimer > 100) {
                    //
                    // Slow Operation Detected
                    //
                    if (timer.getObjectId() != null) {
                        logger.warn("Timer: [{}] Operation:[{}] on Object:[{}] Took: {} ms (>100ms)", timer.getKey(), timer.getOperation(), timer.getObjectId(), tookTimer);
                    } else {
                        logger.warn("Timer: [{}] Operation:[{}] Took: {} ms (>100ms)", timer.getKey(), timer.getOperation(), tookTimer);
                    }
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
