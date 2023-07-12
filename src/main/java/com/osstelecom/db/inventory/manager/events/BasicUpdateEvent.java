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
package com.osstelecom.db.inventory.manager.events;

import com.osstelecom.db.inventory.manager.jobs.DBJobInstance;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 12.07.2023
 */
public abstract class BasicUpdateEvent<T> implements IEvent {

    private T oldData;
    private T newData;

    private DBJobInstance relatedJob;

    public BasicUpdateEvent(T oldData, T newData) {
        this.oldData = oldData;
        this.newData = newData;
    }

    public BasicUpdateEvent(T oldData, T newData, DBJobInstance relatedJob) {
        this.oldData = oldData;
        this.newData = newData;
        this.relatedJob = relatedJob;
    }

    public DBJobInstance getRelatedJob() {
        return relatedJob;
    }

    public void setRelatedJob(DBJobInstance relatedJob) {
        this.relatedJob = relatedJob;
    }
}
