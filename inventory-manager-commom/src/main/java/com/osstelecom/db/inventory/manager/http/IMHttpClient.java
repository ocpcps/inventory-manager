/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.osstelecom.db.inventory.manager.http;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 17.08.2022
 */
public class IMHttpClient {

    private final OkHttpClient client;
    private final String authToken;

    public IMHttpClient(String authToken) {
        this.authToken = authToken;
        this.client = new OkHttpClient.Builder()
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    public String getUrl(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url).addHeader("x-auth-token", authToken)
                .build();
        Response getResponse = client.newCall(request).execute();
        return getResponse.body().string();
    }

    public String postUrl(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url).addHeader("x-auth-token", authToken)
                .post(body)
                .build();
        Response getResponse = client.newCall(request).execute();
        return getResponse.body().string();
    }

    public String putUrl(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url).addHeader("x-auth-token", authToken)
                .put(body)
                .build();
        Response getResponse = client.newCall(request).execute();
        return getResponse.body().string();
    }

    public String pathUrl(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url).addHeader("x-auth-token", authToken)
                .patch(body)
                .build();
        Response getResponse = client.newCall(request).execute();
        return getResponse.body().string();
    }

    public String delUrl(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url).addHeader("x-auth-token", authToken)
                .delete()
                .build();
        Response getResponse = client.newCall(request).execute();
        return getResponse.body().string();
    }

}
