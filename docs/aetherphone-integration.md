# Aetherphone-style XIVChat shell

The Aetherphone repository is a Dalamud/ImGui plugin. Its `PhoneShell` and
`HomeScreen` are rendered inside the game and its social applications use the
Aethernet HTTPS/WebSocket service. It does not expose a local TCP listener that
an Android client can reuse.

XIVChat already has the local, encrypted transport needed by the Android
client. The plugin listens on the configured port (14777 by default), performs
the existing public-key handshake, and streams the game chat, player data and
friend list through the same session. The Android shell therefore uses one
connection and one port; it does not open a second Aetherphone socket.

The Android test shell in `AndroidShell/` borrows the visual structure of
Aetherphone's phone window: a dark glass home screen, status line, app tiles and
a dock. `XIVChat` is the first app tile. Tapping it dismisses the shell and
reveals the existing messages destination. The navigation drawer still exposes
friends, servers and settings, so no duplicate friend-list implementation is
introduced.

This keeps the protocol boundary explicit: game data remains in XIVChat's
authenticated stream, while Aetherphone's online social backend remains a
separate optional service. A future Aethernet client can be added as another
tile without changing the XIVChat TCP framing.
