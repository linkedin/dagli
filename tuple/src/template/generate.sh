#!/usr/bin/env bash

MY_DIR="`dirname \"$0\"`"

fmpp ${MY_DIR}/ftl/tuple/Tuple.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/tuple/Tuple.java -S ${MY_DIR}/ftl &
fmpp ${MY_DIR}/ftl/tuple/TupleGenerators.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/tuple/TupleGenerators.java -S ${MY_DIR}/ftl &

for index in `seq 0 19`;
do
  fmpp ${MY_DIR}/ftl/tuple/TupleValueX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/tuple/TupleValue${index}.java -S ${MY_DIR}/ftl -D index:${index} &
done

for arity in `seq 1 20`;
do
  fmpp ${MY_DIR}/ftl/tuple/TupleX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/tuple/Tuple${arity}.java -S ${MY_DIR}/ftl -D arity:${arity} &
  fmpp ${MY_DIR}/ftl/tuple/ArrayTupleX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/tuple/ArrayTuple${arity}.java -S ${MY_DIR}/ftl -D arity:${arity} &
  fmpp ${MY_DIR}/ftl/tuple/FieldTupleX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/tuple/FieldTuple${arity}.java -S ${MY_DIR}/ftl -D arity:${arity} &
done

wait

javaformat ${MY_DIR}/generated