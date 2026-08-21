# Dalamud plugins — game API mapping (first draft)

This is the first draft (v1) of the Dalamud/"卫月" plugin API mapping and plugin index.  I created this file as the initial deliverable and pushed it to the `api` branch.

Location: docs/dalamud_plugins_api_mapping_v1.md (in branch `api`).

---

## Summary

Goal: find public Dalamud plugins that read game state and identify which game/Dalamud APIs they call so you can implement a plugin that reads game data and forwards it to your app.

What I delivered in this first draft:
- a short, prioritized index of representative Dalamud plugins (initial 10+ examples),
- for each example: the main Dalamud/game interfaces used and one or two source links (permalink) showing the calls,
- a short consolidated list of common Dalamud / game APIs to rely on,
- recommended ways for a plugin to send data to an external app and short pros/cons,
- next steps (how I'll produce the full "top 100" detailed mapping in the branch).

Note: this is a working draft. The automated code search I use returns partial results per query — I'll run a full pass over the top 100 candidates next and attach per-repo code snippets & permalinks.

---

## Representative plugin examples (quick index & findings)

I started by extracting and confirming interface usage from several popular/open plugins. Each entry below lists the repo, a short note, and 1–2 source permalinks where the plugin uses Dalamud/game APIs.

1) TPie — ring hotbar plugin
   - Repo: https://github.com/Tischel/TPie
   - Observed APIs: IClientState, IObjectTable, ICommandManager, IDalamudPluginInterface, IDataManager, IFramework, IGameGui, ISigScanner, IGameInteropProvider, IKeyState, IPluginLog, ITextureProvider
   - Example source: TPie/Plugin.cs — shows services injected and singletons: https://github.com/Tischel/TPie/blob/a861c2132284430e6b2d4666d6fc708051d9b115/TPie/Plugin.cs
   - Helpers using game data: CooldownHelper.cs (uses ActionManager / FFXIVClientStructs): https://github.com/Tischel/TPie/blob/a861c2132284430e6b2d4666d6fc708051d9b115/TPie/Helpers/CooldownHelper.cs

2) DelvUI — full UI replacement
   - Repo: https://github.com/DelvUI/DelvUI
   - Observed APIs: many Dalamud services (IPluginLog, IChatGui, IGameGui, IFramework, ISigScanner, IObjectTable, IClientState, ITextureProvider, INotificationManager, IPartyList, ITargetManager, etc.), use of FFXIVClientStructs and hooking for some features
   - Example sources: README and PullTimerHelper (hooking Agent update, GameInteropProvider hooks): https://github.com/DelvUI/DelvUI/blob/edc6ec52620ddf3feaa28115e69329a09b865f4a/README.md
     https://github.com/DelvUI/DelvUI/blob/edc6ec52620ddf3feaa28115e69329a09b865f4a/DelvUI/Helpers/PullTimerHelper.cs

3) Brio — GPose / actor APIs + plugin IPC
   - Repo: https://github.com/Etheirys/Brio
   - Observed APIs: many Dalamud services injected via [PluginService], framework update events, IDataManager/Lumina, IObjectTable, IClientState; provides its own IPC API for other plugins (BrioAPI), uses ICallGateSubscriber
   - Example sources: DalamudService, BrioAPI bindings: https://github.com/Etheirys/Brio/blob/76254a165e839fc97e4edae065905d7c3976652e/Brio/Game/Core/DalamudService.cs
     https://github.com/Etheirys/Brio/blob/76254a165e839fc97e4edae065905d7c3976652e/BrioAPI_V2.cs

4) Triad Buddy (FFTriadBuddy) — reads UI + game memory for Triple Triad
   - Repo: https://github.com/MgAl2O4/FFTriadBuddyDalamud
   - Observed APIs: IDataManager, ICommandManager, IFramework, IGameGui, ISigScanner, ITextureProvider; uses unsafe memory readers to parse game UI/agents; uses UiReaderScheduler to find addons by name.
   - Example sources: plugin/service and UIReaderScheduler: https://github.com/MgAl2O4/FFTriadBuddyDalamud/blob/1bffa57921d3eedbb201c227b6ea05f35a0ee75e/plugin/Service.cs
     https://github.com/MgAl2O4/FFTriadBuddyDalamud/blob/1bffa57921d3eedbb201c227b6ea05f35a0ee75e/utils/UIReaderScheduler.cs

