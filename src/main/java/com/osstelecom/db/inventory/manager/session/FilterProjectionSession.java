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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vamos tentar fazer uma projeção de dados para remover campos indesejaveis
 */
@Service
public class FilterProjectionSession {

    @Autowired
    private UtilSession utils;

    private ConcurrentHashMap<String, ArrayList<Object>> objects = new ConcurrentHashMap<>();
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
            //
            // Gera um UID de sessão
            //
            String uid = utils.getRequestId();
            try {

                this.objects.put(uid, new ArrayList<Object>());
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
                this.objects.get(uid).add(jsonString);
                String filterString = this.filterJson(jsonString, filterDTO.getFields(), uid);
                this.objects.get(uid).add(filterString);
                FilterResponse filteredResponse = utils.getGson().fromJson(filterString, FilterResponse.class);
                //
                // Se chegou aqui conseguiu filtrar a response.
                //

                return filteredResponse;
            } catch (Exception ex) {
                //
                // Omite O erro
                //
                logger.error("Failed to Apply Filter", ex);
            } finally {
                ArrayList<Object> objectsUsed = this.objects.remove(uid);
                if (objectsUsed != null) {
                    if (!objectsUsed.isEmpty()) {
                        logger.debug("Releasing [{}] from Filter Buffer", objectsUsed.size());
                        while (!objectsUsed.isEmpty()) {
                            Object o = objectsUsed.remove(0);
                            o = null;
                        }

                    }
                    objectsUsed = null;
                }
            }
        }
        //
        // Se chegou aqui é porque não conseguiu filtrar a reponse T_T
        //
        return response;
    }

    private String filterJson(String json, List<String> fields, String uid) {
        Set<String> fieldSet = new HashSet<>(fields);
        ObjectMapper objectMapper = new ObjectMapper();

        this.objects.get(uid).add(fieldSet);
        this.objects.get(uid).add(objectMapper);

        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            JsonNode result = filter(jsonNode, fieldSet, "", objectMapper, uid);
            this.objects.get(uid).add(jsonNode);
            this.objects.get(uid).add(result);
            return result != null ? result.toString() : null;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao processar o JSON", e);
        }
    }

    private JsonNode filter(JsonNode jsonNode, Set<String> fields, String currentPath, ObjectMapper objectMapper, String uid) {
        if (jsonNode.isObject()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            this.objects.get(uid).add(objectNode);
            jsonNode.fields().forEachRemaining(field -> {
                String updatedPath = currentPath.isEmpty() ? field.getKey() : currentPath + "." + field.getKey();
                this.objects.get(uid).add(updatedPath);
                if (fields.contains(updatedPath) || anyStartsWith(fields, updatedPath) || isWildcardMatch(fields, currentPath)) {
                    JsonNode child = filter(field.getValue(), fields, updatedPath, objectMapper, uid);
                    this.objects.get(uid).add(child);
                    if (child != null) {
                        objectNode.set(field.getKey(), child);
                    }
                }
            });
            return objectNode;
        } else if (jsonNode.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            this.objects.get(uid).add(arrayNode);
            jsonNode.elements().forEachRemaining(element -> {
                JsonNode child = filter(element, fields, currentPath + ".[*]", objectMapper, uid);
                this.objects.get(uid).add(child);
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
