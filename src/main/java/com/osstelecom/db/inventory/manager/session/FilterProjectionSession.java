package com.osstelecom.db.inventory.manager.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.response.FilterResponse;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vamos tentar fazer uma projeção de dados para remover campos indesejaveis
 */
@Service
public class FilterProjectionSession {

    @Autowired
    private UtilSession utils;

    private Logger logger = LoggerFactory.getLogger(FilterProjectionSession.class);

    /**
     * Removes all Except the fields in the filterDTO Exemplo do Filtro:
     * Altamente Experimental
     *
     * @param <T>
     * @param filterDTO
     * @param response
     * @param list
     * @return
     */
    public FilterResponse filterProjection(FilterDTO filterDTO, FilterResponse response) {
        if (!filterDTO.getFields().isEmpty()) {
            try {
                /**
                 * Encontramos fields para filtrar, o problema é que este método
                 * vai exigir bastante memória pois vai trabalhar com maps e
                 * listas.
                 */

                /**
                 * @todo: avaliar se no lugar do GSON , não é melhor utilizar o
                 * Jackson
                 */
                String jsonString = utils.getGson().toJson(response);
                jsonString = this.filterJson(jsonString, filterDTO.getFields());
                FilterResponse filteredResponse = utils.getGson().fromJson(jsonString, FilterResponse.class);
                //
                // Se chegou aqui conseguiu filtrar a response.
                //

                return filteredResponse;
            } catch (Exception ex) {
                //
                // Omite O erro
                //
                logger.error("Failed to Apply Filter", ex);
            }
        }
        //
        // Se chegou aqui é porque não conseguiu filtrar a reponse T_T
        //
        return response;
    }

    private String filterJson(String json, List<String> fields) {
        Set<String> fieldSet = new HashSet<>(fields);
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            JsonNode result = filter(jsonNode, fieldSet, "", objectMapper);

            return result != null ? result.toString() : null;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao processar o JSON", e);
        }
    }

    private JsonNode filter(JsonNode jsonNode, Set<String> fields, String currentPath, ObjectMapper objectMapper) {
        if (jsonNode.isObject()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            jsonNode.fields().forEachRemaining(field -> {
                String updatedPath = currentPath.isEmpty() ? field.getKey() : currentPath + "." + field.getKey();
                if (fields.contains(updatedPath) || anyStartsWith(fields, updatedPath) || isWildcardMatch(fields, currentPath)) {
                    JsonNode child = filter(field.getValue(), fields, updatedPath, objectMapper);
                    if (child != null) {
                        objectNode.set(field.getKey(), child);
                    }
                }
            });
            return objectNode;
        } else if (jsonNode.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            jsonNode.elements().forEachRemaining(element -> {
                JsonNode child = filter(element, fields, currentPath + ".[*]", objectMapper);
                if (child != null && !child.isEmpty(objectMapper.getSerializerProvider())) {
                    arrayNode.add(child);
                }
            });
            return arrayNode;
        } else {
            return jsonNode.deepCopy();
        }
    }

    private boolean anyStartsWith(Set<String> fields, String path) {
        return fields.stream().anyMatch(field -> field.startsWith(path + "."));
    }

    private boolean isWildcardMatch(Set<String> fields, String path) {
        return fields.contains(path + ".*") || fields.contains(path + ".[*].*");
    }

}
