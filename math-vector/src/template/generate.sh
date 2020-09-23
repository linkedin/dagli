#!/usr/bin/env bash
# Generates Java source from the Freemarker Templates (ftl files) in this directory.  This should be run any time the
# ftl files are modified.  You'll need fmpp (the Freemarker standalone utility) available on your PATH for this script
# to run correctly.

MY_DIR="`dirname \"$0\"`"

TYPENAME=(Float Double)

for typeIndex in `seq 0 1`;
do
  fmpp ${MY_DIR}/ftl/math/vector/DenseXArrayVector.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/math/vector/Dense${TYPENAME[typeIndex]}ArrayVector.java -S ${MY_DIR}/ftl -D typeIndex:${typeIndex} &
  fmpp ${MY_DIR}/ftl/math/vector/DenseXBufferVector.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/math/vector/Dense${TYPENAME[typeIndex]}BufferVector.java -S ${MY_DIR}/ftl -D typeIndex:${typeIndex} &
  fmpp ${MY_DIR}/ftl/math/vector/SparseXArrayVector.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/math/vector/Sparse${TYPENAME[typeIndex]}ArrayVector.java -S ${MY_DIR}/ftl -D typeIndex:${typeIndex} &
  fmpp ${MY_DIR}/ftl/math/vector/SparseXMapVector.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/math/vector/Sparse${TYPENAME[typeIndex]}MapVector.java -S ${MY_DIR}/ftl -D typeIndex:${typeIndex} &
done

wait

javaformat ${MY_DIR}/generated