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
import com.osstelecom.db.inventory.topology.impact.WeakNodesImpactManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.osstelecom.db.inventory.topology.impact.ImpactManagerIf;

/**
 *
 * @author Nishisan
 */
public class DefaultExample {

    public DefaultExample() throws GraphNotEnabledException {
//WeakNodesImpactManager a =;
        DefaultTopology topology = new DefaultTopology(new WeakNodesImpactManager());
//        topology.setImpactManager();
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
        DefaultNode router7 = new DefaultNode("router7", 7, topology);
//        DefaultNode router8 = new DefaultNode("router8", 8, topology);
//        DefaultNode router9 = new DefaultNode("router9", 9, topology);
//        DefaultNode router10 = new DefaultNode("router10", 10, topology);
//        //
        // Conexões
        //
        topology.addConnection(router1, saida);

        topology.addConnection(router2, saida);

        topology.addConnection(router2, router1);
        topology.addConnection(router3, router2);

        topology.addConnection(router3, router4);

        topology.addConnection(router5, router4);
        topology.addConnection(router6, router4);
        topology.addConnection(router7, router6);
        topology.addConnection(router7, router5);
//        topology.addConnection(router7, router8);
//        topology.addConnection(router7, router9);
//        topology.addConnection(router10, router9);
//        topology.addConnection(router10, router8);
//        topology.addConnection(router10, router1);

        Boolean stressMe = true;
        if (stressMe) {
            Integer fakeNodCount = 500;
//            ConcurrentHashMap<String, INetworkNode> nodes = new ConcurrentHashMap<>();
            for (int x = 0; x < fakeNodCount; x++) {
                DefaultNode router = new DefaultNode("DYN-1-" + x, 10000 + x, topology);
                topology.addConnection(router, router1);
                topology.addConnection(router, router2);
                System.out.println("Created Node:" + router.getName() + " Connections Size:" + router.getConnectionCount() + " ED:" + router.getEndpointConnectionsCount());
//                nodes.put(router.getName(), router);

            }

        }
        ImpactManagerIf impactManager = topology.getImpactManager();

        System.out.println("Topologyy Size: " + topology.getNodes().size() + "  Connections:" + topology.getConnections().size());
        System.out.println("-------------------------------------------------------------");
        System.out.println("Weak Nodes:");
        System.out.println("-------------------------------------------------------------");
        Long start = System.currentTimeMillis();
        List<INetworkNode> weak = topology.getImpactManager().getWeakNodes(1, false, 16, false);
        Long end = System.currentTimeMillis();
        Long took = end - start;
        System.out.println("Found " + weak.size() + " Weak Nodes Took:" + took + " ms");
        for (INetworkNode n : weak) {
            System.out.println("  ::Weak " + n.getName() + " Endpoint Connections size:" + n.getEndpointConnectionsCount() + " Total:" + n.getActiveConnnectionsCount() + " Probed:" + n.getProbedConnectionsCount());
        }

//        List<INetworkNode> allNodes = topology.getNodes();
//        for (INetworkNode node : allNodes) {
//            for (INetworkConnection connection : node.getConnections()) {
//                System.out.println(": Connection: " + connection.leadsToEndpoint());
////                connection.printPathList();
//            }
//        }
//        List<INetworkNode> unreacheble = topology.getImpactManager().getUnreacheableNodes();
//        System.out.println("Found " + unreacheble.size() + " Unreacheable Nodes");
        System.out.println("Done...");

//        topology.getGraph().display();
    }

    private void printNodeList(ArrayList<INetworkNode> nodes) {
        System.out.println("");
//        System.out.println("Source Node:" + sourceNode.getName() + " Reaches:" + saida.getName() + " Via:");
        System.out.print("   [[");
        for (INetworkNode n : nodes) {
            System.out.print(n.getName() + ".");
        }
        System.out.println("]]");
    }

    /**
     *
     *
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            new DefaultExample();
        } catch (GraphNotEnabledException ex) {
            Logger.getLogger(DefaultExample.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
