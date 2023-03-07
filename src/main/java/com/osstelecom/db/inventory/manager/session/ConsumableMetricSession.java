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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.operation.ConsumableMetricManager;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.request.CreateConsumableMetricRequest;
import com.osstelecom.db.inventory.manager.request.DeleteConsumableMetricRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.GetConsumableMetricRequest;
import com.osstelecom.db.inventory.manager.request.PatchConsumableMetricRequest;
import com.osstelecom.db.inventory.manager.resources.ConsumableMetric;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.response.CreateConsumableMetricResponse;
import com.osstelecom.db.inventory.manager.response.DeleteConsumableMetricResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.GetConsumableMetricResponse;
import com.osstelecom.db.inventory.manager.response.PatchConsumableMetricResponse;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 18.08.2022
 */
@Service
public class ConsumableMetricSession {

    @Autowired
    private ConsumableMetricManager consumableMetricManager;

    @Autowired
    private DomainManager domainManager;

    public GetConsumableMetricResponse getConsumableMetricByName(GetConsumableMetricRequest request)
            throws ResourceNotFoundException, DomainNotFoundException, ArangoDaoException, InvalidRequestException {
        if (request.getPayLoad().getMetricName() == null) {
            throw new InvalidRequestException("Metric Name Field Missing");
        }

        if (request.getRequestDomain() == null) {
            throw new DomainNotFoundException("Domain With Name:[" + request.getRequestDomain() + "] not found");
        }
        request.getPayLoad().setDomain(domainManager.getDomain(request.getRequestDomain()));

        return new GetConsumableMetricResponse(consumableMetricManager.getConsumableMetric(request.getPayLoad()));
    }

    public DeleteConsumableMetricResponse deleteConsumableMetric(DeleteConsumableMetricRequest request)
            throws DomainNotFoundException, ArangoDaoException, InvalidRequestException {
        if (request == null || request.getPayLoad() == null) {
            throw new InvalidRequestException("Request is null please send a valid request");
        }

        if (request.getPayLoad().getMetricName() == null) {
            throw new InvalidRequestException("Metric Name Field Missing");
        }

        if (request.getRequestDomain() == null) {
            throw new DomainNotFoundException("Domain With Name:[" + request.getRequestDomain() + "] not found");
        }
        request.getPayLoad().setDomain(domainManager.getDomain(request.getRequestDomain()));

        ConsumableMetric payload = request.getPayLoad();
        if (!StringUtils.hasText(payload.getKey())) {
            payload.setKey(payload.getMetricName());
        }

        return new DeleteConsumableMetricResponse(consumableMetricManager.deleteConsumableMetric(payload));
    }

    public CreateConsumableMetricResponse createConsumableMetric(CreateConsumableMetricRequest request)
            throws InvalidRequestException, DomainNotFoundException, ArangoDaoException {

        if (request == null || request.getPayLoad() == null) {
            throw new InvalidRequestException("Request is null please send a valid request");
        }

        if (request.getPayLoad().getMetricName() == null) {
            throw new InvalidRequestException("Metric Name Field Missing");
        }

        if (request.getRequestDomain() == null) {
            throw new DomainNotFoundException("Domain With Name:[" + request.getRequestDomain() + "] not found");
        }

        request.getPayLoad().setDomain(domainManager.getDomain(request.getRequestDomain()));

        ConsumableMetric payload = request.getPayLoad();
        if (!StringUtils.hasText(payload.getKey())) {
            payload.setKey(payload.getMetricName());
        }

        return new CreateConsumableMetricResponse(consumableMetricManager.createConsumableMetric(payload));
    }

    public PatchConsumableMetricResponse updateConsumableMetric(PatchConsumableMetricRequest request)
            throws InvalidRequestException, DomainNotFoundException, ArangoDaoException {
        if (request.getPayLoad().getMetricName() == null) {
            throw new InvalidRequestException("Metric Name Field Missing");
        }

        if (request.getRequestDomain() == null) {
            throw new DomainNotFoundException("Domain With Name:[" + request.getRequestDomain() + "] not found");
        }
        request.getPayLoad().setDomain(domainManager.getDomain(request.getRequestDomain()));

        ConsumableMetric payload = request.getPayLoad();
        if (!StringUtils.hasText(payload.getKey())) {
            payload.setKey(payload.getMetricName());
        }

        return new PatchConsumableMetricResponse(consumableMetricManager.updateConsumableMetric(payload));
    }

    public FilterResponse findConsumableMetricByFilter(FilterRequest filter)
            throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {
        //
        // Validação para evitar abusos de uso da API
        //
        if (filter.getPayLoad() != null) {
            if (filter.getPayLoad().getLimit() != null) {
                if (filter.getPayLoad().getLimit() > 1000) {
                    throw new InvalidRequestException(
                            "Result Set Limit cannot be over 1000, please descrease limit value to a range between 0 and 1000");
                }
            }
        }
        FilterResponse response = new FilterResponse(filter.getPayLoad());
        if (filter.getPayLoad().getObjects().contains("metric")
                || filter.getPayLoad().getObjects().contains("metrics")) {
            Domain domain = domainManager.getDomain(filter.getRequestDomain());
            GraphList<ConsumableMetric> graphList = consumableMetricManager.findServiceByFilter(filter.getPayLoad(),
                    domain);
            response.getPayLoad().setMetrics(graphList.toList());
            response.getPayLoad().setMetricCount(graphList.size());
            response.setSize(graphList.size());
        } else {
            throw new InvalidRequestException("Filter object does not have service");
        }

        return response;
    }

}
