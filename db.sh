#!/bin/bash

export JAVA_HOME=/path/to/java
export PATH=$JAVA_HOME/bin:$PATH

dir=$(dirname $BASH_SOURCE)
cd $dir
lib=./lib
jar=./sqlcl-1.1.jar

# Invoke application. $* passes argument list as command line arguments.
java --add-opens java.base/java.lang=ALL-UNNAMED -Dloader.path="$lib" -jar "$jar" $*
