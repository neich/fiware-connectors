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

import es.tid.fiware.fiwareconnectors.cygnus.sinks.ContextAttributeStats;
import java.util.ArrayList;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import static org.junit.Assert.*; // this is required by "fail" like assertions
import static org.mockito.Mockito.*; // this is required by "when" like functions

/**
 *
 * @author frb
 */
@RunWith(MockitoJUnitRunner.class)
public class OrionBackendTest {
    
    // instance to be tested
    private OrionBackend backend;
    
    // mocks
    // the DefaultHttpClient class cannot be mocked:
    // http://stackoverflow.com/questions/4547852/why-does-my-mockito-mock-object-use-real-the-implementation
    @Mock
    private HttpClient mockHttpClientUpdateContext;
    
    // constants
    private final String orionHost = "localhost";
    private final String orionPort = "1026";
    private final String entityId = "orion_backend_test_entity_id";
    private final String entityType = "orion_backend_test_entity_type";
    private static final ArrayList<ContextAttributeStats> ALLATTRSTATS = new ArrayList<ContextAttributeStats>();
    
    static {
        ALLATTRSTATS.add(new ContextAttributeStats("orion_backend_attr_name_1", "orion_backend_attr_type_1"));
        ALLATTRSTATS.add(new ContextAttributeStats("orion_backend_attr_name_2", "orion_backend_attr_type_2"));
        ALLATTRSTATS.add(new ContextAttributeStats("orion_backend_attr_name_3", "orion_backend_attr_type_3"));
    } // static

    /**
     * Sets up tests by creating a unique instance of the tested class, and by defining the behaviour of the mocked
     * classes.
     *  
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        // set up the instance of the tested class
        backend = new OrionBackend(orionHost, orionPort);
        
        // set up other instances
        BasicHttpResponse resp200 = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 200, "OK");

        // set up the behaviour of the mocked classes
        when(mockHttpClientUpdateContext.execute(Mockito.any(HttpUriRequest.class))).thenReturn(resp200);
    } // setUp
    
    /**
     * Test of createDir method, of class HDFSBackendImpl.
     */
    @Test
    public void testUpdateContext() {
        System.out.println("Testing OrionBackend.updateContext");
        
        try {
            backend.setHttpClient(mockHttpClientUpdateContext);
            backend.updateContext(entityId, entityType, ALLATTRSTATS);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            assertTrue(true);
        } // try catch finally
    } // testUpdateContext
    
} // OrionBackendTest
