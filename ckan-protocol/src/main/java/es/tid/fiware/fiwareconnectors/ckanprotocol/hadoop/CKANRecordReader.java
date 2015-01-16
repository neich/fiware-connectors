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

package es.tid.fiware.fiwareconnectors.ckanprotocol.hadoop;

import es.tid.fiware.fiwareconnectors.ckanprotocol.backends.ckan.CKANBackend;
import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;

/**
 *
 * @author frb
 */
public class CKANRecordReader extends RecordReader<LongWritable, Text> { // FIXME: Json or CKANRecord instead of Text?
    
    private Logger logger;
    private CKANBackend backend;
    private long start;
    private long end;
    private int current; // FIXME: this should be a long integer... but arrays do not accept such a large index
    private JSONArray records;
    private LongWritable key;
    private Text value;
    
    /**
     * Constructor.
     */
    public CKANRecordReader(CKANBackend backend) {
        this.logger = Logger.getLogger(CKANRecordReader.class);
        this.backend = backend;
    } // CKANRecordReader
    
    @Override
    public void initialize(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {
        logger.info("--- 1");
        // get start, end and current positions
        CKANInputSplit ckanInputSplit = (CKANInputSplit) split;
        logger.info("--- 2");
        start = ckanInputSplit.getFirstRecordIndex();
        logger.info("--- 3");
        end = start + ckanInputSplit.getLength();
        logger.info("--- 4");
        current = 0;
        logger.info("--- 5");
        
        // query CKAN for the related resource, seeking to the start of the split
        records = backend.getRecords(ckanInputSplit.getResId(), start, end);
        logger.info("--- 6");
    } // initialize
    
    @Override
    public boolean nextKeyValue() throws IOException, InterruptedException {
        if (current < end) {
            key = new LongWritable(current);
            value = new Text(records.get(current).toString());
            current++;
            return true;
        } else {
            return false;
        } // if else
    } // nextKeyValue
    
    @Override
    public LongWritable getCurrentKey() throws IOException, InterruptedException {
        return key;
    } // getCurrentKey
    
    @Override
    public Text getCurrentValue() throws IOException, InterruptedException {
        return value;
    } // getCurrentValue
    
    @Override
    public float getProgress() throws IOException, InterruptedException {
        if (start == end) {
            return 0.0f;
        } else {
            return Math.min(1.0f, current / (float) (end - start));
        } // if else
    } // getProgress
    
    @Override
    public void close() throws IOException {
        // nothig to close, using the CKAN REST API there is no resource "opening" at all
    } // close
    
} // CKANRecordReader
