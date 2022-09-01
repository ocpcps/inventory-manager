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
package com.osstelecom.db.inventory.manager.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.osstelecom.db.inventory.manager.configuration.InventoryConfiguration;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 15.12.2021
 */
@Service
public class UtilSession {

    private MessageDigest md;
    private InventoryConfiguration configuration;
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ObjectMapper objectMapper = new ObjectMapper();

    public String getMd5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(input.getBytes(StandardCharsets.UTF_8));
            final byte[] resultByte = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < resultByte.length; ++i) {
                sb.append(Integer.toHexString((resultByte[i] & 0xFF) | 0x100).substring(1, 3));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {

        }
        return "";

    }

    public Gson getGson() {
        return this.gson;
    }

    public synchronized String getRequestId() {
        return UUID.randomUUID().toString();
    }

    public synchronized String getResponseId() {
        return UUID.randomUUID().toString();
    }

    public String toJson(Object obj){
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(UtilSession.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "{}";
    }
}
