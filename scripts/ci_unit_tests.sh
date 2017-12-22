#!/usr/bin/env bash

./gradlew assembleEnvProdMinApi21Debug testEnvProdMinApi21DebugUnitTest testEnvProdMinApi21DebugUnitTestCoverage coveralls -Dpre-dex=false -Pkotlin.incremental=false --stacktrace
