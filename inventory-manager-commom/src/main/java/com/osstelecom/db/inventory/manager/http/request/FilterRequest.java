/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.osstelecom.db.inventory.manager.http.request;

import com.osstelecom.db.inventory.manager.objects.Filter;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 17.08.2022
 */
public class FilterRequest extends BasicRequest<Filter> {
    
    public FilterRequest(Filter payLoad) {
        super(payLoad);
        this.setRequestDomain(payLoad.getDomain());
    }
    
    public FilterRequest(String requestDomain, Filter payLoad) {
        super(requestDomain, payLoad);
    }
    
}
