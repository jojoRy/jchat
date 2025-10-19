#!/usr/bin/env sh
# Minimal bootstrapper; ensure gradle wrapper JAR exists or run 'gradle wrapper' once locally.
DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
exec "${JAVA_HOME:-/usr/bin/java}" -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
