#!/usr/bin/env bash

./gradlew lintEnvProdMinApi16Debug -Dpre-dex=false -Pkotlin.incremental=false --no-daemon --stacktrace