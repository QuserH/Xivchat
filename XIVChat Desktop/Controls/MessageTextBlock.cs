using System.Windows;
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

        public static readonly DependencyProperty ProcessMarkdownProperty = DependencyProperty.Register(
            "ProcessMarkdown",
            typeof(bool),
            typeof(MessageTextBlock),
            new PropertyMetadata(false, PropertyChanged)
        );

        public bool ProcessMarkdown {
            get => (bool)this.GetValue(ProcessMarkdownProperty);
            set => this.SetValue(ProcessMarkdownProperty, value);
        }

        public static readonly DependencyProperty ShowTimestampsProperty = DependencyProperty.Register(
            "ShowTimestamps",
            typeof(bool),
            typeof(MessageTextBlock),
            new PropertyMetadata(true, PropertyChanged)
        );

        public bool ShowTimestamps {
            get => (bool)this.GetValue(ShowTimestampsProperty);
            set => this.SetValue(ShowTimestampsProperty, value);
        }

        private static void PropertyChanged(DependencyObject d, DependencyPropertyChangedEventArgs e) {
            // Clear current textBlock
            if (!(d is MessageTextBlock textBlock)) {
                return;
            }

            var message = textBlock.Message;

            if (message == null) {
                return;
            }

            textBlock.ClearValue(TextProperty);
            textBlock.Inlines.Clear();

            // Create new formatted text
            var lineHeight = textBlock.FontFamily.LineSpacing * textBlock.FontSize;
            var inlines = MessageFormatter.ChunksToTextBlock(
                message,
                lineHeight,
                textBlock.ProcessMarkdown,
                textBlock.ShowTimestamps
            );
            foreach (var inline in inlines) {
                textBlock.Inlines.Add(inline);
            }
        }
    }
}
