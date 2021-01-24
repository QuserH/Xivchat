using Newtonsoft.Json;
using Sodium;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using System.ComponentModel;
using System.Diagnostics.CodeAnalysis;
using System.IO;
using System.Linq;
using System.Text.RegularExpressions;
using XIVChatCommon.Message;
using XIVChatCommon.Message.Server;

namespace XIVChat_Desktop {
    [JsonObject]
    public class Configuration : INotifyPropertyChanged {
        public event PropertyChangedEventHandler? PropertyChanged;

        public string? LicenceKey { get; set; }

        public KeyPair KeyPair { get; set; } = PublicKeyBox.GenerateKeyPair();

        public ObservableCollection<SavedServer> Servers { get; set; } = new ObservableCollection<SavedServer>();
        public HashSet<TrustedKey> TrustedKeys { get; set; } = new HashSet<TrustedKey>();

        public ObservableCollection<Tab> Tabs { get; set; } = Tab.Defaults();

        public bool AlwaysOnTop { get; set; }

        public double FontSize { get; set; } = 14d;

        public ushort BacklogMessages { get; set; } = 500;

        public uint LocalBacklogMessages { get; set; } = 10_000;

        public double Opacity { get; set; } = 1.0;

        public bool CompactMode { get; set; }

        public Theme Theme { get; set; } = Theme.System;

        public ObservableCollection<Notification> Notifications { get; set; } = new ObservableCollection<Notification>();

        #region io

        private static string FilePath() => Path.Join(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "XIVChat for Windows",
            "config.json"
        );

        public static Configuration? Load() {
            var path = FilePath();
            if (!File.Exists(path)) {
                return null;
            }

            using var reader = File.OpenText(path);
            using var json = new JsonTextReader(reader);

            var serializer = new JsonSerializer {
                TypeNameHandling = TypeNameHandling.Auto,
                ObjectCreationHandling = ObjectCreationHandling.Replace,
            };
            return serializer.Deserialize<Configuration>(json);
        }

        public void Save() {
            var path = FilePath();
            if (!File.Exists(path)) {
                var dir = Path.GetDirectoryName(path);
                Directory.CreateDirectory(dir);
            }

            using var file = File.CreateText(path);
            using var json = new JsonTextWriter(file);

            var serialiser = new JsonSerializer {
                TypeNameHandling = TypeNameHandling.Auto,
            };
            serialiser.Serialize(json, this);
        }

        #endregion
    }

    public abstract class SavedServer : INotifyPropertyChanged {
        public event PropertyChangedEventHandler? PropertyChanged;

        public string Name { get; set; }
    }

    [JsonObject]
    public class DirectServer : SavedServer {
        public string Host { get; set; }

        public ushort Port { get; set; }

        public DirectServer(string name, string host, ushort port) {
            this.Name = name;
            this.Host = host;
            this.Port = port;
        }

        protected bool Equals(DirectServer other) {
            return this.Name == other.Name && this.Host == other.Host && this.Port == other.Port;
        }

        public override bool Equals(object? obj) {
            if (ReferenceEquals(null, obj)) {
                return false;
            }

            if (ReferenceEquals(this, obj)) {
                return true;
            }

            return obj.GetType() == this.GetType() && this.Equals((DirectServer) obj);
        }

        public override int GetHashCode() {
            unchecked {
                return (this.Host.GetHashCode() * 397) ^ this.Port.GetHashCode();
            }
        }
    }

    [JsonObject]
    public class RelayServer : SavedServer {
        public string RelayAuth { get; set; }
        public string RelayTarget { get; set; }

        public RelayServer(string name, string relayAuth, string relayTarget) {
            this.Name = name;
            this.RelayAuth = relayAuth;
            this.RelayTarget = relayTarget;
        }

        protected bool Equals(RelayServer other) {
            return this.Name == other.Name && this.RelayAuth == other.RelayAuth && this.RelayTarget == other.RelayTarget;
        }

        public override bool Equals(object? obj) {
            if (ReferenceEquals(null, obj)) {
                return false;
            }

            if (ReferenceEquals(this, obj)) {
                return true;
            }

            return obj.GetType() == this.GetType() && this.Equals((RelayServer) obj);
        }

        public override int GetHashCode() {
            unchecked {
                return (this.RelayAuth.GetHashCode() * 397) ^ this.RelayTarget.GetHashCode();
            }
        }
    }

    public enum Theme {
        System,
        Light,
        Dark,
    }

    [JsonObject]
    public class TrustedKey {
        public string Name { get; set; }
        public byte[] Key { get; set; }

        public TrustedKey(string name, byte[] key) {
            this.Name = name;
            this.Key = key;
        }
    }

    [JsonObject]
    public class Tab : IEnumerable<ServerMessage>, INotifyCollectionChanged, INotifyPropertyChanged {
        public string Name { get; set; }

        public Filter Filter { get; set; } = new Filter();

        public bool ProcessMarkdown { get; set; }

        [JsonIgnore]
        public List<ServerMessage> Messages { get; } = new List<ServerMessage>();

        public Tab(string name) {
            this.Name = name;
        }

        private void NotifyReset() {
            this.CollectionChanged?.Invoke(this, new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Reset));
        }

