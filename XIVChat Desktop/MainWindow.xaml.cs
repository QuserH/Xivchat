using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
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
    public partial class MainWindow {
        public App App => (App)Application.Current;

        public ConcurrentStack<ServerMessage> Messages { get; } = new ConcurrentStack<ServerMessage>();

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

        public void AddMessage(ServerMessage message) {
            // detect if scroller is at the bottom
            var scroller = this.FindElementByName<ScrollViewer>(this.Tabs, "scroller");
            var wasAtBottom = Math.Abs(scroller!.VerticalOffset - scroller.ScrollableHeight) < .0001;

            // add message to main list
            this.Messages.Push(message);
            // add message to each tab if the filter allows for it
            foreach (var tab in this.App.Config.Tabs) {
                tab.AddMessage(message);
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
    }
}
