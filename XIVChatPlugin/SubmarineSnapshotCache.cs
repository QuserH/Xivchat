using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using XIVChatCommon.Message.Server;

namespace XIVChatPlugin {
    internal sealed class SubmarineSnapshotCache {
        private readonly IDictionary<string, ServerSubmarine> _snapshots;
        private readonly Action _save;

        internal SubmarineSnapshotCache(IDictionary<string, ServerSubmarine> snapshots, Action save) {
            this._snapshots = snapshots;
            this._save = save;
        }

        internal ServerSubmarine? Update(string? characterTag, ServerSubmarine? live) {
            // A name without a home world is not a safe cross-character cache key.
            if (string.IsNullOrWhiteSpace(characterTag) || !characterTag.Contains('@')) return null;
            var key = characterTag.ToLowerInvariant();
            this._snapshots.TryGetValue(key, out var previous);
            if (live == null || live.Vessels.Length == 0) return previous;
            if (previous != null && SameVessels(previous, live)) return previous;
            this._snapshots[key] = live;
            this._save();
            return live;
        }

        internal static string ReadName(ReadOnlySpan<byte> bytes) {
            var terminator = bytes.IndexOf((byte)0);
            return Encoding.UTF8.GetString(terminator >= 0 ? bytes[..terminator] : bytes).Trim();
        }

        private static bool SameVessels(ServerSubmarine left, ServerSubmarine right) {
            return left.Vessels.Length == right.Vessels.Length && left.Vessels.Zip(right.Vessels).All(pair =>
                pair.First.Name == pair.Second.Name && pair.First.ReturnUnix == pair.Second.ReturnUnix &&
                pair.First.RankId == pair.Second.RankId && pair.First.CurrentExp == pair.Second.CurrentExp &&
                pair.First.NextLevelExp == pair.Second.NextLevelExp);
        }
    }
}
