Installation instructions
========================

##software requirements


+ [Cassandra 1.2.x] (http://cassandra.apache.org/download/)
+ [ZMQ 2.2] ( http://download.zeromq.org/zeromq-2.2.0.tar.gz)
+ [Java binding for ZMQ] (http://zeromq.org/bindings:java)
+ [AspectJ 1.6.11] (http://www.eclipse.org/aspectj/)
+ [Cassandra driver for Java] (https://github.com/datastax/java-driver)

Your system should be installed with ZMQ 2.2 and Java binding for ZeroMQ.
pom.xml file is included with above said dependencies There is a maven assembly script provided to make zip/tar.gz file of the compiled jar, it’s dependencies and installation scripts.


##Compile and build

From project root directory, Execute **mvn assembly:assembly** to compile and package generated jar and it’s dependencies as zip, tar.gz

##Instructions for installation castriggers

Extract the supplied archive file of castriggers. Run the following scripts located in bin directory to install and setup triggers.

+ `export CASSANDRA_HOME=<<PATH to cassandra home>>`
+ `cd bin`
+ `./install.sh`
+ `./setup.sh`

Those scripts perform the following actions

Copy dependency library is to  $CASSANDRA_HOME/lib

+ aspectjweaver-1.6.11.jar
+ cassandra-driver-core-1.0.2.jar
+ org.zeromq.jzmq-2.2.0.jar
+ triggers-0.1.jar (Generated jar file)


Add aspect weaver as javaagent to $CASSANDRA_HOME/conf/cassandra-env.sh file

`JVM_OPTS="$JVM_OPTS -javaagent:$CASSANDRA_HOME/lib/aspectjweaver-1.6.11.jar -Djava.library.path=/usr/local/lib" `

Set up trigger database for the first time


##Setup Trigger for a columnFamily

There is a sample triggers_example script provided in the bin directory. Modify the scripy and run as follows to setup triggers
Make sure cassandra is running before running this script

`$CASSANDRA_HOME/bin/cqlsh -f triggers_example.cql`

## API

CasTriggers provides two classes that support streaming of data. You have to include triggers-0.1.jar (Generated jar file) in classpath to process the stream of mutations at client side.

com.tuplejump.calliope.streaming.CasMutation
com.tuplejump.calliope.streaming.ColumnData

Check Javadocs on how to use them.  Javadocs are included in doc folder.

