/**
 * Copyright 2014 Telefonica Investigación y Desarrollo, S.A.U
 *
 * This file is part of fiware-connectors (FI-WARE project).
 *
 * fiware-connectors is free software: you can redistribute it and/or modify it under the terms of the GNU Affero
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * fiware-connectors is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with fiware-connectors. If not, see
 * http://www.gnu.org/licenses/.
 *
 * For those usages not covered by the GNU Affero General Public License please contact with iot_support at tid dot es
 */

package es.tid.fiware.fiwareconnectors.cygnus.backends.orion;

import es.tid.fiware.fiwareconnectors.cygnus.errors.CygnusPersistenceError;
import es.tid.fiware.fiwareconnectors.cygnus.errors.CygnusRuntimeError;
import es.tid.fiware.fiwareconnectors.cygnus.http.HttpClientFactory;
import es.tid.fiware.fiwareconnectors.cygnus.sinks.OrionStatsSink.ContextAttributeStats;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.log4j.Logger;

/**
 *
 * @author frb
 */
public class OrionBackend {
    
    private Logger logger;
    private String orionHost;
    private String orionPort;
    private HttpClientFactory httpClientFactory;
    private HttpClient httpClient;
    
    /**
     * Constructor.
     * @param orionHost
     * @param orionPort
     */
    public OrionBackend(String orionHost, String orionPort) {
        this.orionHost = orionHost;
        this.orionPort = orionPort;
        
        // create a Http clients factory (no SSL) and an initial connection (no SSL)
        httpClientFactory = new HttpClientFactory(false, null, null);
        httpClient = httpClientFactory.getHttpClient(false, false);

        // logger
        logger = Logger.getLogger(OrionBackend.class);
    } // OrionBackend
    
    /**
     * Updates a context element.
     * @throws Exception
     */
    public void updateContext(String entityId, String entityType, ArrayList<ContextAttributeStats> allAttrStats)
        throws Exception {
        // create the relative URL
        String relativeURL = "/v1/updateContext";
        
        // create the http headers
        ArrayList<Header> headers = new ArrayList<Header>();
        
        // create the Json-based payload
        String jsonStr = ""
                + "{"
                + "   \"contextElements\": ["
                + "      {"
                + "         \"type\": \"" + entityType + "\","
                + "         \"isPattern\": \"false\","
                + "         \"id\": \"" + entityId + "\","
                + "         \"attributes\": [";
        
        for (int i = 0; i < allAttrStats.size(); i++) {
            ContextAttributeStats attrStats = allAttrStats.get(i);
            jsonStr += ""
                    + "         {"
                    + "             \"name\": \"" + attrStats.getName() + "\","
                    + "             \"type\": \"" + attrStats.getType() + "\""
                    + "             \"metadatas\": ["
                    + "             {"
                    + "                \"name\": \"max\","
                    + "                \"type\": \"double\","
                    + "                \"value\": \"" + attrStats.getMax() + "\""
                    + "             },"
                    + "             {"
                    + "                \"name\": \"min\","
                    + "                \"type\": \"double\","
                    + "                \"value\": \"" + attrStats.getMin() + "\""
                    + "             },"
                    + "             {"
                    + "                \"name\": \"average\","
                    + "                \"type\": \"double\","
                    + "                \"value\": \"" + attrStats.getAverage() + "\""
                    + "             },"
                    + "             {"
                    + "                \"name\": \"variation\","
                    + "                \"type\": \"double\","
                    + "                \"value\": \"" + attrStats.getVariation() + "\""
                    + "             }"
                    + "             ]"
                    + "         }";
            
            if (i != (allAttrStats.size() - 1)) {
                jsonStr += ",";
            }
        } // for
        
        jsonStr += ""
                + "         ]"
                + "      }"
                + "   ],"
                + "   \"updateAction\": \"UPDATE\""
                + "}";
        StringEntity entity = new StringEntity(jsonStr);
        
        // do the request
        HttpResponse response = doRequest("PUT", relativeURL, true, headers, entity);

        // check the status
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new CygnusPersistenceError("The context could not be updated. HttpFS response: "
                    + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
        } // if
    } // updateContext
    
    /**
     * Does a HDFS request given a HTTP client, a method and a relative URL (the final URL will be composed by using
     * this relative URL and the active HDFS endpoint).
     * @param method
     * @param relativeURL
     * @return
     * @throws Exception
     */
    private HttpResponse doRequest(String method, String url, boolean relative, ArrayList<Header> headers,
            StringEntity entity) throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_SERVICE_UNAVAILABLE,
                "Service unavailable");
        
        if (relative) {
            // create the HttpFS URL
            String effectiveURL = "http://" + orionHost + ":" + orionPort + url;
            response = doRequest(method, effectiveURL, headers, entity);
        } else {
            response = doRequest(method, url, headers, entity);
        } // if else
        
        return response;
    } // doRequest
        
    private HttpResponse doRequest(String method, String url, ArrayList<Header> headers, StringEntity entity)
        throws Exception {
        HttpResponse response = null;
        HttpRequestBase request = null;

        if (method.equals("PUT")) {
            HttpPut req = new HttpPut(url);

            if (entity != null) {
                req.setEntity(entity);
            } // if

            request = req;
        } else if (method.equals("POST")) {
            HttpPost req = new HttpPost(url);

            if (entity != null) {
                req.setEntity(entity);
            } // if

            request = req;
        } else if (method.equals("GET")) {
            request = new HttpGet(url);
        } else {
            throw new CygnusRuntimeError("HTTP method not supported: " + method);
        } // if else

        if (headers != null) {
            for (Header header : headers) {
                request.setHeader(header);
            } // for
        } // if

        logger.debug("HDFS request: " + request.toString());

        try {
            response = httpClient.execute(request);
        } catch (IOException e) {
            throw new CygnusPersistenceError(e.getMessage());
        } // try catch

        request.releaseConnection();
        logger.debug("HDFS response: " + response.getStatusLine().toString());
        return response;
    } // doRequest

} // OrionBackend
