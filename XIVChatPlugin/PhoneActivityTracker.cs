using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text.Json;
using Dalamud.Game.DutyState;
using FFXIVClientStructs.FFXIV.Client.Game;
using FFXIVClientStructs.FFXIV.Client.Game.UI;
using Lumina.Excel.Sheets;
using XIVChatCommon.Message.Server;

namespace XIVChatPlugin {
    internal sealed unsafe class PhoneActivityTracker : IDisposable {
        private sealed class Day {
            public string Date { get; set; } = string.Empty;
            public long PlaySeconds { get; set; }
            public long ExpGained { get; set; }
            public int LevelsGained { get; set; }
            public long GilEarned { get; set; }
            public int DutiesCompleted { get; set; }
        }

        private readonly Plugin _plugin;
        private readonly Stopwatch _watch = Stopwatch.StartNew();
        private readonly List<Day> _days = [];
        private readonly ServerActivity _snapshot = new();
        private ulong _contentId;
        private Day _today = new();
        private long _lastTickUnix;
        private long _lastSaveUnix;
        private long _baselineGil = -1;
        private bool _hasExpBaseline;
        private uint _baselineJobId;
        private int _baselineLevel;
        private long _baselineExp;
        private long _baselineNeededExp;
        private int _collectionTick;
        private uint[]? _mountIds;
        private uint[]? _minionIds;
        private bool _dirty;

        internal PhoneActivityTracker(Plugin plugin) {
            _plugin = plugin;
            _snapshot.SessionStartedUnix = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            XIVChatPlugin.Plugin.DutyState.DutyCompleted += OnDutyCompleted;
            XIVChatPlugin.Plugin.ClientState.Logout += OnLogout;
        }

        internal ServerActivity? Update() {
            if (_watch.Elapsed < TimeSpan.FromSeconds(2)) return null;
            _watch.Restart();
            var playerState = PlayerState.Instance();
            var player = XIVChatPlugin.Plugin.ObjectTable.LocalPlayer;
            if (playerState == null || player == null || playerState->ContentId == 0) return null;

            if (_contentId != playerState->ContentId) SwitchCharacter(playerState->ContentId);
            RollDay();
            var now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            var elapsed = _lastTickUnix == 0 ? 0 : Math.Clamp(now - _lastTickUnix, 0, 10);
            _lastTickUnix = now;
            if (elapsed > 0) {
                _today.PlaySeconds += elapsed;
                _snapshot.SessionPlaySeconds += elapsed;
                _dirty = true;
            }

            SampleExperience(playerState, player.ClassJob.RowId);
            SampleGil();
            SampleRetainers();
            if (_collectionTick++ % 30 == 0) SampleCollections(playerState);
            CopyToday();
            _snapshot.UpdatedUnix = now;
            if (_dirty && now - _lastSaveUnix >= 60) Save();
            return _snapshot;
        }

        internal ServerActivity Snapshot() => _snapshot;

        private void OnDutyCompleted(IDutyStateEventArgs args) {
            if (_contentId == 0) return;
            _today.DutiesCompleted++;
            _snapshot.SessionDutiesCompleted++;
            _dirty = true;
        }

        private void SwitchCharacter(ulong contentId) {
            Save();
            _contentId = contentId;
            _days.Clear();
            try {
                var path = PathFor(contentId);
                if (File.Exists(path)) _days.AddRange(JsonSerializer.Deserialize<List<Day>>(File.ReadAllText(path)) ?? []);
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not load activity history: {ex.Message}");
            }
            _today = DayFor(DateTime.Now.ToString("yyyy-MM-dd"));
            _snapshot.SessionStartedUnix = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            _snapshot.SessionPlaySeconds = 0;
            _snapshot.SessionExpGained = 0;
            _snapshot.SessionLevelsGained = 0;
            _snapshot.SessionGilEarned = 0;
            _snapshot.SessionDutiesCompleted = 0;
            _lastTickUnix = 0;
            ResetBaselines();
        }

        private void RollDay() {
            var key = DateTime.Now.ToString("yyyy-MM-dd");
            if (_today.Date == key) return;
            _today = DayFor(key);
            _dirty = true;
        }

        private Day DayFor(string key) {
            var existing = _days.FirstOrDefault(day => day.Date == key);
            if (existing != null) return existing;
            var day = new Day { Date = key };
            _days.Add(day);
            while (_days.Count > 60) _days.RemoveAt(0);
            return day;
        }

