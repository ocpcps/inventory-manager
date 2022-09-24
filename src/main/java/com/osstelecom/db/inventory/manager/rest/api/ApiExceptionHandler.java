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

    @ExceptionHandler(BasicException.class)
    protected ResponseEntity<Object> handleGenericException(
            BasicException ex) {
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
        return new ResponseEntity(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
