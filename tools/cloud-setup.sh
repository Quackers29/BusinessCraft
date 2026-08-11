#!/usr/bin/env bash
#
# BusinessCraft Cloud Agent environment setup.
#
# Idempotent bootstrap for a Cursor Cloud Agent VM. It provisions the Java 17
# toolchain that ForgeGradle 6 / fabric-loom require (the base image ships
# Java 21, which ForgeGradle 6.0.16 does not run on cleanly), makes the Gradle
# wrapper executable, and warms the Gradle build so Minecraft/Forge is
# downloaded, decompiled, and all module dependencies are resolved.
#
# Safe to run repeatedly: every step checks state before acting.

set -euo pipefail

JAVA17_HOME="/usr/lib/jvm/java-17-openjdk-amd64"

log() { printf '[cloud-setup] %s\n' "$*"; }

# Prefer sudo when not already root (Cloud Agent runs as the "ubuntu" user).
SUDO=""
if [ "$(id -u)" -ne 0 ]; then
  SUDO="sudo"
fi

# 1. Ensure the Java 17 JDK is installed.
if [ ! -x "${JAVA17_HOME}/bin/javac" ]; then
  log "Installing OpenJDK 17..."
  $SUDO apt-get update
  $SUDO apt-get install -y openjdk-17-jdk-headless
else
  log "OpenJDK 17 already present."
fi

# 2. Make Java 17 the default toolchain so ./gradlew runs on it.
if command -v update-alternatives >/dev/null 2>&1; then
  $SUDO update-alternatives --set java "${JAVA17_HOME}/bin/java" || true
  $SUDO update-alternatives --set javac "${JAVA17_HOME}/bin/javac" || true
fi
export JAVA_HOME="${JAVA17_HOME}"

# 3. Pin Gradle to Java 17 for this user (persists in the home directory,
#    kept out of the shared repo gradle.properties which must stay portable).
mkdir -p "${HOME}/.gradle"
GRADLE_PROPS="${HOME}/.gradle/gradle.properties"
if ! grep -q "^org.gradle.java.home=" "${GRADLE_PROPS}" 2>/dev/null; then
  echo "org.gradle.java.home=${JAVA17_HOME}" >> "${GRADLE_PROPS}"
  log "Pinned Gradle JDK in ${GRADLE_PROPS}."
fi

# 4. The Gradle wrapper is committed without the execute bit on some checkouts.
chmod +x ./gradlew

# 5. Warm the build: resolves dependencies, downloads + decompiles Minecraft,
#    and compiles the common/forge/fabric modules. Tests are intentionally
#    excluded here; run them explicitly with `./gradlew :common:test`.
log "Warming Gradle build (assemble)..."
./gradlew --no-daemon assemble

log "Environment ready. Java: $("${JAVA17_HOME}/bin/java" -version 2>&1 | head -n1)"
