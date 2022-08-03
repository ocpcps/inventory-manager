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
import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import java.util.ArrayList;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 */
public class ResourceConnection extends BasicResource {

    /**
     * Renamed to avoid confusion
     */
    private BasicResource fromResource;
    private BasicResource toResource;
    private Boolean propagateCapacity;
    private Boolean propagateConsuption;
    private Boolean propagateOperStatus;
    private Boolean bidirectionalConsuptions;
    private Boolean bidirectionCapacity;
    @DocumentField(DocumentField.Type.FROM)
    private String _fromUid;
    @DocumentField(DocumentField.Type.TO)
    private String _toUid;

    private ArrayList<String> relatedNodes = new ArrayList<>();

    private ArrayList<String> circuits = new ArrayList<>();

    public ResourceConnection(String attributeSchema, DomainDTO domain) {
        super(attributeSchema, domain);

    }

    public ResourceConnection(DomainDTO domain) {
        super(domain);
    }

    public ResourceConnection() {
    }

    public void setFrom(BasicResource resource) {
        this.fromResource = resource;
        this.setFromUid(this.getDomain().getNodes() + "/" + resource.getUid());
        if (!this.relatedNodes.contains(this.getFromUid())){
            this.relatedNodes.add(this.getFromUid());
        }
    }

    public void setTo(BasicResource resource) {
        this.toResource = resource;
        this.setToUid(this.getDomain().getNodes() + "/" + resource.getUid());
        if (!this.relatedNodes.contains(this.getToUid())){
            this.relatedNodes.add(this.getToUid());
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
    public Boolean getBidirectionalConsuptions() {
        return bidirectionalConsuptions;
    }

    /**
     * @param bidirectionalConsuptions the bidirectionalConsuptions toResource set
     */
    public void setBidirectionalConsuptions(Boolean bidirectionalConsuptions) {
        this.bidirectionalConsuptions = bidirectionalConsuptions;
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
     * @return the _fromUid
     */
    public String getFromUid() {
        return _fromUid;
    }

    /**
     * @param _fromUid the _fromUid toResource set
     */
    public void setFromUid(String _fromUid) {
        this._fromUid = _fromUid;
    }

    /**
     * @return the _toUid
     */
    public String getToUid() {
        return _toUid;
    }

    /**
     * @param _toUid the _toUid toResource set
     */
    public void setToUid(String _toUid) {
        this._toUid = _toUid;
    }

    /**
     * @return the circuits
     */
    public ArrayList<String> getCircuits() {
        return circuits;
    }

    /**
     * @param circuits the circuits toResource set
     */
    public void setCircuits(ArrayList<String> circuits) {
        this.circuits = circuits;
    }

    /**
     * @return the relatedNodes
     */
    public ArrayList<String> getRelatedNodes() {
        return relatedNodes;
    }

}
