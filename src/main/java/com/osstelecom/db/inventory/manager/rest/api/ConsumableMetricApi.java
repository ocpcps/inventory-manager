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
package com.osstelecom.db.inventory.manager.rest.api;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.request.CreateConsumableMetricRequest;
import com.osstelecom.db.inventory.manager.request.DeleteConsumableMetricRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.GetConsumableMetricRequest;
import com.osstelecom.db.inventory.manager.request.PatchConsumableMetricRequest;
import com.osstelecom.db.inventory.manager.resources.ConsumableMetric;
import com.osstelecom.db.inventory.manager.response.CreateConsumableMetricResponse;
import com.osstelecom.db.inventory.manager.response.DeleteConsumableMetricResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.GetConsumableMetricResponse;
import com.osstelecom.db.inventory.manager.response.PatchConsumableMetricResponse;
import com.osstelecom.db.inventory.manager.security.model.AuthenticatedCall;
import com.osstelecom.db.inventory.manager.session.ConsumableMetricSession;

@RestController
@RequestMapping("inventory/v1")
public class ConsumableMetricApi extends BaseApi {

    @Autowired
    private ConsumableMetricSession consumableMetricSession;

    @AuthenticatedCall(role = {"user"})
    @PutMapping(path = "/{domain}/consumableMetric", produces = "application/json", consumes = "application/json")
    public CreateConsumableMetricResponse createConsumableMetric(@RequestBody CreateConsumableMetricRequest request,
            @PathVariable("domain") String domain, HttpServletRequest httpRequest) throws ArangoDaoException, DomainNotFoundException, InvalidRequestException {
        request.setRequestDomain(domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return this.consumableMetricSession.createConsumableMetric(request);
    }

    @AuthenticatedCall(role = {"user"})
    @PatchMapping(path = "/{domain}/consumableMetric", produces = "application/json", consumes = "application/json")
    public PatchConsumableMetricResponse patchConsumableMetric(@RequestBody PatchConsumableMetricRequest request,
            @PathVariable("domain") String domain, HttpServletRequest httpRequest) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        request.setRequestDomain(domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return this.consumableMetricSession.updateConsumableMetric(request);
    }                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       

    @AuthenticatedCall(role = {"user"})
    @PatchMapping(path = "/{domain}/consumableMetric/{metricName}", produces = "application/json", consumes = "application/json")
    public PatchConsumableMetricResponse patchConsumableMetric(@RequestBody PatchConsumableMetricRequest request,
            @PathVariable("domain") String domain, @PathVariable("metricName") String metricName, HttpServletRequest httpRequest) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        request.setRequestDomain(domain);
        request.setPayLoad(new ConsumableMetric());
        request.getPayLoad().setMetricName(metricName);
        request.setRequestDomain(domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return this.consumableMetricSession.updateConsumableMetric(request);
    }

    
    @AuthenticatedCall(role = {"user"})
    @PostMapping(path = "/{domain}/consumableMetric/filter", produces = "application/json", consumes = "application/json")
    public FilterResponse findConsumableMetricsByFilter(@RequestBody FilterRequest filter,
            @PathVariable("domain") String domain, HttpServletRequest httpRequest) throws ArangoDaoException, ResourceNotFoundException, DomainNotFoundException, InvalidRequestException {
        this.setUserDetails(filter);
        filter.setRequestDomain(domain);
        httpRequest.setAttribute("request", filter);
        return this.consumableMetricSession.findConsumableMetricByFilter(filter);
    }

    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{domain}/consumableMetric/{metricName}", produces = "application/json")
    public GetConsumableMetricResponse getConsumableMetricByName(@PathVariable("domain") String domain,
            @PathVariable("metricName") String metricName, HttpServletRequest httpRequest) throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        GetConsumableMetricRequest request = new GetConsumableMetricRequest();
        request.setPayLoad(new ConsumableMetric());
        request.getPayLoad().setMetricName(metricName);
        request.setRequestDomain(domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return this.consumableMetricSession.getConsumableMetricByName(request);
    }

    @AuthenticatedCall(role = {"user"})
    @DeleteMapping(path = "/{domain}/consumableMetric/{metricName}", produces = "application/json")
    public DeleteConsumableMetricResponse deleteConsumableMetricByName(@PathVariable("domain") String domain,
            @PathVariable("metricName") String metricName, HttpServletRequest httpRequest) throws DomainNotFoundException, ArangoDaoException, InvalidRequestException {
        DeleteConsumableMetricRequest request = new DeleteConsumableMetricRequest();
        request.setPayLoad(new ConsumableMetric());
        request.getPayLoad().setMetricName(metricName);
        request.setRequestDomain(domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return this.consumableMetricSession.deleteConsumableMetric(request);

    }
}
