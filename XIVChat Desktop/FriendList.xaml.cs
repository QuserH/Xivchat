using System;
using System.Collections.Specialized;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows;

namespace XIVChat_Desktop {
    public partial class FriendList : INotifyPropertyChanged {
        public App App => (App)Application.Current;

        private bool waiting;

        public bool Waiting {
            get => this.waiting;
            set {
                this.waiting = value;
                this.OnPropertyChanged(nameof(this.Waiting));
            }
        }

        public FriendList(Window owner) {
            this.Owner = owner;

            this.InitializeComponent();
            this.DataContext = this;

            this.App.Window.FriendList.CollectionChanged += this.OnFriendListChanged;
        }

        private void Refresh_Click(object sender, RoutedEventArgs e) {
            var conn = this.App.Connection;
            if (conn == null) {
                return;
            }

            this.Waiting = true;
            conn.RequestFriendList();
        }

        public event PropertyChangedEventHandler? PropertyChanged;

        private void OnPropertyChanged([CallerMemberName] string? propertyName = null) {
            this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }

        private void OnFriendListChanged(object sender, NotifyCollectionChangedEventArgs e) {
            this.Waiting = false;
        }

        private void FriendList_OnClosed(object? sender, EventArgs e) {
            this.App.Window.FriendList.CollectionChanged -= this.OnFriendListChanged;
        }
    }
}
