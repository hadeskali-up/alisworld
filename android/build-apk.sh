#!/bin/bash
# Quick setup script for AlisWorld Android build

echo "═══════════════════════════════════════"
echo "  AlisWorld Android Build Setup"
echo "═══════════════════════════════════════"
echo

# Check Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️  ANDROID_HOME not set"
    echo "   Install Android Studio and set ANDROID_HOME"
    echo "   Example: export ANDROID_HOME=/c/Users/User/AppData/Local/Android/Sdk"
    echo
fi

# Check if in android directory
if [ ! -f "build.gradle.kts" ]; then
    echo "❌ Run this script from android/ directory"
    exit 1
fi

# Check local.properties
if [ ! -f "local.properties" ]; then
    echo "📝 Creating local.properties..."
    cat > local.properties << EOF
sdk.dir=${ANDROID_HOME:-/c/Users/User/AppData/Local/Android/Sdk}
backend.url=http://10.0.2.2:8000
backend.apiKey=test123
EOF
    echo "✅ local.properties created"
    echo "   ⚠️  Edit backend.url and backend.apiKey for your setup"
    echo
fi

# Check google-services.json
if [ ! -f "app/google-services.json" ]; then
    echo "⚠️  app/google-services.json missing"
    echo "   Template exists at android/app/google-services.json"
    echo "   For full Firebase: Download from Firebase Console"
    echo
else
    echo "✅ google-services.json found"
fi

# Build
echo "🔨 Building APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo
    echo "═══════════════════════════════════════"
    echo "  ✅ Build Success!"
    echo "═══════════════════════════════════════"
    echo
    echo "APK: app/build/outputs/apk/debug/app-debug.apk"
    echo
    echo "Install:"
    echo "  adb install app/build/outputs/apk/debug/app-debug.apk"
    echo
else
    echo
    echo "═══════════════════════════════════════"
    echo "  ❌ Build Failed"
    echo "═══════════════════════════════════════"
    echo
    echo "Check:"
    echo "  1. Android SDK installed?"
    echo "  2. ANDROID_HOME set?"
    echo "  3. local.properties correct?"
    echo "  4. google-services.json exists?"
    echo
fi
