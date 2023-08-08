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
package com.osstelecom.db.inventory.manager.session;

import com.osstelecom.db.inventory.manager.operation.DbJobManager;
import com.osstelecom.db.inventory.manager.response.GetRunningDbJobsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * :) Gerencia as Jobs do banco
 *
 * @author Lucas Nishimura
 * @created 19.07.2023
 */
@Service
public class DbJobSession {

    @Autowired
    private DbJobManager jobManager;

    public GetRunningDbJobsResponse getRunningJobs() {
        return new GetRunningDbJobsResponse(this.jobManager.getRunningJobs());
    }
}
