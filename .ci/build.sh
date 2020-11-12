#!/bin/bash
set -euxo pipefail

[ $JAVA_VERSION -lt 8 ] && echo "build does not support JAVA_VERSION: $JAVA_VERSION" && exit 0

echo "starting build for JAVA_VERSION: $JAVA_VERSION"

# install deps
mvn install -DskipTests=true -Dmaven.javadoc.skip=true -B -V

# clean and test
mvn clean test -B

# publish only from java 8 and master branch
if [ "$BRANCH_NAME" == "master" -a $JAVA_VERSION -eq 8 ]
then
    echo 'deploying to maven'
    mvn deploy -Dmaven.test.skip=true -B

    mkdir -p release
    mv './jDnsProxy-all/target/jDnsProxy-all.jar' 'release/jDnsProxy.jar'
fi

echo 'build success!'
exit 0
