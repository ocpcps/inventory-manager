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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.stream.JsonReader;
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
    private Gson gson = new GsonBuilder().create();
    /**
     * Retrieves a icon by id
     *
     * @param icon
     * @return
     * @throws ResourceNotFoundException
 
     */ 
    public IconModel getIconById(IconModel icon) throws ResourceNotFoundException
           {
        if(iconsDir == null){
            this.iconsDir = configurationManager.loadConfiguration().getIconsDir();
        }
        
        try{
            Path path =  Paths.get(iconsDir+File.separator+icon.getSchemaName()+".json");
            FileReader reader = new FileReader(path.toFile());
            icon = gson.fromJson(reader, IconModel.class);

        }
        catch(FileNotFoundException  e){
            return new IconModel();
        }
        catch (IOException x) {
            // File permission problems are caught here.
            System.err.println(x);
        }
        return icon;
       
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
        Path fileDelete = Paths.get(iconsDir+File.separator+iconModel.getSchemaName()+".json");
        //Path fileDelete = Paths.get("C:\\icons/"+iconModel.getSchemaName()+".json");
        try{
            Files.delete(fileDelete);
            return iconModel;
        }
        catch(FileNotFoundException  e){
            throw new ResourceNotFoundException(e.getMessage());
        }
         catch (NoSuchFileException x) {
            System.err.format("%s: no such" + " file or directory%n", fileDelete);
        } catch (DirectoryNotEmptyException x) {
            System.err.format("%s not empty%n", fileDelete);
        } catch (IOException x) {
            // File permission problems are caught here.
            System.err.println(x);
        }
        return iconModel;

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
           writer.close();
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


    // public Set<String> listFilesUsingFilesList(FilterDTO filter) throws IOException {
    //     if(iconsDir == null){
    //         this.iconsDir = configurationManager.loadConfiguration().getIconsDir();
    //     }
    //     if(filter.getOffSet() != null){
    //         arquivos.skip(filter.getOffSet()); 
    //     }

    //     if(filter.getLimit() != null){
    //         arquivos.limit(filter.getLimit()); 
    //     }    
    //     try (Stream<Path> stream = Files.list(Paths.get(this.iconsDir))) {
    //         return stream
    //           .filter(file -> !Files.isDirectory(file))
    //           .map(Path::getFileName)
    //           .map(Path::toString)
    //           .collect(Collectors.toSet());
    //     }
    // }

    public List<IconModel> findIconByFilter(FilterDTO filter) throws IOException   {
        List<IconModel> result = new ArrayList<>();

        if(iconsDir == null){
            this.iconsDir = configurationManager.loadConfiguration().getIconsDir();
        }
        
        Stream<Path> arquivos = Files.list(new File("./icons").toPath()).sorted();

        if(StringUtils.isNotEmpty(filter.getAqlFilter())){
            arquivos = arquivos.filter(path -> path.toString().contains(filter.getAqlFilter()));
        }
        if(filter.getOffSet() >= 0){
            arquivos = arquivos.skip(filter.getOffSet()); 
        }

        if(filter.getLimit() >= 0 && StringUtils.isEmpty(filter.getAqlFilter())){
            arquivos = arquivos.limit(filter.getLimit()); 
        }        
        arquivos.forEachOrdered((a)->{         
            try{
                FileReader reader = new FileReader(a.toFile());
                IconModel icon = gson.fromJson(reader, IconModel.class);
                result.add(icon);
            }catch(IOException e){    
           
            }
        }) ;

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
                writer.close();
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
