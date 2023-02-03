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
package com.osstelecom.db.inventory.manager.rest.api;

import com.osstelecom.db.inventory.manager.dto.ApiErrorDTO;
import com.osstelecom.db.inventory.manager.exception.BasicException;
import com.osstelecom.db.inventory.manager.session.UtilSession;
import groovy.json.JsonSlurper;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Cuida das Exceptions...
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 15.12.2021
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Autowired
    private UtilSession utils;
    //
    // @since 12/12/2022
    // @uthor: Lucas Nishimura
    //
    private Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private JsonSlurper parser = new JsonSlurper();

    @ExceptionHandler(BasicException.class)
    public ResponseEntity<Object> handleGenericException(
            BasicException ex, HttpServletRequest request) {
        ex.printStackTrace();
        ApiErrorDTO apiError = new ApiErrorDTO();
        apiError.setMsg(ex.getMessage());
        apiError.setStatusCode(ex.getStatusCode());
        apiError.setClassName(ex.getClass().getSimpleName());

        if (ex.getDetails() != null) {
            apiError.setDetails(ex.getDetails());
        } else {
            //
            // Set details
            //
            apiError.setDetails("NONE");
        }
        logger.error("Excpetion Occurreed: MSG:[{}] ClassName: [{}]", ex.getMessage(), apiError.getClassName());
        if (request.getHeader("x-show-errors") != null) {
            if (request.getAttribute("request") != null) {
                Object requestBody = request.getAttribute("request");
                apiError.setRequest(requestBody);
            }
            return new ResponseEntity(apiError, HttpStatus.valueOf(500));
        } else {
            return new ResponseEntity(apiError, HttpStatus.valueOf(apiError.getStatusCode()));
        }
    }
}
