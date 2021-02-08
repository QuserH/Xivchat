using System;
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace XIVChat_Desktop {
    public class XivChatWindow : Window {
        // NOTE: making this protected breaks WPF
        // ReSharper disable once MemberCanBeProtected.Global
        public XivChatWindow() {
            this.Icon = new BitmapImage(new Uri("pack://application:,,,/Resources/logo.ico"));
            this.SetValue(TextOptions.TextRenderingModeProperty, TextRenderingMode.Auto);
            this.SetValue(TextOptions.TextFormattingModeProperty, TextFormattingMode.Display);
            this.SetValue(TextOptions.TextHintingModeProperty, TextHintingMode.Fixed);

            this.ContentRendered += this.FixRendering;
        }

        private void FixRendering(object? sender, EventArgs eventArgs) {
            this.InvalidateVisual();
        }
    }
}
