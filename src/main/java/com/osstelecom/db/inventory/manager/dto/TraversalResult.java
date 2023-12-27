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
package com.osstelecom.db.inventory.manager.dto;

import com.osstelecom.db.inventory.manager.elements.PathElement;
import java.util.List;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 30.08.2023
 */
public class TraversalResult {

    private final List<List<PathElement>> paths;
    private final Long iteration;
    private final Long processingTime;
    

    public TraversalResult(List<List<PathElement>> paths, Long iteration, Long processingTime) {
        this.paths = paths;
        this.iteration = iteration;
        this.processingTime
                = processingTime;
    }

    public Long getProcessingTime() {
        return processingTime;
    }

    public List<List<PathElement>> getPaths() {
        return paths;
    }

    public Long getIteration() {
        return iteration;
    }

}
