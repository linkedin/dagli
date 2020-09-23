#!/usr/bin/env bash

MY_DIR="`dirname \"$0\"`"

for i in `seq 1 10`;
do
  fmpp ${MY_DIR}/ftl/preparer/TrivialPreparerX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/preparer/TrivialPreparer${i}.java -S ${MY_DIR}/ftl -D arity:${i} &
  fmpp ${MY_DIR}/ftl/preparer/PreparerX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/preparer/Preparer${i}.java -S ${MY_DIR}/ftl -D arity:${i} &
  fmpp ${MY_DIR}/ftl/preparer/AbstractPreparerX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/preparer/AbstractPreparer${i}.java -S ${MY_DIR}/ftl -D arity:${i} &
  fmpp ${MY_DIR}/ftl/preparer/AbstractStreamPreparerX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/preparer/AbstractStreamPreparer${i}.java -S ${MY_DIR}/ftl -D arity:${i} &
  fmpp ${MY_DIR}/ftl/preparer/AbstractBatchPreparerX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/preparer/AbstractBatchPreparer${i}.java -S ${MY_DIR}/ftl -D arity:${i} &
done

wait

javaformat ${MY_DIR}/generated/com/linkedin/dagli/preparer