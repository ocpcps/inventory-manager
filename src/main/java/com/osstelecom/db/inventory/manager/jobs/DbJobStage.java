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
package com.osstelecom.db.inventory.manager.jobs;

import java.util.Date;
import java.util.UUID;

/**
 * Esta classe representa um dos estagios de uma job
 * @author Lucas Nishimura
 * @created 14.12.2022
 */
public class DbJobStage {

    private final String jobStageId;
    private String jobStageName;
    private String jobDescription;
    private Date startDate;
    private Date doneDate;
    private Double percDone;
    private Long totalRecords;
    private Long doneRecords;

    public DbJobStage() {
        this.jobStageId = UUID.randomUUID().toString();
    }

    /**
     * @return the jobStageId
     */
    public String getJobStageId() {
        return jobStageId;
    }

   

    /**
     * @return the jobStageName
     */
    public String getJobStageName() {
        return jobStageName;
    }

    /**
     * @param jobStageName the jobStageName to set
     */
    public void setJobStageName(String jobStageName) {
        this.jobStageName = jobStageName;
    }

    /**
     * @return the percDone
     */
    public Double getPercDone() {
        return percDone;
    }

    /**
     * @param percDone the percDone to set
     */
    public void setPercDone(Double percDone) {
        this.percDone = percDone;
    }

    /**
     * @return the totalRecords
     */
    public Long getTotalRecords() {
        return totalRecords;
    }

    /**
     * @param totalRecords the totalRecords to set
     */
    public void setTotalRecords(Long totalRecords) {
        this.totalRecords = totalRecords;
    }

    /**
     * @return the doneRecords
     */
    public Long getDoneRecords() {
        return doneRecords;
    }

    /**
     * @param doneRecords the doneRecords to set
     */
    public void setDoneRecords(Long doneRecords) {
        this.doneRecords = doneRecords;
    }

    /**
     * @return the jobDescription
     */
    public String getJobDescription() {
        return jobDescription;
    }

    /**
     * @param jobDescription the jobDescription to set
     */
    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    /**
     * @return the startDate
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * @param startDate the startDate to set
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * @return the doneDate
     */
    public Date getDoneDate() {
        return doneDate;
    }

    /**
     * @param doneDate the doneDate to set
     */
    public void setDoneDate(Date doneDate) {
        this.doneDate = doneDate;
    }
}
