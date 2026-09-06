using System.Text;
using System.Text.Json;
using XIVChatCommon.Message.Server;
using XIVChatPlugin;

static void Check(bool condition, string name) {
    if (!condition) throw new InvalidOperationException(name);
    Console.WriteLine($"PASS {name}");
}

static ServerSubmarine Snapshot(long updated, long returnTime = 12345) => new() {
    UpdatedUnix = updated,
    Vessels = [new ServerSubmarineVessel { Name = "Test", ReturnUnix = returnTime, RankId = 30, CurrentExp = 50, NextLevelExp = 100 }],
};

Check(SubmarineSnapshotCache.ReadName(Encoding.UTF8.GetBytes("\u6f5c\u6c34\u8247\0ignored")) == "\u6f5c\u6c34\u8247", "UTF-8 name ends at NUL");
Check(SubmarineSnapshotCache.ReadName(new byte[20]) == "", "Unloaded name is empty");
Check(SubmarineSnapshotCache.ReadName(Encoding.UTF8.GetBytes("TwentyCharacterName!")) == "TwentyCharacterName!", "Full-width name needs no NUL");

var writes = 0;
var store = new Dictionary<string, ServerSubmarine>();
var cache = new SubmarineSnapshotCache(store, () => writes++);
var first = cache.Update("Alice@World", Snapshot(100));
Check(first != null && writes == 1, "Capture without any client");
Check(ReferenceEquals(first, cache.Update("alice@world", null)), "Reconnect outside workshop uses cache");
Check(ReferenceEquals(first, cache.Update("Alice@World", new ServerSubmarine { UpdatedUnix = 200 })), "Empty workshop preserves fleet");
Check(ReferenceEquals(first, cache.Update("Alice@World", Snapshot(200))) && writes == 1, "Unchanged poll avoids disk and push churn");
Check(cache.Update("Bob@World", null) == null && cache.Update("Alice", Snapshot(200)) == null, "Characters and unknown worlds stay isolated");
var changed = cache.Update("Alice@World", Snapshot(300, 23456));
Check(changed?.Vessels[0].ReturnUnix == 23456 && writes == 2, "New voyage updates snapshot");

var restored = JsonSerializer.Deserialize<Dictionary<string, ServerSubmarine>>(JsonSerializer.Serialize(store))!;
var restarted = new SubmarineSnapshotCache(restored, () => throw new InvalidOperationException("Unexpected write"));
Check(restarted.Update("Alice@World", null)?.Vessels[0].ReturnUnix == 23456, "Plugin restart retains last known voyage");
