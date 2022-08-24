/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

    public Boolean isVisited();

    public void setVisited();

    public void setUnvisited();

    public void addAttribute(String key, Object value);

    public Object getAttribute(String key);

}
