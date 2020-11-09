using System.Windows;
using System.Windows.Controls;
using XIVChatCommon;

namespace XIVChat_Desktop.Controls {
    public class MessageTextBlock : SelectableTextBlock {
        public static readonly DependencyProperty MessageProperty = DependencyProperty.Register(
            "Message",
            typeof(ServerMessage),
            typeof(MessageTextBlock),
            new PropertyMetadata(null, PropertyChanged)
        );

        public ServerMessage? Message {
            get => (ServerMessage)this.GetValue(MessageProperty);
            set => this.SetValue(MessageProperty, value);
        }

        public static readonly DependencyProperty TabProperty = DependencyProperty.Register(
            "Tab",
            typeof(Tab),
            typeof(MessageTextBlock),
            new PropertyMetadata(null, PropertyChanged)
        );

        public Tab? Tab {
            get => (Tab)this.GetValue(TabProperty);
            set => this.SetValue(TabProperty, value);
        }

        public static void PropertyChanged(DependencyObject d, DependencyPropertyChangedEventArgs e) {
            // Clear current textBlock
            if (!(d is MessageTextBlock textBlock)) {
                return;
            }

            var message = textBlock.Message;
            var tab = textBlock.Tab;

            if (message == null || tab == null) {
                return;
            }

            textBlock.ClearValue(TextBlock.TextProperty);
            textBlock.Inlines.Clear();

            // Create new formatted text
            var lineHeight = textBlock.FontFamily.LineSpacing * textBlock.FontSize;
            foreach (var inline in MessageFormatter.ChunksToTextBlock(lineHeight, message, tab)) {
                textBlock.Inlines.Add(inline);
            }
        }
    }
}
