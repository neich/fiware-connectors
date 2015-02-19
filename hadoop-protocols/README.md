#<a name="section0"></a>Hadoop Protocols

* [What are Hadoop Protocols](#section1)
* [Building](#section2)
  * [Prerequisites](#section2.1)
  * [Building Hadoop Protocols and its dependencies](#section2.2)
* [Usage](#section3)
  * [CKAN extensions](#section3.1)
  * [NGSI extensions](#section3.2)
* [Contact](#section4)

##<a name="section1"></a>What are Hadoop Protocols

Hadoop Protocols is a suite of extensions for [Hadoop](apache.hadoop.org), the <i>defacto</i> Big Data standard for batch data analysis based in the MapReduce paradigm.

Typically, MapReduce applications within Hadoop read the data to be analyzed from HDFS, the Hadoop Distributed File System. Neverhteless, it may happen the data meant to be analyzed is not in HDFS, but in a remote non-HDFS storage. In that cases, the data must be moved to HDFS in order it can be read by MapReduce applications.

By using Hadoop Protocols you will not need to copy data from remote to HDFS anymore, but you will be able to read such data by pointing to it. At least, this is true if your data is stored at:

* [CKAN](ckan.org), the Open Data platform.
* [NGSI Short-Term Historic](https://github.com/telefonicaid/IoT-STH), the NGSI-oriented storage for [Orion Context Broker](https://github.com/telefonicaid/fiware-orion).

The extensions provide specific Java classes per each remote storage type; those classes must be packaged into a Java jar file.

[Top](#section0)
##<a name="section2"></a>Building
###<a name="section2.1"></a>Prerequisites

Maven (and thus Java SDK, since Maven is a Java tool) is needed in order to build Hadoop Protocols.

In order to install Java SDK (not JRE), just type (CentOS machines):

    $ yum install java-1.6.0-openjdk-devel

Remember to export the JAVA_HOME environment variable. In the case of using `yum install` as shown above, it would be:

    $ export JAVA_HOME=/usr/lib/jvm/java-1.6.0-openjdk.x86_64

In order to do it permanently, edit `/root/.bash_profile` (`root` user) or `/etc/profile` (other users).

Maven is installed by downloading it from [maven.apache.org](http://maven.apache.org/download.cgi). Install it in a folder of your choice (represented by `APACHE_MAVEN_HOME`):

    $ wget http://www.eu.apache.org/dist/maven/maven-3/3.2.5/binaries/apache-maven-3.2.5-bin.tar.gz
    $ tar xzvf apache-maven-3.2.5-bin.tar.gz
    $ mv apache-maven-3.2.5 APACHE_MAVEN_HOME

[Top](#section0)

###<a name="section2.2"></a>Building Hadoop Protocols and its dependencies

Build the project; this can be done by including the dependencies in the package (**recommended**):

    $ git clone https://github.com/telefonicaid/XXXXXX.git
    $ git checkout <branch>
    $ APACHE_MAVEN_HOME/bin/mvn clean compile assembly:single

or not:

    $ git clone https://github.com/telefonicaid/XXXXXX.git
    $ git checkout <branch>
    $ APACHE_MAVEN_HOME/bin/mvn package

where `<branch>` is `develop` if you are trying to install the latest features or `release/x.y.z` if you are trying to install a stable release.

If the dependencies are included in the built Hadoop Protocols package, then nothing has to be done. If not, and depending on the Hadoop Protocols components you are going to use, you may need to install additional .jar files somewhere in the classpath. Typically, you can get the .jar file from your Maven repository (under `/home/<your_user>/.m2` folder in your user home directory) and use the `cp` command.

Once you have built Hadoop Protocols a `hadoop-protocols-x.y.z-jar-with-dependencies.jar` / `hadoop-protocols-x.y.z.jar` (depending on the dependencies are packaged or not, respectively) file will appear under `target/` folder. This jar is the file you will need to add to your Hadoop classpath in order to use the Hadoop Protocols extensions. 

[Top](#section0)

##<a name="section3"></a>Usage

###<a name="section3.1"></a>CKAN extensions

**Disclaimer:** If you are reading this section then you should be familiar with CKAN concepts and hierarchies. However, as a quick reminder, it will be said that CKAN organizes the data into <i>organizations</i>, having each organization a set of <i>packages</i> or <i>datasets</i>, having each package/organization a set of <i>resources</i>. Finally, each resource has a list of data records.

A good way to learn about CKAN extensions is to have a look to the testing purpose MapReduce application distributed with Hadoop Protocols. This can be found at `src/main/java/es/tid/fiware/fiwareconnectors/ckanprotocol/utils/CKANMapReduceTest.java`. Such a testing application is a simply record count application, following the idea behind the Hadoop's <i>Hello World</i> application, which counts the appearances of each word within a text.

Map an Reduce classes are the typical ones for a count behaviour, nothing special is done regarding CKAN interfacing:

```
map
```

```
reduce
```

The relevant part of the code can be found within the `main` class, where the MapReduce job is defined, configured and finally started:

``` java
@Override
public int run(String[] args) throws Exception {
    // check the number of arguments, show the usage if it is wrong
    if (args.length != 7) {
        showUsage();
    } // if
    
    // get the arguments
    String ckanHost = args[0];
    String ckanPort = args[1];
    boolean sslEnabled = args[2].equals("true");
    String ckanAPIKey = args[3];
    String ckanInputs = args[4];
    String hdfsOutput = args[5];
    String splitsLength = args[6];
        
    // create and configure a MapReduce job
    Configuration conf = this.getConf();
    Job job = Job.getInstance(conf, "CKAN MapReduce test");
    job.setJarByClass(CKANMapReduceTest.class);
    job.setMapperClass(TokenizerMapper.class);
    job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(IntSumReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    job.setInputFormatClass(CKANInputFormat.class);
    CKANInputFormat.addCKANInput(job, ckanInputs);
    CKANInputFormat.setCKANEnvironmnet(job, ckanHost, ckanPort, sslEnabled, ckanAPIKey);
    CKANInputFormat.setCKANSplitsLength(job, splitsLength);
    FileOutputFormat.setOutputPath(job, new Path(hdfsOutput));
        
    // run the MapReduce job
    return job.waitForCompletion(true) ? 0 : 1;
} // main
```

As can be seen, everything is as usual in a MapReduce application... except for the input format; in order to use the CKAN extensions the `CKANInputFormat` class must be set. This class, when asked for the input splits definition, will create a bunch of `CKANInputSplit` objects, and when asked for a record reader for those input splits, it will create a specific `CKANRecordReader`.

A `CKANInputSlit` references the resource where the records can be found, in addition to the offset start and length within that resource. Thus, assuming a CKAN resource has 3580 records and the maximum length for a split is set to 1000 then 4 splits will be defined:

* split #1: offset start=0, length=1000 (1000 records are represented)
* split #2: offset start=1000, length=2000 (1000 records are represented)
* split #3: offset start=2000, length=3000 (1000 records are represented)
* split #4: offset start=3000, length=580 (580 records are represented)

The `CKANInputFormat` is configured by providing:

* The host where the CKAN server runs.
* The port used to connect to the CKAN server, usually `80` or `443`.
* If SSL is enabled (typically, this is equals to `true` if the `443` port is used).
* The CKAN API key necessary to authenticate against the CKAN server.
* The input data within CKAN taht will be analyzed.
* The splits lenght.

Regarding the input data, worths mentioning this may be a CKAN <i>organization</i> name, a <i>package/dataset</i> name or a <i>resource</i> identifier. Both in the case of organizations and packages/datasets these are internally expanded until the resoure level. Thus for instance, by configuring a whole organization all the packages under it, and all the resources under all the packages will be processed. In addition, the method `setCKANInput` supports a list of comma-separated inputs of any type (e.g. you can combine a single organization with a subset of packages under any other organization, or selected resources from several packages related to several organizations).

Finally, the application can be run as:

    $ hadoop jar \
        target/ckan-protocol-0.1-jar-with-dependencies.jar \
        es.tid.fiware.fiwareconnectors.ckanprotocol.utils.CKANMapReduceTest \
        -libjars target/ckan-protocol-0.1-jar-with-dependencies.jar \
        <ckan host> \
        <ckan port> \
        <ssl enabled=true|false> \
        <ckan API key> \
        <comma-separated list of ckan inputs> \
        <hdfs output folder> \
        <splits length>
        
An example of run:



[Top](#section0)

###<a name="section3.2"></a>NGSI extensions

To be released.

[Top](#section0)

##<a name="section4"></a>Contact
Francisco Romero Bueno (francisco dot romerobueno at telefonica dot com)

[Top](#section0)