#!/usr/bin/env bash

MY_DIR="`dirname \"$0\"`"

${MY_DIR}/generate-dag.sh &
${MY_DIR}/generate-tuple.sh &
${MY_DIR}/generate-preparer.sh &
${MY_DIR}/generate-transformer.sh &
${MY_DIR}/generate-function.sh &

wait