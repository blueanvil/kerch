#!/usr/bin/env bash

if [ ! -e etc/bluebash ]; then
    git clone https://github.com/blueanvil/bluebash etc/bluebash
fi

etc/bluebash/src/tools/elasticsearch.sh start etc/temp 7.6.2-linux-x86_64 blueanvil 9200 9300
