#!/bin/bash
# Test script for Wednesday Season 2 Episode 2

echo "Testing Wednesday S02E02 playback..."

# Simulate user input: 1 (select Wednesday) -> 2 (Season 2) -> 2 (Episode 2)
echo -e "1\n2\n2" | ./dist/index.js wednesday --debug 2>&1 | tee /tmp/topster-test.log

echo ""
echo "Check debug log at: ~/.topster/debug.log"
echo "Check test output at: /tmp/topster-test.log"
