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

/**
 * Statistics regarding a single entity's attribute.
 * 
 * @author frb
 */
public class ContextAttributeStats {

    private String attrName;
    private String attrType;
    private double numMeasures;
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
        if (numMeasures == 0 || numMeasures == Double.MAX_VALUE) {
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