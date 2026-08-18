# Aetherphone-style Android shell

This directory contains the small overlay and build script used to produce the
side-by-side Android test package. The upstream XIVChat repository does not
ship the Android source project, so the script applies the overlay to a locally
available XIVChat APK with Apktool.

The resulting package uses the application id
`io.annaclemens.xivchat.aetherphone`, so it can be installed next to the
regular XIVChat app. The shell is only an entry screen: the existing XIVChat
navigation, encrypted TCP connection, messages, friend list, and server
settings remain unchanged underneath it.

Example (PowerShell):

```powershell
./build.ps1 -BaseApk .\XIVChat.apk -ApktoolJar .\apktool.jar -OutputApk .\XIVChat-aetherphone-unsigned.apk
```

Sign the unsigned output with a separate test keystore before installing it.
