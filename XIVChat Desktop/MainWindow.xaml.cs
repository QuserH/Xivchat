using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using XIVChatCommon;

namespace XIVChat_Desktop {
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : INotifyPropertyChanged {
        public App App => (App)Application.Current;

        public List<ServerMessage> Messages { get; } = new List<ServerMessage>();

        public string InputPlaceholder => this.App.Connection?.Available == true ? "Send a message..." : "Chat is currently unavailable";

        public MainWindow() {
            this.InitializeComponent();
            this.DataContext = this;
        }

        private T? FindElementByName<T>(DependencyObject element, string sChildName) where T : FrameworkElement {
            T? childElement = null;
            var nChildCount = VisualTreeHelper.GetChildrenCount(element);
            for (int i = 0; i < nChildCount; i++) {
                if (!(VisualTreeHelper.GetChild(element, i) is FrameworkElement child)) {
                    continue;
                }

                if (child is T t && child.Name.Equals(sChildName)) {
                    childElement = t;
                    break;
                }

                childElement = this.FindElementByName<T>(child, sChildName);

                if (childElement != null) {
                    break;
                }
            }

            return childElement;
        }

        public void ClearAllMessages() {
            this.Messages.Clear();
            foreach (var tab in this.App.Config.Tabs) {
                tab.ClearMessages();
            }
        }

        public void AddSystemMessage(string content) {
            var message = new ServerMessage(
                DateTime.UtcNow,
                0,
                new byte[0],
                Encoding.UTF8.GetBytes(content),
                new List<Chunk> {
                    new TextChunk(content) {
                        Foreground = 0xb38cffff,
                    },
                }
            );
            this.AddMessage(message);
        }

        private int lastSequence = -1;
        private int insertAt;

        public void AddReversedChunk(ServerMessage[] messages, int sequence) {
            if (sequence != this.lastSequence) {
                this.lastSequence = sequence;
                this.insertAt = this.Messages.Count;
            }

            // detect if scroller is at the bottom
            var scroller = this.FindElementByName<ScrollViewer>(this.Tabs, "scroller");
            var wasAtBottom = Math.Abs(scroller!.VerticalOffset - scroller.ScrollableHeight) < .0001;

            // add messages to main list
            this.Messages.InsertRange(this.insertAt, messages);
            // add message to each tab if the filter allows for it
            foreach (var tab in this.App.Config.Tabs) {
                tab.AddReversedChunk(messages, sequence, this.App.Config);
            }

            var diff = this.Messages.Count - this.App.Config.LocalBacklogMessages;
            if (diff > 0) {
                this.Messages.RemoveRange(0, (int)diff);
            }

            // scroll to the bottom if previously at the bottom
            if (wasAtBottom) {
                scroller.ScrollToBottom();
            }
        }

        public void AddMessage(ServerMessage message) {
            // detect if scroller is at the bottom
            var scroller = this.FindElementByName<ScrollViewer>(this.Tabs, "scroller");
            var wasAtBottom = Math.Abs(scroller!.VerticalOffset - scroller.ScrollableHeight) < .0001;

            // add message to main list
            this.Messages.Add(message);
            // add message to each tab if the filter allows for it
            foreach (var tab in this.App.Config.Tabs) {
                tab.AddMessage(message, this.App.Config);
            }

            var diff = this.Messages.Count - this.App.Config.LocalBacklogMessages;
            if (diff > 0) {
                this.Messages.RemoveRange(0, (int)diff);
            }

            // scroll to the bottom if previously at the bottom
            if (wasAtBottom) {
                scroller.ScrollToBottom();
            }
        }

        private void Connect_Click(object sender, RoutedEventArgs e) {
            var dialog = new ConnectDialog {
                Owner = this,
            };
            dialog.ShowDialog();
        }

        private void Disconnect_Click(object sender, RoutedEventArgs e) {
            this.App.Disconnect();
        }

        private void Input_Submit(object sender, KeyEventArgs e) {
            if (e.Key != Key.Return) {
                return;
            }

            var conn = this.App.Connection;
            if (conn == null) {
                return;
            }

            if (!(sender is TextBox)) {
                return;
            }

            var textBox = (TextBox)sender;

            conn.SendMessage(textBox.Text);
            textBox.Text = "";
        }

        private void Configuration_Click(object sender, RoutedEventArgs e) {
            new ConfigWindow(this, this.App.Config).Show();
        }

        private void Tabs_Loaded(object sender, RoutedEventArgs e) {
            this.Tabs.SelectedIndex = 0;
        }

        private void Tabs_SelectionChanged(object sender, SelectionChangedEventArgs e) {
            var scroller = this.FindElementByName<ScrollViewer>(this.Tabs, "scroller");
            scroller?.ScrollToBottom();
        }

        private void ManageTabs_Click(object sender, RoutedEventArgs e) {
            new ManageTabs(this).Show();
        }

        private void Scan_Click(object sender, RoutedEventArgs e) {
            new ServerScan(this).Show();
        }

        public event PropertyChangedEventHandler? PropertyChanged;

        internal virtual void OnPropertyChanged([CallerMemberName] string? propertyName = null) {
            this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }

        private void Export_Click(object sender, RoutedEventArgs e) {
            new Export(this).Show();
        }

        private void Licence_Click(object sender, RoutedEventArgs e) {
            new LicenceWindow(this, false).Show();
        }
    }
}
