package com.osstelecom.db.inventory.manager;

import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

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
 * @created 14.12.2021
 */
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class InventoryManagerApplication {

    private Logger logger = LoggerFactory.getLogger(InventoryManagerApplication.class);

    public static void main(String[] args) {
//        SpringApplication.run(InventoryManagerApplication.class, args);
        SpringApplication application = new SpringApplication(InventoryManagerApplication.class);
        application.setBannerMode(Banner.Mode.CONSOLE);
        application.run(args);
    }

    @EventListener(ApplicationReadyEvent.class)
    private void onStartup() {
        logger.debug("Hello Master of Universe, Nice to see you :) Inventory Manager Starting...");
    }

    @PreDestroy
    private void onShutDown() {
        logger.debug("Bye Master of Universe,Inventory Manager Shutting Down...");
    }
}
