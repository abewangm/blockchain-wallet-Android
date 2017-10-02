#!/usr/bin/env bash

./gradlew testEnvProdMinApi21DebugUnitTest testEnvProdMinApi21DebugUnitTestCoverage coveralls -Dpre-dex=false -Pkotlin.incremental=false --stacktrace