        private void NotifyAdd(ServerMessage message) {
            this.CollectionChanged?.Invoke(this, new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Add, message));
        }

        private void NotifyAddItemsAt(IList messages, int index) {
            this.CollectionChanged?.Invoke(this, new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Add, messages, index));
        }

        private void NotifyRemoveItemsAt(IList messages, int index) {
            this.CollectionChanged?.Invoke(this, new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Remove, messages, index));
        }

        public void RepopulateMessages(IEnumerable<ServerMessage> mainMessages) {
            this.Messages.Clear();

            // add messages from newest to oldest
            foreach (var message in mainMessages.Where(msg => this.Filter.Allowed(msg))) {
                this.Messages.Add(message);
            }

            this.NotifyReset();
        }

        private int _lastSequence = -1;
        private int _insertAt;

        public void AddReversedChunk(ServerMessage[] messages, int sequence, Configuration config) {
            if (sequence != this._lastSequence) {
                this._lastSequence = sequence;
                this._insertAt = this.Messages.Count;
            }

            var filtered = messages
                .Where(msg => msg.Channel == 0 || this.Filter.Allowed(msg))
                .ToList();

            this.Messages.InsertRange(this._insertAt, filtered);
            this.NotifyAddItemsAt(filtered, this._insertAt);

            this.Prune(config);
        }

        public void AddMessage(ServerMessage message, Configuration config) {
            if (message.Channel != 0 && !this.Filter.Allowed(message)) {
                return;
            }

            this.Messages.Add(message);
            this.NotifyAdd(message);

            this.Prune(config);
        }

        private void Prune(Configuration config) {
            var diff = this.Messages.Count - config.LocalBacklogMessages;
            if (diff <= 0) {
                return;
            }

            var removed = this.Messages.Take((int) diff).ToList();
            this.Messages.RemoveRange(0, (int) diff);
            this.NotifyRemoveItemsAt(removed, 0);
        }

        public void ClearMessages() {
            this.Messages.Clear();
            this.NotifyReset();
        }

        public static Filter GeneralFilter() {
            var generalFilters = FilterCategory.Chat.Types()
                .Concat(FilterCategory.Announcements.Types())
                .ToHashSet();
            generalFilters.Remove(FilterType.OwnBattleSystem);
            generalFilters.Remove(FilterType.OthersBattleSystem);
            generalFilters.Remove(FilterType.NpcDialogue);
            generalFilters.Remove(FilterType.OthersFishing);
            return new Filter {
                Types = generalFilters,
            };
        }

        public static ObservableCollection<Tab> Defaults() {
            var battleFilters = FilterCategory.Battle.Types()
                .Append(FilterType.OwnBattleSystem)
                .Append(FilterType.OthersBattleSystem)
                .ToHashSet();

            return new ObservableCollection<Tab> {
                new Tab("General") {
                    Filter = GeneralFilter(),
                },
                new Tab("Battle") {
                    Filter = new Filter {
                        Types = battleFilters,
                    },
                },
            };
        }

        public IEnumerator<ServerMessage> GetEnumerator() {
            return this.Messages.GetEnumerator();
        }

        IEnumerator IEnumerable.GetEnumerator() {
            return this.GetEnumerator();
        }

        public event NotifyCollectionChangedEventHandler? CollectionChanged;
        public event PropertyChangedEventHandler? PropertyChanged;
    }

    [JsonObject]
    public class Filter {
        public HashSet<FilterType> Types { get; set; } = new HashSet<FilterType>();

        public virtual bool Allowed(ServerMessage message) {
            var code = new ChatCode((ushort) message.Channel);
            return this.Types.Any(type => type.Allowed(code));
        }
    }

    [JsonObject]
    public class Notification {
        public string Name { get; set; }
        public bool MatchAll { get; set; }
        public List<ChatType> Channels { get; set; } = new List<ChatType>();
        public List<string> Substrings { get; set; } = new List<string>();

        private IReadOnlyCollection<string> regexes = new List<string>();

        public IReadOnlyCollection<string> Regexes {
            get => this.regexes;
            set {
                this.regexes = value;
                this.ResetRegexes();
            }
        }

        [JsonIgnore]
        public Lazy<List<Regex>> ParsedRegexes { get; private set; } = null!;

        public Notification(string name) {
            this.Name = name;
            this.ResetRegexes();
        }

        private void ResetRegexes() {
            this.ParsedRegexes = new Lazy<List<Regex>>(
                () => {
                    try {
                        return this.ParseRegexes();
                    } catch (ArgumentException) {
                        return new List<Regex>();
                    }
                }
            );
        }

        private List<Regex> ParseRegexes() {
            return this.Regexes
                .Select(regex => new Regex(regex, RegexOptions.Compiled))
                .ToList();
        }

        [SuppressMessage("ReSharper", "ConvertIfStatementToReturnStatement")]
        public bool Matches(ServerMessage message) {
            if (!this.Channels.Contains(message.Channel)) {
                return false;
            }

            if (this.MatchAll) {
                return true;
            }

            if (this.Substrings.Count == 0 && this.Regexes.Count == 0) {
                return false;
            }

            var text = message.ContentText;

            if (this.Substrings.Any(substring => text.ContainsIgnoreCase(substring))) {
                return true;
            }

            if (this.ParsedRegexes.Value.Any(regex => regex.IsMatch(text))) {
                return true;
            }

            return false;
        }
    }
}
