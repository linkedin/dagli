#!/usr/bin/env bash

MY_DIR="`dirname \"$0\"`"

fmpp ${MY_DIR}/ftl/visualization/AbstractVisualization.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/visualization/AbstractVisualization.java -S ${MY_DIR}/ftl &

wait

javaformat ${MY_DIR}/generated/com/linkedin/dagli/visualization