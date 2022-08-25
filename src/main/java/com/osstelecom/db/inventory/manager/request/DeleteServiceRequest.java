package com.osstelecom.db.inventory.manager.request;

import com.osstelecom.db.inventory.manager.resources.ServiceResource;

public class DeleteServiceRequest extends BasicRequest<ServiceResource> {
    
    public DeleteServiceRequest(String id) {
        ServiceResource serviceResource = new ServiceResource(id);
        this.setPayLoad(serviceResource);
    }
    
}
