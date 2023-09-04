/*
 * Copyright (C) 2023 Lucas Nishimura <lucas.nishimura@gmail.com>
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

import java.io.IOException;
import java.util.UUID;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 01.09.2023
 */
public class MdcFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            // Gerar um ID de requisição único
            String requestId = UUID.randomUUID().toString();
            // Adicionar ao MDC
            MDC.put("x-netcompass-requestId", requestId);
            try {
                // Adicionar o ID da requisição ao header do response
                HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                httpServletResponse.addHeader("x-netcompass-requestId", requestId);
            } catch (Exception ex) {
                //
                // Omite
                //
            }

            // Continue o processamento da requisição
            chain.doFilter(request, response);
        } finally {
            // Limpar o MDC após o processamento da requisição para evitar vazamento de memória
            MDC.clear();
        }
    }
}
