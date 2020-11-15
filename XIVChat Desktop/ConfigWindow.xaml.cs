using System;
using System.Linq;
using System.Windows;
using System.Windows.Input;

namespace XIVChat_Desktop {
    /// <summary>
    /// Interaction logic for ConfigWindow.xaml
    /// </summary>
    public partial class ConfigWindow {
        public Configuration Config { get; private set; }

        public ConfigWindow(Window owner, Configuration config) {
            this.Owner = owner;
            this.Config = config;

            this.InitializeComponent();
            this.DataContext = this;

            this.ThemeChooser.ItemsSource = (Theme[])Enum.GetValues(typeof(Theme));
        }

        private void AlwaysOnTop_Checked(object? sender, RoutedEventArgs e) {
            this.SetAlwaysOnTop(true);
        }

        private void AlwaysOnTop_Unchecked(object? sender, RoutedEventArgs e) {
            this.SetAlwaysOnTop(false);
        }

        private void SetAlwaysOnTop(bool onTop) {
            this.Owner.Topmost = onTop;
            this.Config.AlwaysOnTop = onTop;
        }

        private void Save_Click(object? sender, RoutedEventArgs e) {
            this.Config.Save();
        }

        private void SavedServers_ItemDoubleClick(SavedServer? server) {
            new ManageServer(this, server).ShowDialog();
        }

        private void ConfigWindow_OnContentRendered(object? sender, EventArgs e) {
            this.InvalidateVisual();
        }

        private void NumericInputFilter(object sender, TextCompositionEventArgs e) {
            var allDigits = e.Text.All(c => char.IsDigit(c));
            e.Handled = !allDigits;
        }
    }
}
