
import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.resources.ResourceLocation;
import com.osstelecom.db.inventory.manager.resources.exception.ConnectionAlreadyExistsException;
import com.osstelecom.db.inventory.manager.resources.exception.MetricConstraintException;
import com.osstelecom.db.inventory.manager.resources.exception.NoResourcesAvailableException;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Copyright (C) 2021 Lucas Nishimura <lucas.nishimura@gmail.com>
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
/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 16.12.2021
 */
public class TesteFlavio {

    public static void main(String[] args) {

        DomainManager manager = new DomainManager();

        ResourceLocation brasil = new ResourceLocation();
        brasil.setName("Brasil");
        brasil.setClassName("Country");

        ResourceLocation parana = new ResourceLocation();
        parana.setName("Paraná");
        parana.setClassName("State");

        ResourceLocation curitiba = new ResourceLocation();
        curitiba.setName("Curitiba");
        curitiba.setClassName("City");
        
        curitiba.getAttributes().put("hostname", "ajajaj");
        curitiba.getAttributes().put("vendor", "ajajaj");
        curitiba.getAttributes().put("model", "@parent.model"); // virtual attribute
        
        try {
            manager.createResourceConnection(brasil, parana, new DomainDTO());
            manager.createResourceConnection(parana, curitiba, new DomainDTO());
        } catch (ConnectionAlreadyExistsException | MetricConstraintException | NoResourcesAvailableException | GenericException ex) {
            Logger.getLogger(TesteFlavio.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
