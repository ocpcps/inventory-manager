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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.CollectionCreateOptions;

/**
 *
 * Classe ArangoDBStartup
 *
 * <p>
 * Descrição: Classe de configuração para a inicialização do banco de dados
 * ArangoDB. Esta classe define um bean para a criação e configuração da conexão
 * com o banco de dados ArangoDB.</p>
 * <p>
 * Configurações:
 * <ul>
 * <li>logger: Logger utilizado para realizar o registro de mensagens durante a
 * inicialização do banco de dados.</li>
 * </ul>
 * </p>
 * <p>
 * Métodos:
 * <ul>
 * <li>arangoDatabase: Método para criar e configurar a conexão com o banco de
 * dados ArangoDB.</li>
 * </ul>
 * </p>
 * <p>
 * Exceptions:
 * <ul>
 * <li>Não há exceções específicas lançadas por esta classe.</li>
 * </ul>
 * </p>
 *
 * @version 1.0
 *
 *
 */
@Configuration
public class ArangoDBStartup {

    private Logger logger = LoggerFactory.getLogger(ArangoDBStartup.class);

    @Bean
    public ArangoDatabase arangoDatabase(ConfigurationManager configurationManager) {

        InventoryConfiguration inventoryConfiguration = configurationManager.loadConfiguration();
        ArangoDBConfiguration arangoDbConfiguration = inventoryConfiguration.getGraphDbConfiguration();

        ArangoDB graphDb = new ArangoDB.Builder()
                .host(arangoDbConfiguration.getHost(), arangoDbConfiguration.getPort())
                .user(arangoDbConfiguration.getUser())
                .password(arangoDbConfiguration.getPassword())
                .maxConnections(32)
                .connectionTtl(300L)
                .build();
        ArangoDatabase database = graphDb.db(arangoDbConfiguration.getDatabaseName());

        if (!database.exists()) {
            logger.warn("ERROR DB DOES NOT EXISTS... TRYING TO CREATE IT...");
            database.create();
        }
        ArangoCollection domainsCollection = database.collection(arangoDbConfiguration.getDomainsCollection());
        if (!domainsCollection.exists()) {
            domainsCollection.create(new CollectionCreateOptions().type(CollectionType.DOCUMENT));
        }

        try {
            logger.info(".........................................");
            logger.info("Graph DB Connected to: [{} {}]", graphDb.getVersion().getServer(),
                    graphDb.getVersion().getVersion());
            logger.info("Listing Databases:....");
            for (String arangoDbName : graphDb.getDatabases()) {
                if (arangoDbConfiguration.getDatabaseName().equals(arangoDbName)) {
                    logger.info("\tDB:.....: {}\t <---- USING THIS ONE :)", arangoDbName);
                } else {
                    logger.info("\tDB:.....: {}", arangoDbName);
                }
            }
            logger.info(".........................................");
        } catch (ArangoDBException ex) {
            logger.error("Failed GraphDB:", ex);
        }
        return database;
    }

}
