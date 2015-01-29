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
 * For those usages not covered by the GNU Affero General Public License please contact with
 * francisco.romerobueno at telefonica dot com
 */

package es.tid.fiware.fiwareconnectors.ckanprotocol.hadoop.ckan;

import es.tid.fiware.fiwareconnectors.ckanprotocol.backends.ckan.CKANBackend;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.util.StringUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author frb
 * 
 * https://hadoop.apache.org/docs/current/api/org/apache/hadoop/mapred/InputFormat.html
 * https://github.com/apache/hadoop/blob/trunk/hadoop-mapreduce-project/hadoop-mapreduce-client/ \
 *    hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/InputFormat.java
 * 
 */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class CKANInputFormat extends InputFormat<LongWritable, Text> {
    
    private Logger logger;
    public static final String CKAN_SSL = "mapreduce.input.ckaninputformat.ssl";
    public static final String CKAN_API_KEY = "mapreduce.input.ckaninputformat.apikey";
    public static final String INPUT_URLS = "mapreduce.input.ckaninputformat.inputurls";
    private static final int CKAN_BLOCK_SIZE = 1000;
    private static CKANBackend backend = null;
    
    /**
     * Constructor.
     */
    public CKANInputFormat() {
        logger = Logger.getLogger(CKANInputFormat.class);
    } // CKANInputFormat
    
    /**
     * Sets the CKAN API key.
     * @param job
     * @param ckanAPIKey
     */
    public static void setCKANEnvironmnet(Job job, String ckanHost, String ckanPort, boolean ssl, String ckanAPIKey) {
        backend = new CKANBackend(ckanHost, ckanPort, ssl, ckanAPIKey);
    } // setCKANAPIKey
    
    /**
     * Adds a new URL-based CKAN input to the list of already added inputs.
     * @param job
     * @param ckanURL
     */
    public static void addCKANInput(Job job, String ckanURL) {
        Configuration conf = job.getConfiguration();
        String inputs = conf.get(INPUT_URLS, "");
        
        if (inputs.isEmpty()) {
            inputs += ckanURL;
        } else {
            inputs += "," + ckanURL;
        } // if else
        
        conf.set(INPUT_URLS, inputs);
    } // addCKANInput

    @Override
    public RecordReader<LongWritable, Text> createRecordReader(InputSplit split, TaskAttemptContext context) {
        if (backend == null) {
            logger.info("Unable to create a CKANRecordReader, it seems the CKAN environment was not properly set");
            return null;
        } // if else
        
        // create a reader
        return new CKANRecordReader(backend, split, context);
    } // createRecordReader
    
    @Override
    public List<InputSplit> getSplits(JobContext job) {
        // resulting splits container
        List<InputSplit> splits = new ArrayList<InputSplit>();
        
        // get the Job configuration
        Configuration conf = job.getConfiguration();
        
        // get the inputs, i.e. the list of CKAN URLs
        String input = conf.get(INPUT_URLS, "");
        String[] ckanURLs = StringUtils.split(input);
        
        // iterate on the CKAN URLs, they may be related to whole organizations, packages/datasets or specific resources
        for (String ckanURL: ckanURLs) {
            if (isCKANOrg(ckanURL)) {
                splits.addAll(getSplitsOrg(ckanURL, job.getConfiguration()));
            } else if (isCKANPkg(ckanURL)) {
                splits.addAll(getSplitsPkg(ckanURL, job.getConfiguration()));
            } else {
                splits.addAll(getSplitsRes(ckanURL, job.getConfiguration()));
            } // if else if
        } // for
        
        // return the splits
        return splits;
    } // getSplits

    private boolean isCKANOrg(String ckanURL) {
        return ckanURL.contains("organization");
    } // isCKANOrg
    
    private boolean isCKANPkg(String ckanURL) {
        return ckanURL.contains("dataset") && !ckanURL.contains("resource");
    } // isCKANPkg
    
    private List<InputSplit> getSplitsOrg(String orgURL, Configuration conf) {
        if (backend == null) {
            logger.error("Unable to get the input splits, it seems the CKAN environment was not properly set");
            return null;
        } // if
        
        // get the organization identifier from the URL
        String[] urlParts = orgURL.split("/");
        String orgId = urlParts[urlParts.length - 1];
        
        // resulting splits container
        List<InputSplit> splits = new ArrayList<InputSplit>();
        
        // get all the organization packages
        List<String> pkgURLs = backend.getPackages(orgId);

        // for each package, get the splits for its resources
        for (String pkgURL : pkgURLs) {
            List<InputSplit> pkgSplits = getSplitsPkg(pkgURL, conf);
            splits.addAll(pkgSplits);
        } // for

        // return the splits
        return splits;
    } // getSplitsOrg
    
    private List<InputSplit> getSplitsPkg(String pkgURL, Configuration conf) {
        if (backend == null) {
            logger.error("Unable to get the input splits, it seems the CKAN environment was not properly set");
            return null;
        } // if
        
        // get the package identifier from the URL
        String[] urlParts = pkgURL.split("/");
        String pkgId = urlParts[urlParts.length - 1];
        
        // resulting splits container
        List<InputSplit> splits = new ArrayList<InputSplit>();

        // get all the package resources
        List<String> resIds = backend.getResources(pkgId);

        // for each package, get the splits for its resources
        for (String resId : resIds) {
            List<InputSplit> resSplits = getSplitsRes(resId, conf);
            splits.addAll(resSplits);
        } // for

        // return the splits
        return splits;
    } // getSplitsPkg
    
    private List<InputSplit> getSplitsRes(String resURL, Configuration conf) {
        if (backend == null) {
            logger.error("Unable to get the input splits, it seems the CKAN environment was not properly set");
            return null;
        } // if
        
        // get the resource identifier from the URL
        String[] urlParts = resURL.split("/");
        String resId = urlParts[urlParts.length - 1];
        
        // resulting splits container
        List<InputSplit> splits = new ArrayList<InputSplit>();
        
        // calculate the number of complete blocks
        int numRecords = backend.getNumRecords(resId);
        
        if (numRecords == 0) {
            return splits;
        } // if
        
        int numCompleteBlocks = numRecords / CKAN_BLOCK_SIZE;
        int i;
        
        // add a split for each complete block
        for (i = 0; i < numCompleteBlocks; i++) {
            splits.add(new CKANInputSplit(resId, i * CKAN_BLOCK_SIZE, CKAN_BLOCK_SIZE));
        } // for
        
        // add a split for the remaining records (uncomplete block)
        splits.add(new CKANInputSplit(resId, i * CKAN_BLOCK_SIZE, numRecords - (i * CKAN_BLOCK_SIZE)));
        
        // return the splits
        return splits;
    } // getSplitsRes

} // CKANInputFormat
