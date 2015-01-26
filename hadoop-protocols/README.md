# Hadoop Protocols
## Prerequisites

Maven (and thus Java SDK, since Maven is a Java tool) is needed in order to install and run Cygnus.

In order to install Java SDK (not JRE), just type (CentOS machines):

    $ yum install java-1.6.0-openjdk-devel

Remember to export the JAVA_HOME environment variable. In the case of using `yum install` as shown above, it would be:

    $ export JAVA_HOME=/usr/lib/jvm/java-1.6.0-openjdk.x86_64

In order to do it permanently, edit `/root/.bash_profile` (`root` user) or `/etc/profile` (other users).

Maven is installed by downloading it from [maven.apache.org](http://maven.apache.org/download.cgi). Install it in a folder of your choice (represented by `APACHE_MAVEN_HOME`):

    $ wget http://www.eu.apache.org/dist/maven/maven-3/3.2.5/binaries/apache-maven-3.2.5-bin.tar.gz
    $ tar xzvf apache-maven-3.2.5-bin.tar.gz
    $ mv apache-maven-3.2.5 APACHE_MAVEN_HOME

## Installing Hadoop Protocols and its dependencies

Build the project; this can be done by including the dependencies in the package (**recommended**):

    $ git clone https://github.com/telefonicaid/XXXXXX.git
    $ git checkout <branch>
    $ APACHE_MAVEN_HOME/bin/mvn clean compile assembly:single

or not:

    $ git clone https://github.com/telefonicaid/XXXXXX.git
    $ git checkout <branch>
    $ APACHE_MAVEN_HOME/bin/mvn package

where `<branch>` is `develop` if you are trying to install the latest features or `release/x.y.z` if you are trying to install a stable release.

If the dependencies are included in the built Hadoop Protocols package, then nothing has to be done. If not, and depending on the Hadoop Protocols components you are going to use, you may need to install additional .jar files somewhere in the classpath. Typically, you can get the .jar file from your Maven repository (under .m2 in your user home directory) and use the `cp` command.

## Running

    $ hadoop jar target/ckan-protocol-0.1_SNAPSHOT-jar-with-dependencies.jar es.tid.fiware.fiwareconnectors.ckanprotocol.utils.CKANMapReduceTest -libjars target/ckan-protocol-0.1_SNAPSHOT-jar-with-dependencies.jar -D mapreduce.job.queuename=<my_user_queue> /user/<my_user>/ckan_output