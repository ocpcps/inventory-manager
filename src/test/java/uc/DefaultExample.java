/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uc;

import com.osstelecom.db.inventory.topology.node.DefaultNode;
import com.osstelecom.db.inventory.topology.node.INetworkNode;
import com.osstelecom.db.inventory.topology.DefaultTopology;
import com.osstelecom.db.inventory.topology.exception.GraphNotEnabledException;
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
        Long start = System.currentTimeMillis();
        DefaultTopology topology = new DefaultTopology();

        List<String> nomes = new ArrayList<>();
        nomes.add("Nishimura");
        nomes.add("Lucas");
        nomes.add("Debora");

        for (String nome:nomes){
            nome = "la";
            System.out.println(nome);
        }
        
        //
        // Elementos da Topologia
        //
        DefaultNode saida = new DefaultNode("saida1", 0, topology);

        saida.setEndPoint(true);

        DefaultNode router1 = new DefaultNode("router1", 1, topology);
        DefaultNode router2 = new DefaultNode("router2", 2, topology);
        DefaultNode router3 = new DefaultNode("router3", 3, topology);
        DefaultNode router4 = new DefaultNode("router4", 4, topology);
        DefaultNode router5 = new DefaultNode("router5", 5, topology);
        DefaultNode router6 = new DefaultNode("router6", 6, topology);
        DefaultNode router7 = new DefaultNode("router7", 7, topology);
        //
        // Conexões
        //

        topology.addConnection(router1, saida,"r1-s1");

        topology.addConnection(router2, saida,"r2-s1");

        topology.addConnection(router2, router1,"r2-r1");
        topology.addConnection(router3, router2,"r3-r2");

        topology.addConnection(router3, router4,"r3-r4");

        topology.addConnection(router5, router4,"r5-r4");
        topology.addConnection(router6, router4,"r6-r4");
        topology.addConnection(router7, router6,"r7-r6");
        topology.addConnection(router7, router5,"r7-r5");
//        topology.addConnection(router5, router1);

//        HashMap<String, INetworkNode> nodesDinamicos = new HashMap<>();
//        for (int x = 0; x < 100; x++) {
//            DefaultNode router = new DefaultNode("Dyn1[" + x + "]", 1000 + x, topology);
//            nodesDinamicos.put(router.getName(), router);
//            topology.addConnection(router, router2);
//            topology.addConnection(router, router1);
//
//        }
//
//        for (int x = 0; x < 100; x++) {
//            DefaultNode router = new DefaultNode("Dyn2[" + x + "]", 1000 + x, topology);
//            topology.addConnection(router, nodesDinamicos.get("Dyn1[" + x + "]"));
//            topology.addConnection(router, router2);
//            topology.addConnection(router, router1);
//
//        }
        ArrayList<INetworkNode> nodes = new ArrayList<>();

        ImpactManagerIf impactManager = topology.getImpactManager();
        System.out.println("Topologyy Size: " + topology.getNodes().size() + "  Connections:" + topology.getConnections().size());
        System.out.println("-------------------------------------------------------------");
        System.out.println("Weak Nodes:");
        System.out.println("-------------------------------------------------------------");
        List<INetworkNode> weak = topology.getImpactManager().getWeakNodes(1, false,1,false);
        System.out.println("Found " + weak.size() + " Weak Nodes");
        for (INetworkNode n : weak) {
            System.out.println("  ::Weak " + n.getName() + " Connections size:" + n.getEndpointConnectionsCount());
        }

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
            new DefaultExample();
        } catch (GraphNotEnabledException ex) {
            Logger.getLogger(DefaultExample.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
