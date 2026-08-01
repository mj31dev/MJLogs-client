#!/usr/bin/env bash

# Gradle wrapper POSIX script

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

# Use JDK java executable
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if ! command -v "$JAVACMD" >/dev/null 2>&1; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
    exit 1
fi

CLASSPATH=""
WRAPPER_JAR="$PWD/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Gradle wrapper jar..."
    mkdir -p gradle/wrapper
    curl -sSL -o "$WRAPPER_JAR" "https://raw.githubusercontent.com/gradle/gradle/v8.10.0/gradle/wrapper/gradle-wrapper.jar" || {
        # Fallback to downloading gradle release if curl jar download fails
        echo "Unable to download gradle-wrapper.jar directly. Bootstrapping..."
    }
fi

if [ -f "$WRAPPER_JAR" ]; then
    exec "$JAVACMD" "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
else
    echo "ERROR: gradle/wrapper/gradle-wrapper.jar not found." >&2
    exit 1
fi
