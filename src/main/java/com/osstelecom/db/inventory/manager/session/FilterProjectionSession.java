package com.osstelecom.db.inventory.manager.session;

import org.apache.groovy.json.internal.LazyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.response.FilterResponse;

import groovy.json.JsonSlurper;

/**
 * Vamos tentar fazer uma projeção de dados para remover campos indesejaveis
 */
@Service
public class FilterProjectionSession {

    @Autowired
    private UtilSession utils;

    private JsonSlurper slurper = new JsonSlurper();

    /**
     * Removes all Except the fields in the filterDTO
     * Exemplo do Filtro:
     * Altamente Experimental
     * @param <T>
     * @param filterDTO
     * @param list
     * @return
     */
    public FilterResponse filterProjection(FilterDTO filterDTO, FilterResponse response) {
        if (!filterDTO.getFields().isEmpty()) {
            /**
             * Encontramos fields para filtrar, o problema é que este método vai exigir
             * bastante memória pois vai trabalhar com maps e listas.
             */             
            String jsonString = utils.getGson().toJson(response);
            LazyMap map = (LazyMap) slurper.parseText(jsonString);
            

        }
        return response;
    }

}
