#!/usr/bin/env bash

if [ !-e etc/bluebash ]; then
    git clone https://github.com/blueanvil/bluebash etc/bluebash
fi

etc/bluebash/src/tools/elasticsearch.sh start etc/temp 6.6.0 blueanvil 9200 9300
