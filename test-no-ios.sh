#!/bin/bash
# Run all tests except iOS tests
# This avoids Gradle's automatic test aggregation which tries to run iOS tests

set -e  # Exit on error

echo "Running Desktop tests..."
./gradlew :composeApp:desktopTest

echo "Running Android Debug unit tests..."
./gradlew :composeApp:testDebugUnitTest

echo "Running Android Release unit tests..."
./gradlew :composeApp:testReleaseUnitTest

echo ""
echo "✅ All non-iOS tests completed successfully!"
