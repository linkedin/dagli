#!/usr/bin/env bash
# Generates Java source from the Freemarker Templates (ftl files) in this directory.  This should be run any time the
# ftl files are modified.  You'll need fmpp (the Freemarker standalone utility) available on your PATH for this script
# to run correctly.

MY_DIR="`dirname \"$0\"`"

PREFIX=('' Void Boolean Byte Character Short Int Long Float Double)

for arity in `seq 0 11`;
do
  fmpp ${MY_DIR}/ftl/util/function/BooleanNegatedFunction.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/util/function/BooleanNegatedFunction${arity}.java -S ${MY_DIR}/ftl -D arity:${arity} &
done

for typeIndex in `seq 0 9`;
do
  fmpp ${MY_DIR}/ftl/util/function/VariadicFunction.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/util/function/${PREFIX[typeIndex]}FunctionVariadic.java -S ${MY_DIR}/ftl -D typeIndex:${typeIndex} &

  for arity in `seq 1 11`;
  do
    fmpp ${MY_DIR}/ftl/util/function/DefaultOnNullArgument.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/util/function/${PREFIX[typeIndex]}DefaultOnNullArgument${arity}.java -S ${MY_DIR}/ftl -D arity:${arity},typeIndex:${typeIndex} &
  done

  for arity in `seq 0 11`;
  do
    fmpp ${MY_DIR}/ftl/util/function/Function.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/util/function/${PREFIX[typeIndex]}Function${arity}.java -S ${MY_DIR}/ftl -D arity:${arity},typeIndex:${typeIndex} &
    fmpp ${MY_DIR}/ftl/util/function/MethodReference.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/util/function/${PREFIX[typeIndex]}MethodReference${arity}.java -S ${MY_DIR}/ftl -D arity:${arity},typeIndex:${typeIndex} &
    fmpp ${MY_DIR}/ftl/util/function/ComposedFunction.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/util/function/${PREFIX[typeIndex]}ComposedFunction${arity}.java -S ${MY_DIR}/ftl -D arity:${arity},typeIndex:${typeIndex} &
  done
done

wait

javaformat ${MY_DIR}/generated