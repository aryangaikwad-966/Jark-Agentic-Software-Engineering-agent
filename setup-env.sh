#!/bin/bash
# Setup environment for Jark project

# Try to find Java 21 specifically (required for project)
if [ -d "/opt/homebrew/opt/openjdk@21" ]; then
    export JAVA_HOME=/opt/homebrew/opt/openjdk@21
elif [ -d "/Library/Java/JavaVirtualMachines/jdk-21.jdk" ]; then
    export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
elif command -v /usr/libexec/java_home &> /dev/null; then
    # Use Java 21 specifically
    export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null)
    if [ -z "$JAVA_HOME" ]; then
        echo "Error: Java 21 not found. Please install Java 21."
        exit 1
    fi
else
    echo "Error: Java 21 not found. Please install Java 21."
    exit 1
fi

export PATH="$JAVA_HOME/bin:$PATH"
echo "Java 21 environment configured: $JAVA_HOME"
java -version