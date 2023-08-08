/*
 * Copyright (C) 2021 Lucas Nishimura <lucas.nishimura@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.osstelecom.db.inventory.manager.resources.exception;

import com.osstelecom.db.inventory.manager.exception.BasicException;
import com.osstelecom.db.inventory.manager.exception.IBasicException;

/**
 *
 * @author Lucas Nishimura
 */
public class NoResourcesAvailableException extends BasicException {

    public NoResourcesAvailableException() {
    }

    public NoResourcesAvailableException(String string) {
        super(string);
    }

    public NoResourcesAvailableException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

    public NoResourcesAvailableException(Throwable thrwbl) {
        super(thrwbl);
    }

    @Override
    public NoResourcesAvailableException addDetails(String key, Object value) {
        this.addDetailMap(key, value);
        return this;
    }

}
