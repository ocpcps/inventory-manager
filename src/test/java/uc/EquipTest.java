/*
 * Copyright (C) 2021 Lucas Nishimura <lucas.nishimura@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uc;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 */
public class EquipTest {

    public static void main(String[] args) {
        
        String nome = "lucas.nishimura9";
        
        if (!nome.matches("[a-z,A-Z,0-9,\\.,\\-]")){
            System.out.println("HELLO");
        }
//        try {
//            DomainManager domainManager = new DomainManager();
//
//            ResourceLocation dataCenter = domainManager.createResourceLocation("CTA.PV");
//            dataCenter.setCity("Curitiba");
//            dataCenter.setAdminStatus("UP");
//            dataCenter.setOperationalStatus("UP");
//            dataCenter.setIsConsumable(false);
//
//            ResourceLocation andar = domainManager.createResourceLocation("CTA.PV.1");
//            andar.setCity("Curitiba.1");
//            andar.setAdminStatus("UP");
//            andar.setOperationalStatus("UP");
//            andar.setIsConsumable(false);
//
////            dataCenter.getAttachments().put(key, args)
//            ConsumableMetric dcMetric = domainManager.createConsumableMetric("DC ROW");
//            dcMetric.setMaxValue(20D);
//            dcMetric.setMinValue(0D);
//            dcMetric.setUnitValue(1D);
////            dcMetric.setMetricName("DC ROW");
//            dcMetric.setMetricValue(10D);
//            dataCenter.setConsumableMetric(dcMetric);
//
//            ConsumableMetric rackMetric = domainManager.createConsumableMetric("1U Rack Unit");
//            rackMetric.setMaxValue(48D);
//            rackMetric.setMinValue(0D);
//            rackMetric.setMetricValue(48D);
//            rackMetric.setMetricName("1U Rack Unit");
//
//            rackMetric.setMetricDescription("Unit");
//            rackMetric.setMetricShort("U");
//            rackMetric.setUnitValue(1D);
//
//            ManagedResource rack = domainManager.createManagedResource("RACK-1");
////            rack.getAttachments().put("foto-1", rack)
//            rack.setIsConsumable(true);
//            rack.setConsumableMetric(rackMetric);
//
//            rack.setAdminStatus("UP");
//            rack.setOperationalStatus("UP");
//
//            ManagedResource server = domainManager.createManagedResource("SVUXPVOC1");
//
//            server.setAdminStatus("UP");
//            server.setOperationalStatus("UP");
//            server.setIsConsumer(true);
//            server.setConsumerMetric(rackMetric);
//
//            ManagedResource shelf = domainManager.createManagedResource("SHELF");
//
//            ManagedResource slot1 = domainManager.createManagedResource("SLOT-1");
//
//            domainManager.createResourceConnection(shelf, slot1);
//
//            ResourceConnection connection = domainManager.createResourceConnection(rack, server);
//            connection.setPropagateConsuption(true);
////            domainManager.createResourceConnection(rack, server);
//
//        } catch (Exception ex) {
//            System.err.println("Error: " + ex.getMessage());
//            ex.printStackTrace();
//        }

    }

}
