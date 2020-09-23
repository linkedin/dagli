#!/usr/bin/env bash

MY_DIR="`dirname \"$0\"`"

# DAGBuilder deprecated, now removed
# fmpp ${MY_DIR}/ftl/dag/DAGBuilder.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/dag/DAGBuilder.java -S ${MY_DIR}/ftl &
fmpp ${MY_DIR}/ftl/dag/DAG.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/dag/DAG.java -S ${MY_DIR}/ftl &
fmpp ${MY_DIR}/ftl/dag/PartialDAG.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/dag/PartialDAG.java -S ${MY_DIR}/ftl &
fmpp ${MY_DIR}/ftl/dag/DAGMakerUtil.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/dag/DAGMakerUtil.java -S ${MY_DIR}/ftl &
fmpp ${MY_DIR}/ftl/dag/DynamicDAG.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/dag/DynamicDAG.java -S ${MY_DIR}/ftl &

for i in `seq 1 10`;
do
  fmpp ${MY_DIR}/ftl/dag/AbstractDAGResultX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/dag/AbstractDAGResult${i}.java -S ${MY_DIR}/ftl -D arity:${i} &
  fmpp ${MY_DIR}/ftl/dag/DAGResultX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/dag/DAGResult${i}.java -S ${MY_DIR}/ftl -D arity:${i} &

  for j in `seq 1 10`;
  do
    fmpp ${MY_DIR}/ftl/dag/DAGXxY.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/dag/DAG${i}x${j}.java -S ${MY_DIR}/ftl -D arity:${i},resultArity:${j} &
  done
done

wait

javaformat ${MY_DIR}/generated/com/linkedin/dagli/dag