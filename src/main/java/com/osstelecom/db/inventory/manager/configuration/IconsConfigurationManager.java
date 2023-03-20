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
package com.osstelecom.db.inventory.manager.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Gerencia a configuração do Netcompass
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 15.12.2021
 */
@Component
public class IconsConfigurationManager {

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    private InventoryConfiguration configuration = null;


    /**
     * Procura os paths em ordem! o primeiro que encontrar ele vai usar
     *
     * @return
     */
    private ArrayList<String> getConfigurationPaths() {
        ArrayList<String> paths = new ArrayList<>();
        paths.add("c:/temp/icons.json");
        paths.add("/app/icons-manager/config/icons.json");
        paths.add("/etc/icons-manager/icons.json");
        paths.add("config/icons.json");
        String envPath = System.getenv("INVENTORY_MANAGER_CONFIG");
        if (envPath != null) {
            //
            // Variaveis de ambiente tem prioridade
            //
            paths.add(0, envPath);
        }
        return paths;
    }

    /**
     * Carrega ou Cria o arquivo de configuração
     *
     * @return
     */
    public String loadConfiguration() {
        String decodeFile  = new String(Base64.getDecoder().decode(request.getPayLoad().getValue()));
            for (String configPath : this.getConfigurationPaths()) {
                File configFile = new File(configPath);
                if (configFile.exists()) {
                    try {
                        //
                        // Load from Json
                        //

                        FileReader reader = new FileReader(configFile);
                        this.configuration = gson.fromJson(reader, InventoryConfiguration.class);
                        reader.close();
                        return decodeFile;
                    } catch (FileNotFoundException ex) {
                        logger.error("Configuration File , not found... weird exception..", ex);
                    } catch (IOException ex) {
                        logger.error("Failed to Close IO..", ex);
                    }
                    break;
                }
            }
            //
            // Não existe arquivo de configuração... vamos tentar criar...
            //
            File theConfigurationFile = new File(getConfigurationPaths().get(0));
            if (theConfigurationFile.getParentFile().exists()) {
                try {
                    FileWriter writer = new FileWriter(theConfigurationFile);
                    gson.toJson(decodeFile, writer);
                    writer.flush();
                    writer.close();
                } catch (IOException ex) {
                    logger.error("Failed to save configuration file", ex);
                } catch (JsonIOException ex) {
                    logger.error("Configuration File Produced an invalid JSON", ex);
                }
            }
            return decodeFile;
    }
}

