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
 * @author Lucas Nishimura
 * @created 12.07.2023
 */
public interface IEvent {

    public DBJobInstance getRelatedJob();

    public void setRelatedJob(DBJobInstance relatedJob);

    public String getMdcId();

    public void setMdcId(String mdcId);
}
