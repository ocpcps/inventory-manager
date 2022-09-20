/*
 * Copyright (C) 2022 Lucas Nishimura <lucas.nishimura@gmail.com>
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
package com.osstelecom.db.inventory.manager.session;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.osstelecom.db.inventory.manager.configuration.ConfigurationManager;
import com.osstelecom.db.inventory.manager.configuration.InventoryConfiguration;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 03.01.2022
 */
@Service
public class DynamicRuleSession {

    private GroovyScriptEngine gse;
    private InventoryConfiguration configuration;
    private Binding bindings;
    private Logger logger = LoggerFactory.getLogger(DynamicRuleSession.class);
    private Cache<String, File> cachedFile;
    private Cache<String, Boolean> cachedNoRules;
    private LinkedHashMap<String, Object> context = new LinkedHashMap<>();
    @Autowired
    private ConfigurationManager configurationManager;

    @EventListener(ApplicationReadyEvent.class)
    private void initGse() {
        try {
            this.configuration = configurationManager.loadConfiguration();
            logger.info("Starting Dynamic Rule Engine with rules dir at: [{}]", this.configuration.getRulesDir());
            this.gse = new GroovyScriptEngine(configuration.getRulesDir());
            this.bindings = new Binding();
            this.bindings.setVariable("logger", logger);
            this.initGuavaCache();
        } catch (IOException ex) {
            logger.error("Failed to start Rules Engine...");
        }
    }

    /**
     * Inicializa um Cache válido por 30s.
     */
    private void initGuavaCache() {
        this.cachedFile = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(30, TimeUnit.HOURS).build();
        this.cachedNoRules = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(5, TimeUnit.SECONDS).build();
    }

    /**
     * Avalia no groovy se podemos continuar com a transação
     *
     * @param resource
     * @param domainManager
     * @throws ScriptRuleException
     */
    public void evalResource(BasicResource resource, String oper, Object manager) throws ScriptRuleException {
        //
        // Eval the resource chain
        //

        if (this.configurationManager.loadConfiguration().getDynamicRulesEnabled()) {
            String scriptPath = resource.getClassName();

            scriptPath = scriptPath.replaceAll("\\.", "/");
            scriptPath += ".groovy";

            if (this.cachedNoRules.getIfPresent(scriptPath) == null) {
                File scriptFile = this.cachedFile.getIfPresent(scriptPath);
                if (scriptFile == null) {
                    scriptFile = new File(configuration.getRulesDir() + scriptPath);
                    this.cachedFile.put(scriptPath, scriptFile);
                }

                if (scriptFile.exists()) {
                    this.cachedNoRules.invalidate(scriptPath);
                    context.clear();
                    this.bindings.setVariable("manager", manager);
                    this.bindings.setVariable("context", context);

                    context.put("manager", manager);
                    context.put("resource", resource);
                    context.put("oper", oper);

                    String runningScript = scriptPath;
                    this.bindings.setVariable("resource", resource);
                    while (!scriptPath.equals("")) {
                        try {
                            scriptPath = "";
                            context.put("include", scriptPath);
                            this.gse.run(runningScript, bindings);
                            scriptPath = (String) context.get("include");
                            if (StringUtils.hasText(scriptPath)) {
                                runningScript = scriptPath;
                            } else {
                                scriptPath = "";
                            }
                        } catch (ResourceException | ScriptException ex) {
                            throw new ScriptRuleException("Error in Groovy Context", ex);
                        } catch (Exception ex) {
                            throw new ScriptRuleException("Generic Exception in Groovy Context", ex);
                        }
                    }
                    context.clear();
                } else {
                    this.cachedNoRules.put(scriptPath, false);
                    logger.warn("No Rules Found for Class: {}:=[{}] Putting on Cache for 30s", resource.getClassName(), scriptPath);
                }
            } else {
                //
                // Nothing to Eval
                //
            }
        }

    }
}
