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
package com.osstelecom.db.inventory.topology.algorithm;

import com.osstelecom.db.inventory.topology.node.SourceTargetWrapper;
import java.util.Map;
import java.util.Queue;

/**
 *
 * @author Lucas Nishimura
 * @created 24.10.2022
 */
public interface ITopologyAlgorithm {

    /**
     * Run Algorithm Calculation
     *
     * @param weakQueue
     */
    public void calculate(Queue<SourceTargetWrapper> weakQueue);

    public void calculate(Queue<SourceTargetWrapper> weakQueue, int threadCount);

    public void calculate(Queue<SourceTargetWrapper> weakQueue, Map<String, Object> options);

    public void calculate(Queue<SourceTargetWrapper> weakQueue, Map<String, Object> options, int threadCount);

//    public void start();
}
