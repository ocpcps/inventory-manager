/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uc.part2;

import com.osstelecom.db.inventory.topology.node.DefaultNode;
import com.osstelecom.db.inventory.topology.node.INetworkNode;
import com.osstelecom.db.inventory.topology.DefaultTopology;
import com.osstelecom.db.inventory.topology.exception.GraphNotEnabledException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.osstelecom.db.inventory.topology.impact.ImpactManagerIf;

/**
 *
 * @author Nishisan
 */
public class DefaultExample1 {

    public DefaultExample1() throws GraphNotEnabledException {

        DefaultTopology topology = new DefaultTopology(100);

        //
        // Elementos da Topologia
        //
        DefaultNode saida = new DefaultNode("saida1", 10, topology);

        saida.setEndPoint(true);

        DefaultNode router1 = new DefaultNode("router1", 1, topology);
        DefaultNode router2 = new DefaultNode("router2", 2, topology);
        DefaultNode router3 = new DefaultNode("router3", 3, topology);
        DefaultNode router4 = new DefaultNode("router4", 4, topology);
        DefaultNode router5 = new DefaultNode("router5", 5, topology);
        DefaultNode router6 = new DefaultNode("router6", 6, topology);
        DefaultNode erb = new DefaultNode("erb", 7, topology);
        //
        // Conexões
        //
        topology.addConnection(router1, saida);
        topology.addConnection(router2, saida);
        topology.addConnection(router1, router2);

        topology.addConnection(router1, router3);
        topology.addConnection(router3, router2);

        topology.addConnection(router4, router1);
        topology.addConnection(router4, router3);
        topology.addConnection(router4, router2);
        topology.addConnection(router4, router5);
        topology.addConnection(router4, router6);
        topology.addConnection(router3, router5);
        topology.addConnection(router6, router5);
        topology.addConnection(router3, router6);
        topology.addConnection(erb, router6);
        topology.addConnection(erb, router5);

        ImpactManagerIf impactManager = topology.getImpactManager();
        System.out.println("Topologyy Size: " + topology.getNodes().size() + "  Connections:" + topology.getConnections().size());
        System.out.println("-------------------------------------------------------------");
        System.out.println("Weak Nodes:");
        System.out.println("-------------------------------------------------------------");
        Long start = System.currentTimeMillis();

        List<INetworkNode> weak = topology.getImpactManager().getWeakNodes(1, false, 1, false);
        Long end = System.currentTimeMillis();
        Long took = end - start;
        System.out.println("Found " + weak.size() + " Weak Nodes Took:" + took + " ms");
        for (INetworkNode n : weak) {
            System.out.println("  ::Weak " + n.getName() + " Connections size:" + n.getEndpointConnectionsCount());
        }

//        List<INetworkNode> unreacheble = topology.getImpactManager().getUnreacheableNodes();
//        System.out.println("Found " + unreacheble.size() + " Unreacheable Nodes");
        System.out.println("Done...");

//        topology.getGraph().display();
    }

    /**
     *
     *
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            new DefaultExample1();
        } catch (GraphNotEnabledException ex) {
            Logger.getLogger(DefaultExample1.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
