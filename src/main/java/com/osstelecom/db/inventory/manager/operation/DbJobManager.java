/*
 * Copyright (C) 2023 Lucas Nishimura <lucas.nishimura@gmail.com>
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

import com.osstelecom.db.inventory.manager.jobs.DBJobInstance;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 26.01.2023
 */
@Service
public class DbJobManager extends Manager {

    private Logger logger = LoggerFactory.getLogger(DbJobManager.class);

    private Map<String, DBJobInstance> runningJobs = new ConcurrentHashMap<>();

    public void notifyJobStart(DBJobInstance job) {
        this.runningJobs.put(job.getJobId(), job);
    }

    public void notifyJobEnd(DBJobInstance job) {
        job.setJobEnded(new Date());
        this.runningJobs.remove(job.getJobId());
        Long took = job.getJobEnded().getTime() - job.getJobStarted().getTime();
        logger.debug("JOB:[{}] Done: And Took:[{}] ms", job.getJobId(), took);
    }

    public DBJobInstance createJobInstance() {
        String uuid = UUID.randomUUID().toString();
        DBJobInstance instance = new DBJobInstance(uuid);
        return instance;
    }

}
