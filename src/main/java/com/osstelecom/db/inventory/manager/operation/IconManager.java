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
package com.osstelecom.db.inventory.manager.operation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import javax.management.ServiceNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.arangodb.entity.DocumentUpdateEntity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.osstelecom.db.inventory.manager.configuration.ConfigurationManager;
import com.osstelecom.db.inventory.manager.configuration.InventoryConfiguration;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.model.IconModel;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.session.DynamicRuleSession;

@Service
public class IconManager extends Manager {
    
    @Autowired
    private ConfigurationManager configurationManager;
    
    private String iconsDir;
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    /**
     * Retrieves a icon by id
     *
     * @param icon
     * @return
     * @throws ResourceNotFoundException
 
     */ 
    public IconModel getIconById(IconModel icon)
            throws ResourceNotFoundException {
        if(iconsDir == null){
            this.iconsDir = configurationManager.loadConfiguration().getIconsDir();
        }
        String fileSeparator = File.separator;
        try{
            FileReader reader = new FileReader(iconsDir+fileSeparator+icon.getSchemaName());
            return gson.fromJson(reader, IconModel.class);
            
        }
        catch(FileNotFoundException  e){
            throw new ResourceNotFoundException(e.getMessage());
        }
       
    }

    /**
     * Deleta um Icone
     *
     * @param icon
     * @return
     * @throws IOException
     * @throws ResourceNotFoundException
     */
    public IconModel deleteIcon(IconModel iconModel) throws ResourceNotFoundException, IOException {
        if(iconsDir == null){
            this.iconsDir = configurationManager.loadConfiguration().getIconsDir();
        }
        String fileSeparator = File.separator;
        try{
            Files.delete(Paths.get(iconsDir+fileSeparator+iconModel.getSchemaName()));
            return iconModel;
        }
        catch(FileNotFoundException  e){
            throw new ResourceNotFoundException(e.getMessage());
        }
     

    }

    /**
     * Cria um novo icone
     *
     * @param icone
     * @return
     * @throws GenericException
     */
    public IconModel createIcon(IconModel iconModel) throws GenericException {

        if(iconsDir == null){
            this.iconsDir = configurationManager.loadConfiguration().getIconsDir();
        }
        String fileSeparator = File.separator;
        try{
            FileWriter writer = new FileWriter(iconsDir+fileSeparator+iconModel.getSchemaName());

            gson.toJson(iconModel, writer);
            return iconModel;
        }
        catch(JsonIOException | IOException  e){
            throw new GenericException(e.getMessage());
        }

           
        
    }

    /**
     *
     * @param newService
     * @param oldService
     * @throws ArangoDaoException
     */
    public List<IconModel> findIconByFilter(FilterDTO filter) throws ArangoDaoException, ResourceNotFoundException {
        return this.iconDao.findResourceByFilter(filter);
    }
    
    public IconModel updateIcon(IconModel iconModel) throws GenericException {
        
        if(iconsDir == null){
            this.iconsDir = configurationManager.loadConfiguration().getIconsDir();
        }
        String fileSeparator = File.separator;
        try{
            if(Boolean.TRUE.equals(fileExist(iconsDir+fileSeparator+iconModel.getSchemaName()))){
                FileWriter writer = new FileWriter(iconsDir+fileSeparator+iconModel.getSchemaName());
                gson.toJson(iconModel, writer);
                
            }
            return iconModel;
        }
        catch(JsonIOException | IOException  e){
            throw new GenericException(e.getMessage());
        }
    }

    /**
     * @param fileName
     * @return
     * @throws GenericException
     */

    public Boolean fileExist(String fileName){
        File f = new File(fileName);
        return f.exists();
    }

 
}
