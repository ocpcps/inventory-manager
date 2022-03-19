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
package com.osstelecom.db.inventory.manager.configuration;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 15.12.2021
 */
public class ArangoDBConfiguration {

    private String databaseName = "inventory";

    private String nodeSufix = "_nodes";
    private String serviceSufix = "_services";
    private String nodeConnectionSufix = "_connections";
    private String circuitsSufix = "_circuits";
    private String serviceConnectionSufix = "_srv_connections";

    private String connectionLayerSufix = "_connections_layer";
    private String serviceLayerSufix = "_services_layer";

    private String host = "10.113.144.209";
    private int port = 8529;
    private String user = "root";
    private String password = "vivo@123";

    private String domainsCollection = "domains";

    /**
     * @return the databaseName
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * @param databaseName the databaseName to set
     */
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the nodeSufix
     */
    public String getNodeSufix() {
        return nodeSufix;
    }

    /**
     * @param nodeSufix the nodeSufix to set
     */
    public void setNodeSufix(String nodeSufix) {
        this.nodeSufix = nodeSufix;
    }

    /**
     * @return the connectionLayerSufix
     */
    public String getConnectionLayerSufix() {
        return connectionLayerSufix;
    }

    /**
     * @param connectionLayerSufix the connectionLayerSufix to set
     */
    public void setConnectionLayerSufix(String connectionLayerSufix) {
        this.connectionLayerSufix = connectionLayerSufix;
    }

    /**
     * @return the serviceLayerSufix
     */
    public String getServiceLayerSufix() {
        return serviceLayerSufix;
    }

    /**
     * @param serviceLayerSufix the serviceLayerSufix to set
     */
    public void setServiceLayerSufix(String serviceLayerSufix) {
        this.serviceLayerSufix = serviceLayerSufix;
    }

    /**
     * @return the serviceSufix
     */
    public String getServiceSufix() {
        return serviceSufix;
    }

    /**
     * @param serviceSufix the serviceSufix to set
     */
    public void setServiceSufix(String serviceSufix) {
        this.serviceSufix = serviceSufix;
    }

    /**
     * @return the nodeConnectionSufix
     */
    public String getNodeConnectionSufix() {
        return nodeConnectionSufix;
    }

    /**
     * @param nodeConnectionSufix the nodeConnectionSufix to set
     */
    public void setNodeConnectionSufix(String nodeConnectionSufix) {
        this.nodeConnectionSufix = nodeConnectionSufix;
    }

    /**
     * @return the serviceConnectionSufix
     */
    public String getServiceConnectionSufix() {
        return serviceConnectionSufix;
    }

    /**
     * @param serviceConnectionSufix the serviceConnectionSufix to set
     */
    public void setServiceConnectionSufix(String serviceConnectionSufix) {
        this.serviceConnectionSufix = serviceConnectionSufix;
    }

    /**
     * @return the domainsCollection
     */
    public String getDomainsCollection() {
        return domainsCollection;
    }

    /**
     * @param domainsCollection the domainsCollection to set
     */
    public void setDomainsCollection(String domainsCollection) {
        this.domainsCollection = domainsCollection;
    }

    /**
     * @return the circuitsSufix
     */
    public String getCircuitsSufix() {
        return circuitsSufix;
    }

}
