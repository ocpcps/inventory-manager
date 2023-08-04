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

import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.operation.DbJobManager;
import com.osstelecom.db.inventory.manager.operation.GraphManager;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 02.02.2023
 */
@Service
public class GraphSession {

    @Autowired
    private GraphManager graphManager;

    private Logger logger = LoggerFactory.getLogger(GraphSession.class);

    public GraphList<ResourceConnection> expandNode(ManagedResource resource, String direction, Integer depth)
            throws ResourceNotFoundException, InvalidRequestException {
        logger.debug("Expanding Node:[{}] Direction:[{}] Depth:[{}]", resource.getId(), direction, depth);

        /**
         * Performance Saving
         */
        if (depth > 5) {
            throw new InvalidRequestException("Depth cannot be greater than 5");
        }

        return this.graphManager.expandNode(resource, direction, depth);
    }
}
