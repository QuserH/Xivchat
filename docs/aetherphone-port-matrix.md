# Aetherphone Android port matrix

This branch separates features by their authoritative data source. Local game data is transported through the encrypted XIVChat connection. Phone-only state remains on Android. Aethernet/community features are intentionally deferred until the local layer is complete.

## Local game data

| Module | Source in Aetherphone | Current status |
| --- | --- | --- |
| Linkpearl chat | `Core/GameChat`, `Apps/Linkpearl` | Live chat, filters, output channels and friend actions implemented |
| Friends | `Core/Contacts` | Live friend list and detail actions implemented |
| Inventory | `Core/Inventory` | Live bags, armoury, crystals, saddlebag and equipped slots implemented; icons and remote-container cache pending |
| Wallet | `Core/Wallet` | Live balances implemented |
| Weather | `Core/Game/WeatherService` | Current and forecast weather implemented |
| Jobs | `Core/Jobs/JobsReader` | Live job levels and categories implemented |
| Housing location | XIVChat housing data | Current ward/plot implemented; housing market browser is remote and deferred |
| Dailies | `Core/Dailies/DailiesReader` | Live roulettes, tribal allowance, leves, Doman donation, Wondrous Tails and custom-delivery status implemented; manual checks remain Android-local |
| Activity | `Core/Activity/ActivityTracker` | Persistent per-character session/daily tracker implemented for play time, XP, levels, Gil, duties, collections and ventures |
| Collections | `Core/Collections` + Lodestone | Local ownership summary planned; catalog/detail service is remote and deferred |
| Fishing | `Core/Game/GameSchedule`, `Apps/Fishing` | Weather view implemented; ocean schedule and route data pending |

## Phone-local features

| Module | Current status |
| --- | --- |
| Camera and photos | Android camera capture and private photo library implemented |
| Notes | Persistent local note implemented |
| Clock | Local and Eorzea time implemented; alarms/stopwatch tabs pending |
| Calculator | Basic arithmetic implemented; full expression engine pending |
| Timer | Functional single timer implemented; multiple timers pending |
| Calendar | Basic date view implemented; merged reset/event month view pending |
| Notifications | Chat and reset notifications implemented |
| Shortcuts | Fixed game command shortcuts implemented; editable macros pending |
| Games | Aetherphone mini-game catalog not yet ported |

## Shell and interaction

| Area | Current status |
| --- | --- |
| Home shell | Custom status bar, Dynamic Island base, weather widget, two-page grid and translucent dock implemented; edit mode and full island activities pending |
| App transitions | Icon-origin zoom open/close implemented; app-internal detail routes use Android-style horizontal push/back |
| Linkpearl layout | Shared chat/contacts tab bar, friend/everyone scope, online/offline grouped lists and contact detail actions implemented |
| Motion | Press spring, pager motion, weather content animation and shell zoom implemented; icon edit jiggle, drag/reorder and island activity springs pending |

## Deferred remote services

Aethergram, Chirper, Velvet, Polls, Announcements, Venues, Market, Yellow Pages, Aether Coin, casino multiplayer, telephony, streaming and server-backed collections require Aethernet APIs or other remote services. They are not treated as local placeholders and will be handled after the local layer is complete.
