package com.osstelecom.db.inventory.manager.configuration;

import java.util.concurrent.locks.ReentrantLock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LockManagerConfig {
       
    @Bean
    public ReentrantLock lockManager(){
        return new ReentrantLock();
    }

}
