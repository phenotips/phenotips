#!/bin/sh

# ---------------------------------------------------------------------------
# Optional ENV vars
# -----------------
#   START_OPTS - parameters passed to the Java VM when running Jetty
#     e.g. to increase the memory allocated to the JVM to 1GB, use
#       set START_OPTS=-Xmx1024m
# ---------------------------------------------------------------------------

# Ensure that the commands below are always started in the directory where this script is
# located. To do this we compute the location of the current script.
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
PRGDIR=`dirname "$PRG"`
cd "$PRGDIR"

echo $PRGDIR

JETTY_HOME=jetty

# If no START_OPTS env variable has been defined use default values.
if [ -z "$START_OPTS" ] ; then
  START_OPTS="-Xmx512m -XX:MaxPermSize=128m"
fi

# The port on which to start Jetty can be passed to this script as the first argument
if [ -n "$1" ]; then
  JETTY_PORT=$1
else
  JETTY_PORT=8080
fi

# The port on which to stop Jetty can be passed to this script as the second argument
if [ -n "$2" ]; then
  JETTY_STOPPORT=$2
else
  JETTY_STOPPORT=8079
fi

echo Starting Jetty on port $JETTY_PORT ...

# Ensure the logs directory exists as otherwise Jetty reports an error
mkdir -p $JETTY_HOME/logs 2>/dev/null

# Ensure the work directory exists so that Jetty uses it for its temporary files.
mkdir -p $JETTY_HOME/work 2>/dev/null

# Ensure the data directory exists so that Solr can use it for storing permanent data
mkdir -p data 2>/dev/null

# Specify port on which HTTP requests will be handled
START_OPTS="$START_OPTS -Djetty.port=$JETTY_PORT"

# Specify Jetty's home directory
START_OPTS="$START_OPTS -Djetty.home=$JETTY_HOME"

# Specify port and key to stop a running Jetty instance
START_OPTS="$START_OPTS -DSTOP.KEY=solrjettystop -DSTOP.PORT=$JETTY_STOPPORT"

# Specify the encoding to use
START_OPTS="$START_OPTS -Dfile.encoding=UTF8"

# Path to the solr configuration
START_OPTS="$START_OPTS -Dsolr.solr.home=${PRGDIR}/solrconfig/"


java $START_OPTS $3 $4 $5 $6 $7 $8 $9 -jar $JETTY_HOME/start.jar OPTIONS=All
