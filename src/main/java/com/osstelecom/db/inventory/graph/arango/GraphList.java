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
package com.osstelecom.db.inventory.graph.arango;

import com.arangodb.ArangoCursor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * This is a list backed by a ArangoDB Cursor. This is usefull to stream
 * operations
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 29.07.2022
 */
public class GraphList<T> implements AutoCloseable {

    private final ArangoCursor<T> cursor;
    private boolean closedCursor = false;

    public GraphList(ArangoCursor<T> cursor) {
        this.cursor = cursor;

    }

    /**
     * Dynamic Consumer the cursor...
     *
     * @param action
     */
    public void forEachParallel(Consumer<? super T> action) throws IOException, IllegalStateException {
        Objects.requireNonNull(action);
        if (!this.closedCursor) {

            this.cursor.stream().parallel().forEachOrdered(action::accept);

            this.cursor.close();
            this.closedCursor = true;
        } else {
            throw new IllegalStateException("Cursor Closed");
        }
    }

    /**
     * Dynamic Consumer the cursor...
     *
     * @param action
     */
    public void forEach(Consumer<? super T> action) throws IOException, IllegalStateException {
        Objects.requireNonNull(action);
        if (!this.closedCursor) {
            try {
                this.cursor.forEachRemaining(action::accept);
            } finally {
                //
                // make sure the cursor is closed
                //
                this.cursor.close();
                this.closedCursor = true;
            }
        } else {
            throw new IllegalStateException("Cursor Closed");
        }
    }

    public int size() {
        if (cursor.getCount() == null) {
            return -1;
        }
        return cursor.getCount();
    }

    /**
     * @return the cursor
     */
    public ArangoCursor<T> getCursor() {
        return cursor;
    }

    public Stream<T> stream() {
        return cursor.stream();
    }

    @Override
    public void close() throws IOException {
        if (!this.closedCursor) {
            this.cursor.close();
        }
    }

    public List<T> toList() {
        List<T> list = new ArrayList<>();
        if (!this.closedCursor) {
            try {
                this.forEach(list::add);
            } catch (IOException | IllegalStateException ex) {
                Logger.getLogger(GraphList.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return list;
    }
}
