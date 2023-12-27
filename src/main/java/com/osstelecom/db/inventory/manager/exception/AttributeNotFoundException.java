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
package com.osstelecom.db.inventory.manager.exception;

import com.osstelecom.db.inventory.manager.request.BasicRequest;
import com.osstelecom.db.inventory.manager.request.IRequest;

/**
 *
 * @author Lucas Nishimura
 * @created 16.12.2021
 */
public class AttributeNotFoundException extends BasicException {

    public AttributeNotFoundException(Throwable thrwbl) {
        super(thrwbl);
    }

    public AttributeNotFoundException() {
    }

    public AttributeNotFoundException(String msg) {
        super(msg);
    }

    public AttributeNotFoundException(IRequest<? extends BasicRequest> request) {
        super(request);
    }

    public AttributeNotFoundException(IRequest<? extends BasicRequest> request, String message) {
        super(request, message);
    }

    public AttributeNotFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public AttributeNotFoundException(IRequest<? extends BasicRequest> request, String message, Throwable cause) {
        super(request, message, cause);
    }

    public AttributeNotFoundException(IRequest<? extends BasicRequest> request, Throwable cause) {
        super(request, cause);
    }

    public AttributeNotFoundException(IRequest<? extends BasicRequest> request, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(request, message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public AttributeNotFoundException addDetails(String key, Object value) {
        this.addDetailMap(key, value);
        return this;
    }
}
