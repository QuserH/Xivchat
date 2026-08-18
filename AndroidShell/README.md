# 艾欧泽亚终端 Android 测试版

This directory contains the small overlay and build script used to produce the
side-by-side Android test package. The upstream XIVChat repository does not
ship the Android source project, so the script applies the overlay to a locally
available XIVChat APK with Apktool.

The resulting package is labelled `艾欧泽亚终端` and uses the application id
`io.annaclemens.xivchat.aetherphone`, so it can be installed next to the
regular XIVChat app. It keeps XIVChat's encrypted TCP connection, messages,
friend list, and server settings, and adds an Aetherphone-style home screen.

Version 0.1 adds the first real game-data path: the app advertises inventory
support in its backlog request, operation 11 carries a complete inventory
snapshot, and the home screen displays the live occupied-slot and quantity
totals. The capability marker prevents the plugin from sending operation 11 to
unmodified XIVChat clients.

Example (PowerShell):

```powershell
./build.ps1 -BaseApk .\XIVChat.apk -ApktoolJar .\apktool.jar -OutputApk .\XIVChat-aetherphone-unsigned.apk
```

Sign the unsigned output with a separate test keystore before installing it.
