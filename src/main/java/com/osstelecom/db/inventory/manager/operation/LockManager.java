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
package com.osstelecom.db.inventory.manager.operation;

import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura
 * @created 06.09.2022
 */
@Service
public class LockManager {

    private final ReentrantLock lockManager = new ReentrantLock();

    public void lock() {
        lockManager.lock();
    }

    public boolean isLocked() {
        return lockManager.isLocked();
    }

    public void unlock() {
        if (lockManager.isLocked()) {
            lockManager.unlock();
        }
    }
}
