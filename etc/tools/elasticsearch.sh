#!/usr/bin/env bash

echo "*** Starting ElasticSearch"

ES_VERSION=$2
ES_CLUSTER=$3

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
mkdir -p $SCRIPT_DIR/temp/packages

TOOLDIR=$SCRIPT_DIR/temp/elasticsearch-${ES_VERSION}
PACKAGE=$SCRIPT_DIR/temp/packages/elasticsearch-${ES_VERSION}.tar.gz
PIDFILE=$SCRIPT_DIR/temp/elasticsearch.pid


killElasticsearch() {
    # Kill if it exists
    if [ -e $PIDFILE ]; then
        PID=`cat $PIDFILE`
        echo "Killing ElasticSearch with PID $PID (file $PIDFILE)"
        kill -9 $PID
    fi
}


startElasticsearch() {
    # Download ES
    if [ ! -e $PACKAGE ]; then
        DONWLOAD_URL="https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-${ES_VERSION}.tar.gz"
        echo "Downloading ElasticSearch from $DONWLOAD_URL"
        curl "$DONWLOAD_URL" > $PACKAGE
    fi

    # Unpack
    rm -rf $TOOLDIR
    mkdir -p $TOOLDIR

    tar xf $PACKAGE -C $TOOLDIR --strip-components=1
    echo "cluster.name: ${ES_CLUSTER}" >> $TOOLDIR/config/elasticsearch.yml
    echo "transport.tcp.port: 9300" >> $TOOLDIR/config/elasticsearch.yml

    # Start
    echo "Starting ElasticSearch, pid file is $PIDFILE"
    $TOOLDIR/bin/elasticsearch -d -p $PIDFILE &


    # Wait for ES to come up
    url="http://localhost:9200"
    printf "Waiting for ElasticSearch $url ..."
    STATUSCODE=$(curl --silent --output /dev/null --write-out "%{http_code}" $url)
    while [ $STATUSCODE -ne "200" ];
    do
        sleep 1
        STATUSCODE=$(curl --silent --output /dev/null --write-out "%{http_code}" $url)
    done

    printf "done!\n"
    PID=`cat $PIDFILE`
    echo "Started ElasticSearch with PID $PID"
}

case "$1" in
start)
        killElasticsearch
        startElasticsearch
        ;;

stop)
        killElasticsearch
        ;;

esac
