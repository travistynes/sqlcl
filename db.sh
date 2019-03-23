#!/bin/bash

# See: http://mywiki.wooledge.org/WrapperScript

export JAVA_HOME=/path/to/java
export PATH=$JAVA_HOME/bin:$PATH

dir=$(dirname $BASH_SOURCE)
cd $dir
lib=./lib
jar=./sqlcl-1.2.jar

# Invoke application. "$@" passes along arguments to script as arguments to program.
exec java --add-opens java.base/java.lang=ALL-UNNAMED -Dloader.path="$lib" -jar "$jar" "$@"
