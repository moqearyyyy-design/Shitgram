# Shitgram Build Instructions

## Prerequisites
- Android SDK (API 36)
- NDK 27.2.12479018
- JDK 17+
- Node.js (for backend)

## Backend Server
```bash
cd backend
start-server.bat
```
This starts the LAN server on port 8787. Open http://localhost:8787 in browser to grant test stars.

## Android App Build

### Option 1: Gradle Command Line
```bash
./gradlew :TMessagesProj_App:assembleStandalone
```
Output APK: `TMessagesProj_App/build/outputs/apk/standalone/app.apk`

### Option 2: Android Studio
1. Open project in Android Studio
2. Select `standalone` build variant
3. Build > Build Bundle(s) / APK(s) > Build APK(s)

## Project Structure
- `TMessagesProj/` - Main library module (namespace: com.shitgram.messenger)
- `TMessagesProj_App/` - Main app module
- `backend/` - Node.js LAN server

## Key Changes from Telegram
1. **Package**: `org.telegram.messenger` → `com.shitgram.messenger`
2. **Auth**: Removed phone/SMS login, replaced with nickname + server IP
3. **Network**: Replaced MTProto with HTTP/WebSocket to local LAN server
4. **UI**: Kept Telegram chat UI (bubbles, enter view, action bar)
5. **Features**: Added fake gifts/stars system

## LAN Client API (LanClient)
- `login(name, serverUrl, callback)` - Create session
- `fetchMessages(after)` - Poll for new messages
- `sendMessage(text)` - Send text message
- `sendGift(to, title, cost)` - Send gift (transfers stars)
- `fetchMe()` - Get current user's stars
- `logout()` - Clear session

## Testing
1. Start backend server on PC
2. Install APK on 2+ Android devices on same WiFi
3. Enter PC's LAN IP (e.g., http://192.168.1.100:8787) and nickname
4. Chat locally!
5. Use browser on PC (http://localhost:8787) to grant stars to users

## Architecture
```
┌─────────────┐     HTTP/WS      ┌─────────────┐
│  Shitgram   │ ◄──────────────► │  Node.js    │
│  (Android)  │   Port 8787      │  Backend    │
└─────────────┘                  └─────────────┘
       ▲                              ▲
       │                              │
       └────────── LAN ───────────────┘
```