using System;
using System.Windows;
using System.Windows.Controls;

namespace XIVChat_Desktop {
    /// <summary>
    /// Interaction logic for ManageServer.xaml
    /// </summary>
    public partial class ManageServer {
        public App App => (App) Application.Current;
        public SavedServer? Server { get; private set; }

        private bool IsDirect => this.TypeBox.SelectedIndex == 0;

        private readonly bool _isNewServer;

        public ManageServer(Window owner, SavedServer? server) {
            this.Owner = owner;
            this.Server = server;
            this._isNewServer = server == null;

            this.InitializeComponent();
            this.DataContext = this;

            if (this._isNewServer) {
                this.Title = "Add server";
            }
        }

        public ManageServer(Window owner, SavedServer server, bool isNewServer) {
            this.Owner = owner;
            this.Server = server;
            this._isNewServer = isNewServer;

            this.InitializeComponent();
            this.DataContext = this;
        }

        private void Save_Click(object sender, RoutedEventArgs e) {
            var serverName = this.ServerName.Text;

            if (serverName.Length == 0) {
                MessageBox.Show("Server must have a name.");
                return;
            }

            if (this.IsDirect) {
                var serverHost = this.ServerHost.Text.Trim();

                if (serverHost.Length == 0) {
                    MessageBox.Show("Server must have a host.");
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

                if (this._isNewServer) {
                    this.Server = new DirectServer(serverName, serverHost, port);
                    this.App.Config.Servers.Add(this.Server);
                } else if (this.Server is DirectServer direct) {
                    this.Server!.Name = serverName;
                    direct.Host = serverHost;
                    direct.Port = port;
                }
            } else {
                var relayAuth = this.RelayAuth.Text.Trim();
                var relayTarget = this.RelayTarget.Text.Trim();

                if (relayAuth.Length == 0 || relayTarget.Length == 0) {
                    MessageBox.Show("Server must have an auth code and public key.");
                    return;
                }

                if (this._isNewServer) {
                    this.Server = new RelayServer(serverName, relayAuth, relayTarget);
                    this.App.Config.Servers.Add(this.Server);
                } else if (this.Server is RelayServer relay) {
                    this.Server!.Name = serverName;
                    relay.RelayAuth = relayAuth;
                    relay.RelayTarget = relayTarget;
                }
            }

            this.App.Config.Save();

            this.Close();
        }

        private void Cancel_Click(object sender, RoutedEventArgs e) {
            this.Close();
        }

        private void TypeBox_OnSelectionChanged(object sender, SelectionChangedEventArgs e) {
            this.CalcVisibility();
        }

        private void CalcVisibility() {
            try {
                if (this.IsDirect) {
                    this.ServerHostLabel.Visibility = Visibility.Visible;
                    this.ServerHost.Visibility = Visibility.Visible;
                    this.ServerPortLabel.Visibility = Visibility.Visible;
                    this.ServerPort.Visibility = Visibility.Visible;
                    this.RelayAuthLabel.Visibility = Visibility.Collapsed;
                    this.RelayAuth.Visibility = Visibility.Collapsed;
                    this.RelayTargetLabel.Visibility = Visibility.Collapsed;
                    this.RelayTarget.Visibility = Visibility.Collapsed;
                } else {
                    this.ServerHostLabel.Visibility = Visibility.Collapsed;
                    this.ServerHost.Visibility = Visibility.Collapsed;
                    this.ServerPortLabel.Visibility = Visibility.Collapsed;
                    this.ServerPort.Visibility = Visibility.Collapsed;
                    this.RelayAuthLabel.Visibility = Visibility.Visible;
                    this.RelayAuth.Visibility = Visibility.Visible;
                    this.RelayTargetLabel.Visibility = Visibility.Visible;
                    this.RelayTarget.Visibility = Visibility.Visible;
                }
            } catch (NullReferenceException) {
                // ignored
            }
        }

        private void ManageServer_OnInitialized(object? sender, EventArgs e) {
            this.TypeBox.SelectedIndex = this.Server switch {
                RelayServer _ => 1,
                DirectServer _ => 0,
                _ => this.TypeBox.SelectedIndex,
            };

            if (this.Server != null) {
                this.TypeBox.IsEnabled = false;
            }

            this.CalcVisibility();
        }
    }
}
