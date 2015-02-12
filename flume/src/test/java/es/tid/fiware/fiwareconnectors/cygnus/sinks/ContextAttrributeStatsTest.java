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
package es.tid.fiware.fiwareconnectors.cygnus.sinks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import static org.junit.Assert.*; // this is required by "fail" like assertions

/**
 *
 * @author frb
 */
@RunWith(MockitoJUnitRunner.class)
public class ContextAttrributeStatsTest {
    
    // instance to be tested
    private ContextAttributeStats contextAttributeStats;
    
    // constants
    private final String attrName = "context_attribute_stats_test_attr_name";
    private final String attrType = "context_attribute_stats_test_attr_type";
    
    /**
     * Sets up tests by creating a unique instance of the tested class, and by defining the behaviour of the mocked
     * classes.
     *  
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        // set up the instance of the tested class
        contextAttributeStats = new ContextAttributeStats(attrName, attrType);
    } // setUp

    /**
     * Test of processContextResponses method, of class OrionCKANSink.
     */
    @Test
    public void testUpdateStats() throws Exception {
        contextAttributeStats.updateStats(10.0);
        contextAttributeStats.updateStats(11.0);
        contextAttributeStats.updateStats(9.5);
        contextAttributeStats.updateStats(12.0);
        contextAttributeStats.updateStats(11.0);
        contextAttributeStats.updateStats(10.5);
        contextAttributeStats.updateStats(10.0);
        contextAttributeStats.updateStats(6.0);
        assertEquals("12.0", new Double(contextAttributeStats.getMax()).toString());
        assertEquals("6.0", new Double(contextAttributeStats.getMin()).toString());
        assertEquals("10.0", new Double(contextAttributeStats.getAverage()).toString());
        assertEquals("2.8125", new Double(contextAttributeStats.getVariation()).toString());
    } // testUpdateStats
    
} // ContextAttrributeStatsTest
