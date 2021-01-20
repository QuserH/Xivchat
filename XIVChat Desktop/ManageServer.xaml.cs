using System.Windows;

namespace XIVChat_Desktop {
    /// <summary>
    /// Interaction logic for ManageServer.xaml
    /// </summary>
    public partial class ManageServer {
        public App App => (App) Application.Current;
        public SavedServer? Server { get; private set; }

        private readonly bool isNewServer;

        public ManageServer(Window owner, SavedServer? server) {
            this.Owner = owner;
            this.Server = server;
            this.isNewServer = server == null;

            this.InitializeComponent();
            this.DataContext = this;

            if (this.isNewServer) {
                this.Title = "Add server";
            }
        }

        public ManageServer(Window owner, SavedServer server, bool isNewServer) {
            this.Owner = owner;
            this.Server = server;
            this.isNewServer = isNewServer;

            this.InitializeComponent();
            this.DataContext = this;
        }

        private void Save_Click(object sender, RoutedEventArgs e) {
            var serverName = this.ServerName.Text;
            var serverHost = this.ServerHost.Text;
            var relayAuth = this.RelayAuth.Text.Trim();
            var relayTarget = this.RelayTarget.Text.Trim();

            if (relayAuth.Length == 0) {
                relayAuth = null;
            }

            if (relayTarget.Length == 0) {
                relayTarget = null;
            }

            if (serverName.Length == 0 || serverHost.Length == 0) {
                MessageBox.Show("Server must have a name and host.");
                return;
            }

            ushort port;
            if (this.ServerPort.Text.Length == 0) {
                port = 14777;
            } else {
                if (!ushort.TryParse(this.ServerPort.Text, out port) || port < 1) {
                    MessageBox.Show("Port was not valid. It must be a number between 1 and 65535.");
                    return;
                }
            }

            if (this.isNewServer) {
                this.Server = new SavedServer(
                    serverName,
                    serverHost,
                    port,
                    relayAuth,
                    relayTarget
                );

                this.App.Config.Servers.Add(this.Server);
            } else {
                this.Server!.Name = serverName;
                this.Server.Host = serverHost;
                this.Server.Port = port;
                this.Server.RelayAuth = relayAuth;
                this.Server.RelayTarget = relayTarget;
            }

            this.App.Config.Save();

            this.Close();
        }

        private void Cancel_Click(object sender, RoutedEventArgs e) {
            this.Close();
        }
    }
}
