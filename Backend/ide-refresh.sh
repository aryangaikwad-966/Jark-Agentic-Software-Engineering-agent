#!/bin/bash
# IDE Refresh Script for Jark Backend
# This script forces a Maven project refresh to sync IDE configuration

echo "Cleaning Maven project..."
./mvnw clean

echo "Resolving dependencies..."
./mvnw dependency:resolve

echo "Building project to force IDE refresh..."
./mvnw compile

echo "IDE refresh complete. Please reload your IDE project:"
echo "- IntelliJ: Right-click pom.xml -> Maven -> Reload Project"
echo "- VS Code: Command Palette -> 'Java: Clean Java Language Server Workspace'"
echo "- Eclipse: Right-click project -> Maven -> Update Project"