using Newtonsoft.Json;
using Sodium;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
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
    public class Tab : INotifyPropertyChanged {
        private string name;

        public Tab(string name) {
            this.name = name;
        }

        public string Name {
            get => this.name;
            set {
                this.name = value;
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(nameof(this.Name)));
            }
        }

        public Filter Filter { get; set; } = new Filter();

        [JsonIgnore]
        public ObservableCollection<ServerMessage> Messages { get; } = new ObservableCollection<ServerMessage>();

        public event PropertyChangedEventHandler? PropertyChanged;

        public void RepopulateMessages(ConcurrentStack<ServerMessage> mainMessages) {
            this.Messages.Clear();

            foreach (var message in mainMessages.Where(msg => this.Filter.Allowed(msg)).Reverse()) {
                this.Messages.Add(message);
            }

            this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(nameof(this.Messages)));
        }

        public void AddMessage(ServerMessage message) {
            if (message.Channel != 0 && !this.Filter.Allowed(message)) {
                return;
            }

            this.Messages.Add(message);

            this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(nameof(this.Messages)));
        }

        public void ClearMessages() {
            this.Messages.Clear();
            this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(nameof(this.Messages)));
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
    }

    [JsonObject]
    public class Filter {
        public HashSet<FilterType> Types { get; set; } = new HashSet<FilterType>();

        public bool Allowed(ServerMessage message) {
            return this.Types
                .SelectMany(type => type.Types())
                .Contains(new ChatCode((ushort)message.Channel).Type);
        }
    }
}