5) DeathRecap — packet hooks and combat/event capture
   - Repo: https://github.com/Kouzukii/ffxiv-deathrecap
   - Observed APIs: IDalamudPluginInterface injection, ICommandManager, IDataManager, IChatGui, IObjectTable, IPartyList, ICondition, IClientState, IFramework, GameInteropProvider + HookFromSignature, FFXIVClientStructs for parsing packets. Uses custom payloads in chat links to open UI.
   - Example sources: Service.cs (services via plugin interface) and CombatEventCapture hooking signatures: https://github.com/Kouzukii/ffxiv-deathrecap/blob/658ec3a19614f225e354b207ebba87aaf64943c7/Service.cs
     https://github.com/Kouzukii/ffxiv-deathrecap/blob/658ec3a19614f225e354b207ebba87aaf64943c7/Events/CombatEventCapture.cs

6) MidiBard — midi player plugin that integrates with game state and IPC
   - Repo: https://github.com/akira0245/MidiBard
   - Observed APIs: many Dalamud services via injection (plugin interface, ClientState, ChatGui, DataManager, GameGui, KeyState, etc.), uses IPC and local socket/agent logic to coordinate playback across clients, agent/agent interface for Gold Saucer features
   - Example sources: Midibard/DalamudApi/api.cs (service declarations), IPC handlers: https://github.com/akira0245/MidiBard/blob/976b78b801f1ad84ace1ab86f91c31fe9fe5e769/Midibard/DalamudApi/api.cs
     https://github.com/akira0245/MidiBard/blob/976b78b801f1ad84ace1ab86f91c31fe9fe5e769/Midibard/IPC/IPCHandles.cs

7) LMeter — ACT integration (overlay) and many Dalamud services
   - Repo: https://github.com/lichie567/LMeter
   - Observed APIs: IClientState, ICommandManager, ICondition, IDalamudPluginInterface, IDataManager, IFramework, IGameGui, IJobGauges, IObjectTable, IPartyList, ITargetManager, IChatGui, ITextureProvider, INotificationManager; integrates with ACT via WebSocket or IPC
   - Example sources: LMeter/Plugin.cs (service registration & singletons): https://github.com/lichie567/LMeter/blob/7921aeb703321be2feb28f147c3be2b7a9649c71/LMeter/Plugin.cs

8) BossMod — deep game integration, hooks, FFXIVClientStructs
   - Repo: https://github.com/awgil/ffxiv_bossmod
   - Observed APIs: extensive FFXIVClientStructs usage, ActionManager hooks, SigScanner, HookAddress use, IClientState, IObjectTable, IFramework and more
   - Example sources: Service.cs (services via PluginService), Data/ClientState.cs and numerous hook usages: https://github.com/awgil/ffxiv_bossmod/blob/162fde51b5e56ca133cbc13502ae03548a23f461/BossMod/Framework/Service.cs
     https://github.com/awgil/ffxiv_bossmod/blob/162fde51b5e56ca133cbc13502ae03548a23f461/BossMod/Data/ClientState.cs

9) AutoDuty — automation/navigation plugin (hooks, memory reads)
   - Repo: https://github.com/ffxivcode/AutoDuty
   - Observed APIs: many Dalamud services, SigScanner, hooks, FFXIVClientStructs, uses in-memory calls to perform movement & UI automation
   - Example sources: AutoDuty/AutoDuty.cs and helpers: https://github.com/ffxivcode/AutoDuty/blob/53f4e7400f9791f4534f803a2ec2d2ff709375ba/AutoDuty/AutoDuty.cs

10) VFXEditor — VFX editing, FFXIVClientStructs, UI hooks
   - Repo: https://github.com/0ceal0t/Dalamud-VFXEditor
   - Observed APIs: IClientState, IFramework, ICommandManager, IObjectTable, ISigScanner, IDataManager, ITargetManager, IGameInteropProvider, and direct FFXIVClientStructs usage for VFX and animation manipulation
   - Example sources: Dalamud service registration and interop: https://github.com/0ceal0t/Dalamud-VFXEditor/blob/d29a10b7ab9013d274cca7e9b7298e69678348fc/VFXEditor/Dalamud.cs


