#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup script for Unix, version 3.2.0
#
# Optional ENV vars
#   MVNW_REPOURL - repo urlbase for downloading maven
#   MVNW_VERBOSE - enables verbose log output
# ----------------------------------------------------------------------------

set -e

# to isolate internal variables from possible generic names, use MVNW_ prefix
# for local variables.

# set the path of the wrapper jar
MVNW_JAR_PATH=".mvn/wrapper/maven-wrapper.jar"

# if no wrapper jar is found, try to download it
if [ ! -f "$MVNW_JAR_PATH" ]; then
    if [ "$MVNW_VERBOSE" = "true" ]; then
        echo "Couldn't find $MVNW_JAR_PATH, downloading it ..."
    fi
    
    MVNW_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    if [ -n "$MVNW_REPOURL" ]; then
        MVNW_URL="$MVNW_REPOURL/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    fi

    if command -v curl >/dev/null 2>&1; then
        curl -sSL "$MVNW_URL" -o "$MVNW_JAR_PATH"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "$MVNW_URL" -O "$MVNW_JAR_PATH"
    else
        echo "Couldn't find curl or wget, please download $MVNW_URL to $MVNW_JAR_PATH manually."
        exit 1
    fi
fi

# set the command to run maven
MAVEN_PROJECTBASEDIR=$(pwd)
MVNW_JAVA_EXE="java"

if [ -n "$JAVA_HOME" ]; then
    MVNW_JAVA_EXE="$JAVA_HOME/bin/java"
fi

exec "$MVNW_JAVA_EXE" \
  -classpath "$MVNW_JAR_PATH" \
  "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
  org.apache.maven.wrapper.MavenWrapperMain \
  "$@"
