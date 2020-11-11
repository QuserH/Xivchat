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
using XIVChatCommon;

namespace XIVChat_Desktop {
    [JsonObject]
    public class Configuration : INotifyPropertyChanged {
        public event PropertyChangedEventHandler? PropertyChanged;

        public KeyPair KeyPair { get; set; } = PublicKeyBox.GenerateKeyPair();

        public ObservableCollection<SavedServer> Servers { get; set; } = new ObservableCollection<SavedServer>();
        public HashSet<TrustedKey> TrustedKeys { get; set; } = new HashSet<TrustedKey>();

        public ObservableCollection<Tab> Tabs { get; set; } = Tab.Defaults();

        public bool AlwaysOnTop { get; set; }

        private double fontSize = 14d;

        public double FontSize {
            get => this.fontSize;
            set {
                this.fontSize = value;
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(nameof(this.FontSize)));
            }
        }

        public ushort BacklogMessages { get; set; } = 500;

        public uint LocalBacklogMessages { get; set; } = 10_000;

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

            var serialiser = new JsonSerializer();
            serialiser.Serialize(json, this);
        }

        #endregion
    }

    [JsonObject]
    public class SavedServer {
        public string Name { get; set; }
        public string Host { get; set; }
        public ushort Port { get; set; }

        public SavedServer(string name, string host, ushort port) {
            this.Name = name;
            this.Host = host;
            this.Port = port;
        }

        protected bool Equals(SavedServer other) {
            return this.Name == other.Name && this.Host == other.Host && this.Port == other.Port;
        }

        public override bool Equals(object? obj) {
            if (obj is null) {
                return false;
            }

            if (ReferenceEquals(this, obj)) {
                return true;
            }

            return obj.GetType() == this.GetType() && this.Equals((SavedServer)obj);
        }

        [SuppressMessage("ReSharper", "NonReadonlyMemberInGetHashCode")]
        public override int GetHashCode() {
            return HashCode.Combine(this.Name, this.Host, this.Port);
        }
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
    public class Tab : IEnumerable<ServerMessage>, INotifyCollectionChanged {
        private string name;

        public Tab(string name) {
            this.name = name;
        }

        public string Name {
            get => this.name;
            set {
                this.name = value;
            }
        }

        public Filter Filter { get; set; } = new Filter();

        public bool ProcessMarkdown { get; set; }

        [JsonIgnore]
        public List<ServerMessage> Messages { get; } = new List<ServerMessage>();

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

        private int lastSequence = -1;
        private int insertAt;

        public void AddReversedChunk(ServerMessage[] messages, int sequence, Configuration config) {
            if (sequence != this.lastSequence) {
                this.lastSequence = sequence;
                this.insertAt = this.Messages.Count;
            }

            var filtered = messages
                .Where(msg => msg.Channel == 0 || this.Filter.Allowed(msg))
                .ToList();

            this.Messages.InsertRange(this.insertAt, filtered);
            this.NotifyAddItemsAt(filtered, this.insertAt);

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

            var removed = this.Messages.Take((int)diff).ToList();
            this.Messages.RemoveRange(0, (int)diff);
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
    }

    [JsonObject]
    public class Filter {
        public HashSet<FilterType> Types { get; set; } = new HashSet<FilterType>();

        public virtual bool Allowed(ServerMessage message) {
            var code = new ChatCode((ushort)message.Channel);
            return this.Types.Any(type => type.Allowed(code));
        }
    }
}