---

## Common Dalamud / game interfaces you will use (consolidated)

These are the services & APIs that appear across many plugins and are the primary entry points to read game state. When you implement your plugin, request them in the constructor or via [PluginService] injection.

- IDalamudPluginInterface (plugin lifecycle, UiBuilder, GetIpcSubscriber / CallGate)
- IClientState (LocalPlayer, IsPvP, TerritoryType, etc.)
- IObjectTable (search objects, LocalPlayer, iterate scene objects)
- IFramework (RunOnTick, Update events)
- IDataManager / Lumina (GetExcelSheet<T> — static game data)
- ICommandManager (slash commands)
- IChatGui (send/receive chat, AddChatLinkHandler)
- IGameGui (GetAddonByName, UI addon pointers)
- IKeyState (keyboard state)
- ISigScanner (signature scanning: for hooking or locating functions)
- IGameInteropProvider (HookFromSignature / HookFromAddress)
- IPluginLog (logging)
- ITextureProvider (icon/texture lookups)
- ICondition (ConditionFlag checks — in combat, zoning, cutscene)
- IPartyList / ITargetManager / IJobGauges / IToastGui / INotificationManager — optional but common
- FFXIVClientStructs — direct memory structures for low-level readers & hooks

When possible, prefer using Dalamud provided high-level services (IClientState, IObjectTable, IDataManager). Use FFXIVClientStructs and signature hooks only when necessary (e.g., to capture packets, or read UI data that Dalamud doesn't expose).

---

## Sending plugin data to your external app — recommended options

1) WebSocket (local) — recommended for real-time bidirectional data. Plugin acts as WebSocket client connecting to your app's local server or vice versa. Pros: realtime, cross-platform. Cons: need local port and firewall considerations.
2) HTTP POST to local app — simple to implement; plugin posts JSON. Pros: easy; Cons: one-way and polling required or server push needed.
3) TCP/UDP socket — low-level efficient; UDP for lossy telemetry, TCP for reliability. Cons: manual framing and reconnect handling.
4) File (JSON / SQLite) — simplest, safe; app polls files. Cons: latency and file locking considerations.
5) Dalamud Plugin IPC — only for communication between Dalamud plugins (not external apps).

Security / compliance notes:
- Avoid automating game actions that could be considered cheating. Reading state is normally fine, but automated actions or packet injection can be risky.
- When opening network listeners, restrict to loopback (127.0.0.1) to avoid exposing local data.

Example small WebSocket push (C#-pseudo, inside plugin)

```csharp
// Pseudo: run on a background thread
var ws = new ClientWebSocket();
await ws.ConnectAsync(new Uri("ws://127.0.0.1:42000"), CancellationToken.None);
while(ws.State == WebSocketState.Open) {
  var payload = JsonConvert.SerializeObject(new { time = DateTime.UtcNow, player = clientState.LocalPlayer?.Name?.TextValue });
  var bytes = Encoding.UTF8.GetBytes(payload);
  await ws.SendAsync(bytes, WebSocketMessageType.Text, true, CancellationToken.None);
  await Task.Delay(250);
}
```

---

## Next steps (what I'll run next and deliver to `api` branch)

- Gather a prioritized list of the *top 100* candidate repositories (search results filtered for real Dalamud plugins). I'll generate a table of those 100 repos with quick tags (is_fork, last_updated, stars) and then run code extraction over them.
- For each confirmed Dalamud plugin in that 100, extract the exact code locations where game/Dalamud services are consumed and where any external communication (HTTP/WS/IPC/file) is implemented. I'll add per-plugin subsections with permalinks and short API lists.
- Push the full mapping document(s) to `docs/` on the `api` branch. I'll also include a machine-readable CSV/JSON index that your app can later consume if desired.

Estimated ETA for the complete "Top 100" pass and per-repo code snippets: ~6–12 hours. I will push intermediate updates to the `api` branch as I process groups of repositories.

---

If you'd like, I can (next) start by producing the prioritized 100-repo index and commit it to the branch. Confirm and I'll proceed — I'll then run the automated extraction over them and push the first full batch of per-plugin entries.

