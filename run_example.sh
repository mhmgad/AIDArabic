#!/bin/bash

export MAVEN_OPTS="-Xmx44G"

args=(${@// /\\ })

mvn -o exec:java -Dexec.mainClass="Example" -Dorg.postgresql.forcebinary=true -Dexec.args="${args[*]}"



