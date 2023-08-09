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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Gerencia as jobs de update que possam estar rodando no banco
 *
 * @author Lucas Nishimura
 * @created 26.01.2023
 */
@Service
public class DbJobManager extends Manager {

    private Logger logger = LoggerFactory.getLogger(DbJobManager.class);

    private Map<String, DBJobInstance> runningJobs = new ConcurrentHashMap<>();

    private AtomicLong totalJobsDone = new AtomicLong(0);

    /**
     * Chamado quando uma nova job é Iniciada
     *
     * @param job
     */
    public void notifyJobStart(DBJobInstance job) {
        logger.debug("Job:[{}] Started", job.getJobId());
        this.runningJobs.put(job.getJobId(), job);
    }

    /**
     * Chamado quando uma nova job é Finalizada
     *
     * @param job
     */
    public void notifyJobEnd(DBJobInstance job) {
        if (job.getJobEnded() == null) {
            job.setJobEnded(new Date());
            job = this.runningJobs.remove(job.getJobId());
            if (job.getJobStarted() != null) {
                Long took = job.getJobEnded().getTime() - job.getJobStarted().getTime();
                this.totalJobsDone.incrementAndGet();
                logger.debug("JOB:[{}] Done: And Took:[{}] ms", job.getJobId(), took);
            }
        } else {
            logger.warn("JOB:[{}] Already Done", job.getJobId());
        }
    }

    /**
     * Cria uma nova instancia do Job
     *
     * @return
     */
    public DBJobInstance createJobInstance() {
        String uuid = UUID.randomUUID().toString();
        DBJobInstance instance = new DBJobInstance(uuid);
        return instance;
    }

    /**
     * Faz a manutenção das Jobs Finalizadas, executado a cada 1 minuto Depois
     * de 15 minutos , ela é expurgada
     */
    @Scheduled(cron = "0 * * * * ?")
    private void jobCleanUp() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, -15);
        List<String> idsToRemove = new ArrayList<>();
        this.runningJobs.forEach((k, v) -> {
            if (v.getJobEnded() != null) {
                if (v.getJobEnded().before(cal.getTime())) {
                    //
                    // Já passou 15 minutos, manda embora..
                    //
                    idsToRemove.add(k);
                }
            }

        });
        if (!idsToRemove.isEmpty()) {
            logger.debug("Found: [{}] ids to remove:", idsToRemove.size());
            idsToRemove.forEach(i -> {
                DBJobInstance a = this.runningJobs.remove(i);
                logger.debug("Removed Job Data Information:[{}/{}]", a.getJobId(), i);
            });
        }
    }

    /**
     * Retorna a quantidade total de jobs concluidas
     *
     * @return
     */
    public Long getTotalJobsDone() {
        return this.totalJobsDone.get();
    }

    /**
     * Retorna a lista de jobs em execução
     *
     * @return
     */
    public List<DBJobInstance> getRunningJobs() {
        List<DBJobInstance> result = this.runningJobs.values()
                .stream()
                .filter(f -> {
                    //
                    // Se tem data de inicio e não tem data de fim,
                    // Presume que a job esteja em execução
                    //
                    if (f.getJobStarted() != null && f.getJobEnded() == null) {
                        return true;
                    } else {
                        return false;
                    }
                })
                .collect(Collectors.toList());
        return result;
    }
}
