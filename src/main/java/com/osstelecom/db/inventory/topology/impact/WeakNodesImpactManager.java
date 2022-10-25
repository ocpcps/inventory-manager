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
package com.osstelecom.db.inventory.topology.impact;

import com.nimbusds.jose.Algorithm;
import com.osstelecom.db.inventory.topology.ITopology;
import com.osstelecom.db.inventory.topology.algorithm.ITopolocyAlgorithm;
import com.osstelecom.db.inventory.topology.algorithm.WeakNodesAlgorithm;
import com.osstelecom.db.inventory.topology.node.INetworkNode;
import com.osstelecom.db.inventory.topology.node.SourceTargetWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 24.10.2022
 */
public class WeakNodesImpactManager extends DefaultImpactManager {
    
    private ITopolocyAlgorithm algorithm = new WeakNodesAlgorithm();
    private org.slf4j.Logger logger = LoggerFactory.getLogger(WeakNodesImpactManager.class);
    
    public WeakNodesImpactManager(ITopology topology) {
        super(topology);
    }
    
    @Override
    public List<INetworkNode> getWeakNodes(Integer connLimit, Boolean all, Integer threadCount, Boolean useCache) {
        
        LinkedBlockingQueue<SourceTargetWrapper> weakQueue = new LinkedBlockingQueue<>();
        //
        // Esses nunca vão chegar lá....
        // Já elege pelo DFS os que não tocam nenhuma saída...
        //
        ArrayList<INetworkNode> alreadyWeak = this.getUnreacheableNodes();
        logger.debug("Removing: " + alreadyWeak.size() + " Nodes Because Already Weak - Unreacheable");

        //
        // Se um nó tiver menos conexões que o desejado já é fraco também.
        //
        for (INetworkNode node : this.getTopology().getNodes()) {
            if (node.getConnectionCount() < connLimit) {
                alreadyWeak.add(node);
                logger.debug("Removing: " + node.getName() + "  Because Already Weak - Connections Size: " + node.getConnectionCount());
                
            }
        }

        //
        // Cria uma queue, mas a gente já nem calcula aqueles que não chegam a nenhum endpoint..
        //
        for (INetworkNode node : this.getTopology().getNodes()) {
            for (INetworkNode target : this.getTopology().getEndPoints()) {
                if (!alreadyWeak.contains(node)) {
                    if (!node.endPoint()) {
                        SourceTargetWrapper w = new SourceTargetWrapper();
                        w.setSource(node);
                        w.setTarget(target);
                        w.setLimit(connLimit);
                        w.setUseCache(useCache);
                        weakQueue.add(w);
                    }
                }
            }
        }
        logger.debug("Commiting Queue With :[{}] Jobs", weakQueue.size());
        this.algorithm.calculate(weakQueue);
        this.algorithm.start();
        List<INetworkNode> result = new ArrayList<>();
        return result;
    }
    
}
