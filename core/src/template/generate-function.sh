#!/usr/bin/env bash

MY_DIR="`dirname \"$0\"`"

for i in `seq 1 10`;
do
  fmpp ${MY_DIR}/ftl/function/FunctionResultX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/function/FunctionResult${i}.java -S ${MY_DIR}/ftl -D arity:${i} &
done

wait

javaformat ${MY_DIR}/generated/com/linkedin/dagli/function