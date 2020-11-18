using System;
using System.ComponentModel;
using System.Globalization;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Markup;
using ModernWpf;

// TODO: key word notification, notifications on message type, targeted message (like emote targeting you)
// TODO: right click message to send tell to sender?

namespace XIVChat_Desktop {
    /// <summary>
    /// Interaction logic for App.xaml
    /// </summary>
    public partial class App : INotifyPropertyChanged {
        public MainWindow Window { get; private set; } = null!;
        public Configuration Config { get; private set; } = null!;

        public string? LastHost { get; set; }

        private Connection? connection;

        public Connection? Connection {
            get => this.connection;
            set {
                this.connection = value;
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(nameof(this.Connection)));
                this.ConnectionStatusChanged();
            }
        }

        public bool Connected => this.Connection != null;

        public event PropertyChangedEventHandler? PropertyChanged;

        private async void Application_Startup(object sender, StartupEventArgs e) {
            try {
                this.Config = Configuration.Load() ?? new Configuration();
            } catch (Exception ex) {
                var result = MessageBox.Show(
                    $"Could not load the configuration file: {ex.Message}. Do you want to create a new configuration file and overwrite the old one?",
                    "Error loading config",
                    MessageBoxButton.YesNo
                );

                if (result == MessageBoxResult.Yes) {
                    this.Config = new Configuration();
                } else {
                    this.Shutdown(1);
                    return;
                }
            }

            try {
                this.Config.Save();
            } catch (Exception ex) {
                MessageBox.Show($"Could not save configuration file. {ex.Message}");
            }

            this.Config.PropertyChanged += (o, args) => {
                if (args.PropertyName != nameof(Configuration.Theme)) {
                    return;
                }

                this.UpdateTheme();
            };

            this.UpdateTheme();

            FrameworkElement.LanguageProperty.OverrideMetadata(
                typeof(FrameworkElement),
                new FrameworkPropertyMetadata(
                    XmlLanguage.GetLanguage(CultureInfo.CurrentCulture.IetfLanguageTag)
                )
            );

            // I guess this gets initialised where you call it the first time, so initialise it on the UI thread
            this.Dispatcher.Invoke(() => { });

            #if RELEASE
            if (string.IsNullOrWhiteSpace(this.Config.LicenceKey) || !(await LicenceWindow.LicenceInfo(this.Config.LicenceKey)).Valid()) {
                var lic = new LicenceWindow(null, true);
                lic.Show();
                return;
            }
            #endif

            this.InitialiseWindow();
        }

        public void InitialiseWindow() {
            var wnd = new MainWindow();
            this.Window = wnd;

            wnd.Show();

            // initialise a config window to apply all our settings
            _ = new ConfigWindow(wnd, this.Config);
        }

        private void UpdateTheme() {
            ThemeManager.Current.ApplicationTheme = this.Config.Theme switch {
                Theme.System => null,
                Theme.Dark => ApplicationTheme.Dark,
                Theme.Light => ApplicationTheme.Light,
                _ => null,
            };
        }

        private void ConnectionStatusChanged() {
            this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(nameof(this.Connected)));
        }

        public void Connect(string host, ushort port) {
            if (this.Connected) {
                return;
            }

            this.Connection = new Connection(this, host, port);
            Task.Run(this.Connection.Connect);
        }

        public void Disconnect() {
            if (!this.Connected) {
                return;
            }

            this.Connection?.Disconnect();
            this.Connection = null;
        }
    }
}
