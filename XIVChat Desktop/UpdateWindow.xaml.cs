using System.Net.Http;
using System.Threading.Tasks;
using System.Windows;
using AutoUpdaterDotNET;

namespace XIVChat_Desktop {
    public partial class UpdateWindow {
        private App App => (App) Application.Current;

        private UpdateInfoEventArgs Info { get; }

        public UpdateWindow(Window owner, UpdateInfoEventArgs info) {
            this.Owner = owner;
            this.Info = info;

            this.InitializeComponent();
            this.DataContext = this;

            if (info.ChangelogURL != null) {
                Task.Run(async () => {
                    using var client = new HttpClient();
                    try {
                        var changelog = await client.GetStringAsync(info.ChangelogURL);
                        this.Dispatch(() => this.Changelog.Text = changelog);
                    } catch (HttpRequestException ex) {
                        this.Dispatch(() => this.Changelog.Text = $"Error fetching changelog:\n{ex.Message}\n\n{info.ChangelogURL}");
                    }
                });
            }

            var currentVersion = typeof(App).Assembly.GetName().Version?.ToString(3) ?? "<UNKNOWN>";

            this.UpdateInfo.Text = $"A new update is available. You are currently running version {currentVersion}. Version {info.CurrentVersion} is available. Would you like to update?";
        }

        private void Close_Click(object sender, RoutedEventArgs e) {
            this.Close();
        }

        private void Update_Click(object sender, RoutedEventArgs e) {
            if (!AutoUpdater.DownloadUpdate(this.Info)) {
                return;
            }

            this.App.Shutdown();
        }
    }
}
