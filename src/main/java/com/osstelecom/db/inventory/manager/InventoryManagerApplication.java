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
package com.osstelecom.db.inventory.manager;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.annotation.PreDestroy;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe InventoryManagerApplication
 *
 * <p>
 * Descrição: Classe principal da aplicação Inventory Manager. Esta classe é
 * responsável por inicializar e configurar a aplicação.</p>
 *
 * <p>
 * Configurações:
 * <ul>
 * <li>Exclui as configurações de autoconfiguração do MongoDB
 * (MongoAutoConfiguration e MongoDataAutoConfiguration) para evitar conflitos
 * com o banco de dados da aplicação.</li>
 * <li>Realiza a varredura de componentes no pacote
 * "com.osstelecom.db.inventory".</li>
 * <li>Ativa o agendamento (scheduling) da aplicação.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Fluxo:
 * <ol>
 * <li>O método main é responsável por inicializar a aplicação Inventory
 * Manager.</li>
 * <li>O método onStartup é um event listener que é acionado quando a aplicação
 * está pronta para ser executada.</li>
 * <li>O método onShutDown é um event listener que é acionado quando a aplicação
 * está sendo encerrada.</li>
 * <li>O método disableSslVerification é responsável por desabilitar a validação
 * de certificados autoassinados, garantindo que a aplicação aceite conexões SSL
 * não confiáveis.</li>
 * </ol>
 * </p>
 *
 * <p>
 * Exceptions:
 * <ul>
 * <li>Não há exceções específicas lançadas por esta classe.</li>
 * </ul>
 * </p>
 *
 * @version 1.0
 * @since 14-12-2021
 * @see SpringBootApplication
 * @see ComponentScan
 * @see EnableScheduling
 * @author Lucas Nishimura
 * @created 14.12.2021
 */
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@ComponentScan({"com.osstelecom.db.inventory"})
@EnableScheduling
public class InventoryManagerApplication {

    private Logger logger = LoggerFactory.getLogger(InventoryManagerApplication.class);

    public static void main(String[] args) {
        disableSslVerification();
        SpringApplication.run(InventoryManagerApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    private void onStartup() {
        logger.debug("Hello Master of Universe, Nice to see you :) Inventory Manager Starting...");
    }

    @PreDestroy
    private void onShutDown() {
        logger.debug("Bye Master of Universe,Inventory Manager Shutting Down...");
    }

    /**
     * Garante que não vamos validar certificados autoassinados.
     */
    private static void disableSslVerification() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }
}
