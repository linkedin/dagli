#!/usr/bin/env bash
# Generates Java source from the Freemarker Templates (ftl files) in this directory.  This should be run any time the
# ftl files are modified.  You'll need fmpp (the Freemarker standalone utility) available on your PATH for this script
# to run correctly.

MY_DIR="`dirname \"$0\"`"

fmpp ${MY_DIR}/ftl/util/array/ArraysEx.ftl -o ${MY_DIR}/generated/com/linkedin/dagli/util/array/ArraysEx.java -S ${MY_DIR}/ftl &

wait

javaformat ${MY_DIR}/generated