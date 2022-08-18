/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.osstelecom.db.inventory.manager.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osstelecom.db.inventory.manager.http.IMHttpClient;
import com.osstelecom.db.inventory.manager.http.request.FilterRequest;
import com.osstelecom.db.inventory.manager.http.response.GetDomainsResponse;
import com.osstelecom.db.inventory.manager.http.response.GetFilterResponse;
import com.osstelecom.db.inventory.manager.objects.Domain;
import com.osstelecom.db.inventory.manager.objects.Filter;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 17.08.2022
 */
public class InventoryManagerClient {

    private final String authToken;
    private final String baseUrl;
    private ObjectMapper objectMapper = new ObjectMapper();
    private IMHttpClient httpClient;

    public InventoryManagerClient(String baseUrl, String authToken) {
        this.authToken = authToken;
        this.baseUrl = baseUrl;
        this.httpClient = new IMHttpClient(authToken);
    }

    /**
     * Gets the domain in the server
     *
     * @return
     * @throws IOException
     */
    public ArrayList<Domain> listDomains() throws IOException {
        String url = this.baseUrl + "/inventory/v1/domain/";
        String result = this.httpClient.getUrl(url);
        GetDomainsResponse response = objectMapper.readValue(result, GetDomainsResponse.class);
        return response.getPayLoad();
    }

    public Filter findResourcesByFilter(Filter filter) throws JsonProcessingException, IOException {
        String url = this.baseUrl + "/inventory/v1/" + filter.getDomain() + "/filter";
        FilterRequest request = new FilterRequest(filter);
        String requestJson = objectMapper.writeValueAsString(request);
        
        System.out.println("Requesting:" + requestJson);
        String result = this.httpClient.postUrl(url, requestJson);
        GetFilterResponse response = objectMapper.readValue(result, GetFilterResponse.class);
        return response.getPayLoad();
    } 

}