        private void SampleExperience(PlayerState* state, uint jobId) {
            var row = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<ClassJob>().GetRowOrDefault(jobId);
            if (row == null || row.Value.ExpArrayIndex < 0 || row.Value.ExpArrayIndex >= state->ClassJobLevels.Length) {
                _hasExpBaseline = false;
                return;
            }
            var level = (int)state->ClassJobLevels[row.Value.ExpArrayIndex];
            var capped = state->MaxLevel > 0 && level >= state->MaxLevel;
            var exp = capped ? 0 : (long)state->GetCurrentClassJobExp();
            var needed = capped ? 0 : (long)state->GetCurrentClassJobNeededExp();
            long gained = 0;
            var levels = 0;
            if (_hasExpBaseline && jobId == _baselineJobId) {
                if (level == _baselineLevel && exp > _baselineExp) gained = exp - _baselineExp;
                else if (level > _baselineLevel) {
                    gained = Math.Max(0, _baselineNeededExp - _baselineExp) + exp;
                    levels = level - _baselineLevel;
                }
            }
            if (gained > 0 || levels > 0) {
                _today.ExpGained += gained;
                _today.LevelsGained += levels;
                _snapshot.SessionExpGained += gained;
                _snapshot.SessionLevelsGained += levels;
                _dirty = true;
            }
            _hasExpBaseline = true;
            _baselineJobId = jobId;
            _baselineLevel = level;
            _baselineExp = exp;
            _baselineNeededExp = needed;
        }

        private void SampleGil() {
            var manager = InventoryManager.Instance();
            if (manager == null) return;
            var gil = (long)manager->GetGil();
            if (_baselineGil >= 0 && gil > _baselineGil) {
                var gained = gil - _baselineGil;
                _today.GilEarned += gained;
                _snapshot.SessionGilEarned += gained;
                _dirty = true;
            }
            _baselineGil = gil;
        }

        private void SampleRetainers() {
            _snapshot.RetainerCount = 0;
            _snapshot.VenturesReady = 0;
            _snapshot.VenturesActive = 0;
            var manager = RetainerManager.Instance();
            if (manager == null) return;
            var now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            var count = manager->GetRetainerCount();
            for (var index = 0u; index < count; index++) {
                var retainer = manager->GetRetainerBySortedIndex(index);
                if (retainer == null) continue;
                _snapshot.RetainerCount++;
                if (retainer->VentureId == 0) continue;
                if (retainer->VentureComplete <= now) _snapshot.VenturesReady++;
                else _snapshot.VenturesActive++;
            }
        }

        private void SampleCollections(PlayerState* playerState) {
            _mountIds ??= XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Mount>()
                .Where(row => row.RowId != 0 && row.Order >= 0 && row.Singular.ExtractText().Length > 0)
                .Select(row => row.RowId).ToArray();
            _minionIds ??= XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Companion>()
                .Where(row => row.RowId != 0 && row.Order != 0 && row.Singular.ExtractText().Length > 0)
                .Select(row => row.RowId).ToArray();
            _snapshot.MountsTotal = _mountIds.Length;
            _snapshot.MountsOwned = _mountIds.Count(id => playerState->IsMountUnlocked(id));
            var ui = UIState.Instance();
            if (ui == null) return;
            _snapshot.MinionsTotal = _minionIds.Length;
            _snapshot.MinionsOwned = _minionIds.Count(id => ui->IsCompanionUnlocked(id));
        }

        private void CopyToday() {
            _snapshot.TodayPlaySeconds = _today.PlaySeconds;
            _snapshot.TodayExpGained = _today.ExpGained;
            _snapshot.TodayLevelsGained = _today.LevelsGained;
            _snapshot.TodayGilEarned = _today.GilEarned;
            _snapshot.TodayDutiesCompleted = _today.DutiesCompleted;
        }

        private void ResetBaselines() {
            _baselineGil = -1;
            _hasExpBaseline = false;
        }

        private string PathFor(ulong contentId) {
            var dir = Path.Combine(XIVChatPlugin.Plugin.Interface.ConfigDirectory.FullName, "Activity");
            Directory.CreateDirectory(dir);
            return Path.Combine(dir, contentId.ToString("X16") + ".json");
        }

        private void Save() {
            if (!_dirty || _contentId == 0) return;
            try {
                File.WriteAllText(PathFor(_contentId), JsonSerializer.Serialize(_days));
                _dirty = false;
                _lastSaveUnix = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not save activity history: {ex.Message}");
            }
        }

        private void OnLogout(int type, int code) {
            Save();
            _contentId = 0;
            ResetBaselines();
        }

        public void Dispose() {
            Save();
            XIVChatPlugin.Plugin.DutyState.DutyCompleted -= OnDutyCompleted;
            XIVChatPlugin.Plugin.ClientState.Logout -= OnLogout;
        }
    }
}
