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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.osstelecom.db.inventory.manager.configuration.ConfigurationManager;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.model.IconModel;

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
           {
        if(iconsDir == null){
            this.iconsDir = configurationManager.loadConfiguration().getIconsDir();
        }
        
        try{
            FileReader reader = new FileReader(iconsDir+File.separator+icon.getSchemaName()+".json");
            return gson.fromJson(reader, IconModel.class);
            
        }
        catch(FileNotFoundException  e){
            return new IconModel();
           // throw new ResourceNotFoundException(e.getMessage());
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

        try{
            Files.delete(Paths.get(iconsDir+File.separator+iconModel.getSchemaName()+".json"));
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

        try{
            // File writer = new File(iconsDir+File.separator+iconModel.getSchemaName());
            // writer.createNewFile();
            FileWriter writer = new FileWriter(iconsDir+File.separator+iconModel.getSchemaName()+".json");
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
     * @throws IOException
     * @throws ArangoDaoException
     */
    public List<IconModel> findIconByFilter(FilterDTO filter) throws IOException   {
        List<IconModel> result = new ArrayList<>();

        if(iconsDir == null){
            this.iconsDir = configurationManager.loadConfiguration().getIconsDir();
        }
        
        Stream<Path> arquivos = Files.list(Paths.get(iconsDir)).sorted();

        if(filter.getOffSet() != null){
            arquivos.skip(filter.getOffSet()); 
        }

        if(filter.getLimit() != null){
            arquivos.limit(filter.getLimit()); 
        }        

        arquivos.forEachOrdered((a)->{            
            try(FileReader reader = new FileReader(a.toFile())){
                IconModel icon = gson.fromJson(reader, IconModel.class);
                result.add(icon);
            }catch(IOException e){                
            }
        });

        return result;
    }
    
    public IconModel updateIcon(IconModel iconModel) throws GenericException {
        
        if(iconsDir == null){
            this.iconsDir = configurationManager.loadConfiguration().getIconsDir();
        }
        
        try{
            if(Boolean.TRUE.equals(fileExist(iconsDir+File.separator+iconModel.getSchemaName()+".json"))){
                FileWriter writer = new FileWriter(iconsDir+File.separator+iconModel.getSchemaName()+".json");
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
