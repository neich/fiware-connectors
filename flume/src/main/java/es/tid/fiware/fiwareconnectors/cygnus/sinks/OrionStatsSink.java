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

import es.tid.fiware.fiwareconnectors.cygnus.backends.orion.OrionBackend;
import es.tid.fiware.fiwareconnectors.cygnus.containers.NotifyContextRequest;
import es.tid.fiware.fiwareconnectors.cygnus.containers.NotifyContextRequest.ContextAttribute;
import es.tid.fiware.fiwareconnectors.cygnus.containers.NotifyContextRequest.ContextElement;
import es.tid.fiware.fiwareconnectors.cygnus.containers.NotifyContextRequest.ContextElementResponse;
import es.tid.fiware.fiwareconnectors.cygnus.log.CygnusLogger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.flume.Context;
import org.apache.log4j.Logger;

/**
 * OrionStatsSink generates online statistics given a certain number of configured entity's attributes from Orion.
 * Online means no previous measure is stored, but statistics are iteratively built given the previous value of the
 * statistics and the current measure.
 * 
 * This class feeds Orion as well, by updating a "_stats" sufixed version of the original entity (see OrionBackend
 * implementation).
 * 
 * @author frb
 */
public class OrionStatsSink extends OrionSink {
    
    private Logger logger;
    private StatsContainer stats;
    private String orionHost;
    private String orionPort;
    private OrionBackend backend;
    
    /**
     * Constructor.
     */
    public OrionStatsSink() {
        super();
        logger = CygnusLogger.getLogger(OrionStatsSink.class);
        stats = new StatsContainer();
    } // OrionStatsSink
    
    @Override
    public void configure(Context context) {
        orionHost = context.getString("orion_host", "localhost");
        logger.debug("[" + this.getName() + "] Reading configuration (orion_host=" + orionHost + ")");
        orionPort = context.getString("orion_port", "1026");
        logger.debug("[" + this.getName() + "] Reading configuration (orion_port=" + orionPort + ")");
    } // configure

    @Override
    public void start() {
        // create the persistence backend
        backend = new OrionBackend(orionHost, orionPort);
        
        // start
        super.start();
        logger.info("[" + this.getName() + "] Startup completed");
    } // start
    
    @Override
    void persist(Map<String, String> eventHeaders, NotifyContextRequest notification) throws Exception {
        // iterate on the contextResponses
        ArrayList contextResponses = notification.getContextResponses();
        
        for (int i = 0; i < contextResponses.size(); i++) {
            // get the i-th contextElement
            ContextElementResponse contextElementResponse = (ContextElementResponse) contextResponses.get(i);
            ContextElement contextElement = contextElementResponse.getContextElement();
            String entityId = contextElement.getId();
            String entityType = contextElement.getType();
            logger.debug("[" + this.getName() + "] Processing context element (id=" + entityId + ", type=" + entityType
                    + ")");

            // iterate on all this CKANBackend's attributes, if there are attributes
            ArrayList<ContextAttribute> contextAttributes = contextElement.getAttributes();
            
            if (contextAttributes == null || contextAttributes.isEmpty()) {
                logger.warn("No attributes within the notified entity, nothing is done (id=" + entityId + ", type="
                        + entityType + ")");
                continue;
            } // if

            ArrayList<ContextAttributeStats> allAttrStats = new ArrayList<ContextAttributeStats>();
                    
            for (ContextAttribute contextAttribute : contextAttributes) {
                String attrName = contextAttribute.getName();
                String attrType = contextAttribute.getType();
                String attrValue = contextAttribute.getContextValue(true);
                logger.debug("[" + this.getName() + "] Processing context attribute (name=" + attrName + ", type="
                        + attrType + ")");
                ContextAttributeStats contextAttributeStats = stats.updateStats(entityId, entityType, attrName,
                        attrType, new Double(attrValue).doubleValue());
                allAttrStats.add(contextAttributeStats);
            } // for
            
            backend.updateContext(entityId, entityType, allAttrStats);
        } // for
    } // persist
    
    /**
     * Container for all the entity's attributes. It is based on nested hashmaps: a first level for the typed entities,
     * a second level for the typed attributes and a third level containing the specific attribute's statistics.
     */
    private class StatsContainer {
        
        private HashMap entitiesMap;
        
        /**
         * Constructor.
         */
        public StatsContainer() {
            entitiesMap = new HashMap();
        } // stats
        
        /**
         * Updates the statistics regarding an entity's attribute.
         * @param entityId
         * @param entityType
         * @param attrName
         * @param attrType
         * @param value
         * @return
         */
        public ContextAttributeStats updateStats(String entityId, String entityType, String attrName, String attrType,
                double value) {
            // get typed entity id and attribute name
            String typedEntityId = entityId + "-" + entityType;
            String typedAttrName = attrName + "-" + attrType;
            
            // get the stats through the typed entity id and attribute name, creating them if not existing yet
            ContextAttributeStats stats = null;
            HashMap attrsMap = (HashMap) entitiesMap.get(typedEntityId);
            
            if (attrsMap == null) {
                attrsMap = new HashMap();
                stats = new ContextAttributeStats(entityId, entityType);
                attrsMap.put(typedAttrName, stats);
                entitiesMap.put(typedEntityId, attrsMap);
            } else {
                stats = (ContextAttributeStats) attrsMap.get(typedAttrName);
                
                if (stats == null) {
                    stats = new ContextAttributeStats(attrName, attrType);
                    attrsMap.put(typedAttrName, stats);
                } // if
            } // if else
            
            // update the stats
            stats.updateStats(value);
            return stats;
        } // updateStats
        
    } // StatsContainer
    
    /**
     * Statistics regarding a single entity's attribute.
     */
    public class ContextAttributeStats {
        
        private String attrName;
        private String attrType;
        private long numMeasures;
        private double max;
        private double min;
        private double average;
        private double variation;
        
        /**
         * Constructor.
         */
        public ContextAttributeStats(String attrName, String attrType) {
            this.attrName = attrName;
            this.attrType = attrType;
            numMeasures = 0;
            max = 0;
            min = 0;
            average = 0;
            variation = 0;
        } // ContextAttributeStats
        
        /**
         * Updates the statistics given a new value.
         * @param value
         */
        public void updateStats(double value) {
            if (numMeasures == 0 || numMeasures == Long.MAX_VALUE) {
                numMeasures = 1;
                max = value;
                min = value;
                average = value;
                variation = 0;
            } else {
                numMeasures++;
                
                if (value > max) {
                    max = value;
                } // if else if

                if (value < min) {
                    min = value;
                } // if

                average = (((numMeasures - 1) / numMeasures) * average) + (value / numMeasures);
                variation = (((numMeasures - 1) / numMeasures) * variation)
                        + (Math.pow((value - average), 2) / (numMeasures - 1));
            } // if else
        } // updateStats
        
        /**
         * Gets the maximum.
         * @return
         */
        public double getMax() {
            return max;
        } // getMax
        
        /**
         * Gets the minimum.
         * @return
         */
        public double getMin() {
            return min;
        } // getMin
        
        /**
         * Gets the average.
         * @return
         */
        public double getAverage() {
            return average;
        } // getAverage

        /**
         * Gets the variation.
         * @return
         */
        public double getVariation() {
            return variation;
        } // getVariation
        
        /**
         * Gets the attribute name.
         * @return
         */
        public String getName() {
            return attrName;
        } // getName
        
        /**
         * Gets the attribute type.
         * @return
         */
        public String getType() {
            return attrType;
        } // getType

    } // ContextAttributeStats

} // OrionStatsSink
