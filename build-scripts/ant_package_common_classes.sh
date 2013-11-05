#!/bin/sh

if [ $# -lt 1 ] ; then
      echo "Need the input path !"
      exit 0
fi

echo "input path is: $1"

for file in  "$1/net/sf/gratia/util/TidiedDailyRollingFileAppender\$DatedFileFilter.class"  "$1/net/sf/gratia/util/TidiedDailyRollingFileAppender.class"; do \
    [[ -z "$file" ]] && continue; \
    dir="tarball/net/`dirname \"${file##*/net/}\"`"; \
    mkdir -p "${dir}"; \
    cp -v "${file}" "${dir}"; \
    done; \
