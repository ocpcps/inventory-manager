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

/**
 *
 * @author  Lucas Nishimura <lucas.nishimura@gmail.com> 
 * @created 14.12.2022
 */
public class DbJobStage {
    private String jobStageId;
    private String jobStageName;
    private Double percDone;
    private Long totalRecords;
    private Long doneRecords;

    /**
     * @return the jobStageId
     */
    public String getJobStageId() {
        return jobStageId;
    }

    /**
     * @param jobStageId the jobStageId to set
     */
    public void setJobStageId(String jobStageId) {
        this.jobStageId = jobStageId;
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
}
