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
package com.osstelecom.db.inventory.topology.object;

/**
 *
 * @author Nishisan
 */
public interface DefaultNetworkObject {

    public Integer getId();

    public void setId(Integer id);

    public void setPayLoad(Object payLoad);

    public Object getPayLoad();

    public void setActive(Boolean active);

    public Boolean getActive();

    public void setConnectionCount(Integer count);

    public Integer getConnectionCount();

    public Double getWeight();

    public void setWeight(Double weight);

    public Double calculateWeight();

    public String getUuid();

    public Integer getHeigth();

    public Integer getWidth();

    public void setHeigth(Integer heigth);

    public void setWidth(Integer width);

    /**
     * Checks whatever this node was already visited by the current interaction
     *
     * @param uid
     * @return
     */
    public Boolean isVisited(String uid);

    public void setVisited(String uid);

    public void setUnvisited(String uid);

    public void addAttribute(String key, Object value);

    public Object getAttribute(String key);

}
