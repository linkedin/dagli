#!/usr/bin/env bash

MY_DIR="`dirname \"$0\"`"

for i in `seq 1 10`;
do
  fmpp ${MY_DIR}/ftl/transformer/TupledX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/transformer/Tupled${i}.java -S ${MY_DIR}/ftl -D arity:${i} &
  z=$((i-1))
  fmpp ${MY_DIR}/ftl/transformer/ValueXFromTuple.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/transformer/Value${z}FromTuple.java -S ${MY_DIR}/ftl -D arity:${i} &

  fmpp ${MY_DIR}/ftl/transformer/TransformerX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/transformer/Transformer${i}.java -S ${MY_DIR}/ftl -D arity:${i} &
  fmpp ${MY_DIR}/ftl/transformer/PreparableTransformerX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/transformer/PreparableTransformer${i}.java -S ${MY_DIR}/ftl -D arity:${i} &
  fmpp ${MY_DIR}/ftl/transformer/PreparedTransformerX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/transformer/PreparedTransformer${i}.java -S ${MY_DIR}/ftl -D arity:${i} &

  fmpp ${MY_DIR}/ftl/transformer/internal/TransformerXInternalAPI.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/transformer/internal/Transformer${i}InternalAPI.java -S ${MY_DIR}/ftl -D arity:${i} &
  fmpp ${MY_DIR}/ftl/transformer/internal/PreparableTransformerXInternalAPI.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/transformer/internal/PreparableTransformer${i}InternalAPI.java -S ${MY_DIR}/ftl -D arity:${i} &
  fmpp ${MY_DIR}/ftl/transformer/internal/PreparedTransformerXInternalAPI.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/transformer/internal/PreparedTransformer${i}InternalAPI.java -S ${MY_DIR}/ftl -D arity:${i} &

  fmpp ${MY_DIR}/ftl/transformer/AbstractTransformerX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/transformer/AbstractTransformer${i}.java -S ${MY_DIR}/ftl -D arity:${i} &
  fmpp ${MY_DIR}/ftl/transformer/AbstractPreparableTransformerX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/transformer/AbstractPreparableTransformer${i}.java -S ${MY_DIR}/ftl -D arity:${i} &
  fmpp ${MY_DIR}/ftl/transformer/AbstractPreparedTransformerX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/transformer/AbstractPreparedTransformer${i}.java -S ${MY_DIR}/ftl -D arity:${i} &
  fmpp ${MY_DIR}/ftl/transformer/AbstractPreparedStatefulTransformerX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/transformer/AbstractPreparedStatefulTransformer${i}.java -S ${MY_DIR}/ftl -D arity:${i} &

  fmpp ${MY_DIR}/ftl/transformer/ConstantResultTransformationX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/transformer/ConstantResultTransformation${i}.java -S ${MY_DIR}/ftl -D arity:${i} &

  fmpp ${MY_DIR}/ftl/transformer/WrapperTransformerX.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/transformer/WrapperTransformer${i}.java -S ${MY_DIR}/ftl -D arity:${i} &
done

wait

javaformat ${MY_DIR}/generated/com/linkedin/dagli/transformer