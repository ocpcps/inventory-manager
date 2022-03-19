/*
 * Copyright (C) 2022 Lucas Nishimura <lucas.nishimura@gmail.com>
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
package com.osstelecom.db.inventory.manager.exception;

import com.osstelecom.db.inventory.manager.request.IRequest;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 04.01.2022
 */
public class DomainNotFoundException extends BasicException {

    public DomainNotFoundException() {
    }

    public DomainNotFoundException(String msg) {
        super(msg);
    }

    public DomainNotFoundException(IRequest request) {
        super(request);
    }

    public DomainNotFoundException(IRequest request, String message) {
        super(request, message);
    }

    public DomainNotFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public DomainNotFoundException(IRequest request, String message, Throwable cause) {
        super(request, message, cause);
    }

    public DomainNotFoundException(IRequest request, Throwable cause) {
        super(request, cause);
    }

    public DomainNotFoundException(IRequest request, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(request, message, cause, enableSuppression, writableStackTrace);
    }

}
