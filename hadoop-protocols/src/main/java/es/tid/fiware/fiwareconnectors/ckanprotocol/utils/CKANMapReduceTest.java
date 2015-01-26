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

package es.tid.fiware.fiwareconnectors.ckanprotocol.utils;

import es.tid.fiware.fiwareconnectors.ckanprotocol.hadoop.ckan.CKANInputFormat;
import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 *
 * @author frb
 */
public final class CKANMapReduceTest extends Configured implements Tool {
    
    /**
     * Constructor. It is private since utility classes should not have a public or default constructor.
     */
    private CKANMapReduceTest() {
    } // CKANMapReduceTest

    /**
     * Mapper class.
     */
    public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {
        
        private static final IntWritable ONE = new IntWritable(1);
        private Text word = new Text();

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(value.toString());
            
            while (itr.hasMoreTokens()) {
                word.set(itr.nextToken());
                context.write(word, ONE);
            } // while
        } // map
    } // TokenizerMapper

    /**
     * Reducer class.
     */
    public static class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        
        private IntWritable result = new IntWritable();

        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context)
            throws IOException, InterruptedException {
            int sum = 0;
            
            for (IntWritable val : values) {
                sum += val.get();
            } // for
            
            result.set(sum);
            context.write(key, result);
        } // reduce
    } // IntSumReducer

    /**
     * Main class.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new CKANMapReduceTest(), args);
        System.exit(res);
    } // main
    
    @Override
    public int run(String[] args) throws Exception {
        Configuration conf = this.getConf();
        Job job = Job.getInstance(conf, "CKAN MapReduce test");
        job.setJarByClass(CKANMapReduceTest.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        job.setInputFormatClass(CKANInputFormat.class);
        CKANInputFormat.addCKANInput(job,
                "https://data.lab.fiware.org/dataset/logrono_cygnus/resource/ca73a799-9c71-4618-806e-7bd0ca1911f4");
        CKANInputFormat.setCKANEnvironmnet(job, "data.lab.fiware.org", "443", true,
                "2d5bf021-ff9f-48e3-bb97-395b77581665");
        FileOutputFormat.setOutputPath(job, new Path(args[0]));
        return job.waitForCompletion(true) ? 0 : 1;
    } // main
    
} // CKANMapReduceTest
