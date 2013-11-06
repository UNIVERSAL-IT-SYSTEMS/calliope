#Check for cassandra home
if [ -z "$CASSANDRA_HOME" ]; then
    echo "You must set the CASSANDRA_HOME var" >&2
    exit 1
fi

#Start cassandra
$CASSANDRA_HOME/bin/cassandra -p pid.txt

#wait for cluster to be ready
sleep 10

#create tables
$CASSANDRA_HOME/bin/cqlsh -f init.cql

sleep 10

kill `cat pid.txt`