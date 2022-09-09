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
package com.osstelecom.db.inventory.manager.resources;

import com.arangodb.entity.DocumentField;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 */
public class ResourceConnection extends BasicResource {

    /**
     * @return the fromResource
     */
    public BasicResource getFromResource() {
        return fromResource;
    }

    /**
     * @param fromResource the fromResource to set
     */
    public void setFromResource(BasicResource fromResource) {
        this.fromResource = fromResource;
    }

    /**
     * @return the toResource
     */
    public BasicResource getToResource() {
        return toResource;
    }

    /**
     * @param toResource the toResource to set
     */
    public void setToResource(BasicResource toResource) {
        this.toResource = toResource;
    }

    /**
     * Renamed to avoid confusion
     */
    private BasicResource fromResource;
    private BasicResource toResource;
    private Boolean propagateCapacity;
    private Boolean propagateConsuption;
    private Boolean propagateOperStatus;
    private Boolean bidirectionalConsuption;
    private Boolean bidirectionCapacity;
    @DocumentField(DocumentField.Type.FROM)
    private String _fromKey;
    @DocumentField(DocumentField.Type.TO)
    private String _toKey;

    private List<String> relatedNodes = new ArrayList<>();

    private List<String> circuits = new ArrayList<>();   

    public ResourceConnection(String attributeSchema, Domain domain) {
        super(attributeSchema, domain);

    }

    public ResourceConnection(Domain domain) {
        super(domain);
    }

    public ResourceConnection() {
    }

    public void setFrom(BasicResource resource) {
        this.fromResource = resource;
        this.setFromKey(this.getDomain().getNodes() + "/" + resource.getKey());
        if (!this.relatedNodes.contains(this.getFromKey())) {
            this.relatedNodes.add(this.getFromKey());
        }
    }

    public void setTo(BasicResource resource) {
        this.toResource = resource;
        this.setToKey(this.getDomain().getNodes() + "/" + resource.getKey());
        if (!this.relatedNodes.contains(this.getToKey())) {
            this.relatedNodes.add(this.getToKey());
        }
    }

    /**
     * @return the propagateCapacity
     */
    public Boolean getPropagateCapacity() {
        return propagateCapacity;
    }

    /**
     * @param propagateCapacity the propagateCapacity toResource set
     */
    public void setPropagateCapacity(Boolean propagateCapacity) {
        this.propagateCapacity = propagateCapacity;
    }

    /**
     * @return the propagateConsuption
     */
    public Boolean getPropagateConsuption() {
        return propagateConsuption;
    }

    /**
     * @param propagateConsuption the propagateConsuption toResource set
     */
    public void setPropagateConsuption(Boolean propagateConsuption) {
        this.propagateConsuption = propagateConsuption;
    }

    /**
     * @return the bidirectionalConsuptions
     */
    public Boolean getBidirectionalConsuption() {
        return bidirectionalConsuption;
    }

    /**
     * @param bidirectionalConsuptions the bidirectionalConsuptions toResource
     * set
     */
    public void setBidirectionalConsuptions(Boolean bidirectionalConsuption) {
        this.bidirectionalConsuption = bidirectionalConsuption;
    }

    /**
     * @return the bidirectionCapacity
     */
    public Boolean getBidirectionCapacity() {
        return bidirectionCapacity;
    }

    /**
     * @param bidirectionCapacity the bidirectionCapacity toResource set
     */
    public void setBidirectionCapacity(Boolean bidirectionCapacity) {
        this.bidirectionCapacity = bidirectionCapacity;
    }

    /**
     * @return the propagateOperStatus
     */
    public Boolean getPropagateOperStatus() {
        return propagateOperStatus;
    }

    /**
     * @param propagateOperStatus the propagateOperStatus toResource set
     */
    public void setPropagateOperStatus(Boolean propagateOperStatus) {
        this.propagateOperStatus = propagateOperStatus;
    }

    /**
     * @return the _from
     */
    public BasicResource getFrom() {
        return fromResource;
    }

    /**
     * @return the _to
     */
    public BasicResource getTo() {
        return toResource;
    }

    /**
     * @return the _fromKey
     */
    public String getFromKey() {
        return _fromKey;
    }

    /**
     * @param _fromKey the _fromKey toResource set
     */
    public void setFromKey(String _fromKey) {
        this._fromKey = _fromKey;
    }

    /**
     * @return the _toKey
     */
    public String getToKey() {
        return _toKey;
    }

    /**
     * @param _toKey the _toKey toResource set
     */
    public void setToKey(String _toKey) {
        this._toKey = _toKey;
    }

    /**
     * @return the circuits
     */
    public List<String> getCircuits() {
        return circuits;
    }

    /**
     * @param circuits the circuits toResource set
     */
    public void setCircuits(List<String> circuits) {
        this.circuits = circuits;
    }

    /**
     * @return the relatedNodes
     */
    public List<String> getRelatedNodes() {
        return relatedNodes;
    }

}
