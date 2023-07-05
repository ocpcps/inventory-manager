package com.osstelecom.db.inventory.manager.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.response.FilterResponse;

<<<<<<< HEAD (eec0e65) - merging
import groovy.json.JsonSlurper;
=======
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
>>>>>>> feature/performance (1dd71d9) - bump

/**
 * Vamos tentar fazer uma projeção de dados para remover campos indesejaveis
 */
@Service
public class FilterProjectionSession {

    @Autowired
    private UtilSession utils;

<<<<<<< HEAD (eec0e65) - merging
    private JsonSlurper slurper = new JsonSlurper();

    /**
     * Removes all Except the fields in the filterDTO
     * Exemplo do Filtro:
     * Altamente Experimental
     * @param <T>
     * @param filterDTO
=======
    private Logger logger = LoggerFactory.getLogger(FilterProjectionSession.class);

    /**
     * Removes all Except the fields in the filterDTO Exemplo do Filtro:
     * Altamente Experimental
     *
     * @param <T>
     * @param filterDTO
     * @param response
>>>>>>> feature/performance (1dd71d9) - bump
     * @param list
     * @return
     */
    public FilterResponse filterProjection(FilterDTO filterDTO, FilterResponse response) {
        if (!filterDTO.getFields().isEmpty()) {
}
