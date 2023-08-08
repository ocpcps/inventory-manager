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
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura
 * @created 15.12.2021
 */
@Service
public class UtilSession {

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Returns the md5 hash from the string
     *
     * @param input
     * @return
     */
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

    /**
     * Verifica se um nome é valido ou seja, só contém Letras, números pontos ,
     * traços ou underline.
     *
     * @param value
     * @return
     */
    public Boolean isValidStringValue(String value) {
//        if (value!=null)
//        if (value.startsWith(" ") || value.endsWith(" ")){
//            return false;
//        }
        return this.isValidStringValue(value, "^[a-zA-Z0-9\\-\\.\\_]+$");
    }

    public Boolean isValidStringValue(String value, String regex) {
        return value.matches(regex);
    }

    /**
     * Gets the google GSON Object Serializer
     *
     * @return
     */
    public Gson getGson() {
        return this.gson;
    }

    /**
     * Return a UUID Request ID
     *
     * @return
     */
    public synchronized String getRequestId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Return a UUID Response ID
     *
     * @return
     */
    public synchronized String getResponseId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Tries to serialize an object to json using google gson
     *
     * @param obj
     * @return
     */
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(UtilSession.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "{}";
    }

    /**
     * Valida se o Basic Resource possui uma convenção de nome correta
     *
     * @param resource
     * @throws InvalidRequestException
     */
    public void validadeNodeAddressAndName(BasicResource resource) throws InvalidRequestException {
        //
        // Valida os padrões de Nome do Node Address
        //
        if (resource.getNodeAddress() != null) {
            if (!this.isValidStringValue(resource.getNodeAddress())) {
                throw new InvalidRequestException("Invalid Node Address [" + resource.getNodeAddress() + "]");
            }
        }

        //
        // No Nome Permite, Letras, Números, Pontos , Espaços,Acentos e "/"
        //
        if (resource.getName() != null) {
            if (!this.isValidStringValue(resource.getName(), "^[a-zA-Z0-9\\.\\-áàâãéèêíïóôõöúçñÁÀÂÃÉÈÊÍÏÓÔÕÖÚÇÑ\\s/\\_&\\(\\)]+$")) {
                throw new InvalidRequestException("Invalid Node Name:[" + resource.getName() + "]");
            }
        }

    }

    public void validateCanonicalName(BasicResource resource) throws InvalidRequestException {
        if (resource.getAttributeSchemaName() != null && resource.getClassName() != null) {
            if (!this.isValidStringValue(resource.getAttributeSchemaName(), "^[a-zA-Z0-9\\.\\-]+$")) {
                throw new InvalidRequestException("Invalid Attribute Schema Name:[" + resource.getAttributeSchemaName() + "]");
            }

            if (!this.isValidStringValue(resource.getClassName(), "^[a-zA-Z0-9\\.\\-]+$")) {
                throw new InvalidRequestException("Invalid Class Name Name:[" + resource.getClassName() + "]");
            }
        } else {
            throw new InvalidRequestException("Please Provide Atribute Schema Name and ClassName Values");
        }
    }
}
