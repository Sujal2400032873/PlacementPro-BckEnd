@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup script for Windows, version 3.2.0
@REM
@REM Optional ENV vars
@REM   MVNW_REPOURL - repo urlbase for downloading maven
@REM   MVNW_VERBOSE - enables verbose log output
@REM ----------------------------------------------------------------------------

@IF "%DEBUG%" == "on" @ECHO ON
@setlocal EnableDelayedExpansion

set ERROR_CODE=0

@REM To isolate internal variables from possible generic names, use MVNW_ prefix
@REM for local variables.

@REM set the path of the wrapper jar
set MVNW_JAR_PATH=.mvn\wrapper\maven-wrapper.jar

@REM if no wrapper jar is found, try to download it
@IF NOT EXIST "%MVNW_JAR_PATH%" (
    IF "%MVNW_VERBOSE%" == "true" (
        echo "Couldn't find %MVNW_JAR_PATH%, downloading it ..."
    )
    
    set MVNW_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
    @IF NOT "%MVNW_REPOURL%" == "" (
        set MVNW_URL=%MVNW_REPOURL%/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
    )

    powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient = New-Object System.Net.WebClient; $webclient.DownloadFile('!MVNW_URL!', '!MVNW_JAR_PATH!') }"
)

@REM set the command to run maven
set MAVEN_CMD_LINE_ARGS=%*
set MAVEN_PROJECTBASEDIR=%~dp0
@IF "%MAVEN_PROJECTBASEDIR:~-1%" == "\" set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%
set MVNW_JAVA_EXE=java.exe

@IF NOT "%JAVA_HOME%" == "" (
    set MVNW_JAVA_EXE="%JAVA_HOME%\bin\java.exe"
)

%MVNW_JAVA_EXE% -classpath %MVNW_JAR_PATH% "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %MAVEN_CMD_LINE_ARGS%

@IF %ERRORLEVEL% NEQ 0 (
  set ERROR_CODE=%ERRORLEVEL%
)

@endlocal & set ERROR_CODE=%ERROR_CODE%

exit /B %ERROR_CODE%
