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
package com.osstelecom.db.inventory.manager.session;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.operation.IconManager;
import com.osstelecom.db.inventory.manager.request.CreateIconRequest;
import com.osstelecom.db.inventory.manager.request.DeleteIconRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.GetIconRequest;
import com.osstelecom.db.inventory.manager.request.PatchIconRequest;
import com.osstelecom.db.inventory.manager.resources.model.IconModel;
import com.osstelecom.db.inventory.manager.response.CreateIconResponse;
import com.osstelecom.db.inventory.manager.response.DeleteIconResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.GetIconResponse;
import com.osstelecom.db.inventory.manager.response.PatchIconResponse;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 18.08.2022
 */
@Service
public class IconSession {

    @Autowired
    private IconManager iconManager;

    public GetIconResponse getIconById(GetIconRequest request)
            throws ResourceNotFoundException, InvalidRequestException {
        if (request.getPayLoad().getSchemaName() == null) {
            throw new InvalidRequestException("Payload Missing");
        }
        return new GetIconResponse(iconManager.getIconById(request.getPayLoad()));
    }

    public DeleteIconResponse deleteIcon(DeleteIconRequest request)
            throws InvalidRequestException, ResourceNotFoundException, IOException {
        if (request.getPayLoad().getSchemaName() == null) {
            throw new InvalidRequestException("Schema name Field Missing");
        }
        IconModel payload = request.getPayLoad();
        IconModel old = iconManager.getIconById(payload);

        if (old == null) {
            throw new ResourceNotFoundException("Icon not found");
        }

        return new DeleteIconResponse(iconManager.deleteIcon(request.getPayLoad()));
    }

    public CreateIconResponse createIcon(CreateIconRequest request)
            throws InvalidRequestException, ResourceNotFoundException, GenericException {
        
        if (request == null || request.getPayLoad() == null) {
            throw new InvalidRequestException("Request is null please send a valid request");
        }

        if (request.getPayLoad().getSchemaName() == null) {
            throw new InvalidRequestException("Schema name Field Missing");
        }

        IconModel payload = request.getPayLoad();

        IconModel old = iconManager.getIconById(payload);

        if (old.getContent() != null) {
            throw new ResourceNotFoundException("Icon already exists");
        }
        if (payload.getContent() == null) {
            throw new InvalidRequestException("Content not found");
        }

        if (payload.getMimeType() == null) {
            throw new InvalidRequestException("Mime Type not found");
        }
        if (payload.getContent().length() == 0 || payload.getContent().length() > 300000) {
            throw new InvalidRequestException("Content size is invalid");
        }
        CreateIconResponse createIconResponse = new CreateIconResponse(iconManager.createIcon(payload));
        return createIconResponse;
    }

    public PatchIconResponse updateIcon(PatchIconRequest request)
            throws InvalidRequestException, ResourceNotFoundException, GenericException {

        if (request == null || request.getPayLoad() == null) {
            throw new InvalidRequestException("Request is null please send a valid request");
        }

        if (request.getPayLoad().getSchemaName() == null) {
            throw new InvalidRequestException("Schema name Field Missing");
        }

        IconModel payload = request.getPayLoad();
        if (payload.getContent() == null) {
            throw new InvalidRequestException("Content not found");
        }

        if (payload.getMimeType() == null) {
            throw new InvalidRequestException("Mime Type not found");
        }
        if (payload.getContent().length() == 0 || payload.getContent().length() > 300000) {
            throw new InvalidRequestException("Content size is invalid");
        }
        IconModel old = iconManager.getIconById(payload);
        if (old == null) {
            throw new ResourceNotFoundException("Icon not found");
        }

        return new PatchIconResponse(iconManager.updateIcon(payload));

    }

    public FilterResponse findIconByFilter(FilterRequest filter) throws InvalidRequestException, IOException {

        if (filter.getPayLoad() != null && filter.getPayLoad().getLimit() != null
                && filter.getPayLoad().getLimit() > 1000) {
            throw new InvalidRequestException(
                    "Result Set Limit cannot be over 1000, please descrease limit value to a range between 0 and 1000");

        }

        FilterResponse response = new FilterResponse(filter.getPayLoad());
        if (filter.getPayLoad().getObjects().contains("icon") || filter.getPayLoad().getObjects().contains("icons")) {
            List<IconModel> listIcon = iconManager.findIconByFilter(filter.getPayLoad());
            response.getPayLoad().setIcons(listIcon);
            response.getPayLoad().setIconCount(Long.valueOf(listIcon.size()));
            response.setSize(Long.valueOf(listIcon.size()));
        } else {
            throw new InvalidRequestException("Filter object does not have service");
        }

        return response;
    }

}
