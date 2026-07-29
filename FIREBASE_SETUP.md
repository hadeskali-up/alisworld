# Firebase Setup for AlisWorld

## ⚠️ IMPORTANT

The API key provided appears to be a Firebase API/FCM key. However, for full Firebase integration, you need:

1. **For Android**: Complete `google-services.json` from Firebase Console
2. **For Backend**: Firebase service account JSON for admin SDK

## What's Been Done

Created a basic `google-services.json` template with your API key in:
- `android/app/google-services.json`

**However**: This is a TEMPLATE ONLY. For full functionality, you need the actual file from Firebase Console.

## How to Get Proper Firebase Files

### 1. Firebase Console Setup

1. Go to https://console.firebase.google.com
2. Create project: "AlisWorld MT5" (or use existing)
3. Add Android app:
   - Package name: `com.alisworld.app`
   - App nickname: AlisWorld
   - Download `google-services.json`
   - **Replace** the template file with this real one

### 2. Backend Firebase Admin Setup

1. Firebase Console → Project Settings → Service Accounts
2. Click "Generate new private key"
3. Download the JSON file
4. Save as `backend/firebase-service-account.json`
5. Update `backend/.env`:
   ```
   FIREBASE_CREDENTIALS_PATH=firebase-service-account.json
   ```

### 3. GitHub Actions Secret

For auto-build APK with Firebase:

1. Go to https://github.com/hadeskali-up/alisworld/settings/secrets/actions
2. Add secret:
   - Name: `GOOGLE_SERVICES_JSON`
   - Value: Paste entire content of real `google-services.json`

## Current API Key

Stored in template google-services.json:
```
BMimjrWsIJlXg2VWDq-JwnDu1Li_NI5mEH-fw4dgV7Y4LrrG2T1k4ghTou9DUB677y-GBOC4YM7s3zQ0R73Veos
```

**Note**: This key format suggests it might be:
- Firebase Web API key
- FCM Server Key (legacy)

For security, this should be treated as sensitive and not committed to public repos.

## What Works Now vs What Needs Real Firebase

### ✅ Will Work (with template)
- Android app compiles
- Basic structure in place

### ❌ Won't Work (needs real Firebase setup)
- Push notifications (FCM)
- Backend notification sending
- Firebase Analytics
- Remote Config
- Crashlytics

## Recommended Action

1. Follow steps above to get real `google-services.json`
2. Replace the template file
3. Get service account JSON for backend
4. Test notifications end-to-end

---

**Current Status**: Template created with your API key, but proper Firebase setup still needed for notifications.
