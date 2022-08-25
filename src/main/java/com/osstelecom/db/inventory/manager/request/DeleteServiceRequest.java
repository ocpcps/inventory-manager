package com.osstelecom.db.inventory.manager.request;

import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;

public class DeleteServiceRequest extends BasicRequest<ServiceResource> {
    
    public DeleteServiceRequest(String id, String domainName) {
        DomainDTO domain = new DomainDTO();
        domain.setDomainName(domainName);
        ServiceResource serviceResource = new ServiceResource(domain);
        serviceResource.setId(id);
        this.setPayLoad(serviceResource);
    }
    
}
