#!/usr/bin/env bash

rm -rf etc/bluebash
git clone https://github.com/blueanvil/bluebash etc/bluebash
etc/bluebash/src/tools/elasticsearch.sh start etc/temp 6.6.0 blueanvil 9200 9300

