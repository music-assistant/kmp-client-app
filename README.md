# Music Assistant Kmp Client

Music Assistant Kmp Client is a cross-platform client application designed for Android, iOS, and Java runtime environments. Developed using Kotlin Multiplatform (KMP) and Compose Multiplatform frameworks, this project aims to provide a unified codebase for seamless music management across multiple platforms.

This client interfaces with the [Music Assistant Server](https://github.com/music-assistant/server), an open-source media library manager that integrates with various streaming services and connected speakers. The server acts as the core component, running on devices like Raspberry Pi, NAS, or Intel NUC, and facilitates centralized music management.

By leveraging the capabilities of KMP and Compose Multiplatform, Music Assistant Kmp Client offers a consistent and efficient user experience across different platforms, simplifying the development process and ensuring feature parity.

## The project is on early stage of development. Any help (especially from designers and iOS developers) is appreciated.

https://youtu.be/BhwEn_68rGg

## Current set of features:

- All platforms:
  - managing MA players queues and playback;
  - local playback on device from MA library.
- Android-specific:
  - media service (background playback) and media notification in system area for quick access to players controls;
  - Android Auto support for built-in player.

## Want to try it?

It's a bit advanced, but not too hard.

1. Download and install [Android Studio](https://developer.android.com/studio).
2. Clone this project and open it in Androiud Studio.
3. Open Terminal in Android Studio and run `./gradlew assembleDebug`.
4. After successful run, the APK file will be placed in subfolder `kmp-client-app/composeApp/build/outputs/apk/debug`.
5. Install this APK to the device.

### To use the app with Android Auto you will need additional steps

1. Enable debug mode in your Android Auto:
   - in Android Auto menu on your phone, click repeatedly on `Version and permission info` text, until dialog appears, that will allow you turning dev mode on;
   - after turning it on, in overflow menu (three dots on top) choose `Developer settings`;
   - in dev settings, find and enable `Unknown sources`;
   - after this, customize your launcher to show Music Assistant.
     
2. Set up VPN.
   Since Music Assistant API isn't exposed, you will need to have active VPN connection on your phone, so the app can reach the server.
   Make sure you have Android Auto in exclusions in your VPN app, because Android Auto won't work with VPN connection.
