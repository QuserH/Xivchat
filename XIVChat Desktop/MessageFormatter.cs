using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using MdXaml;
using XIVChatCommon.Message;
using XIVChatCommon.Message.Server;

namespace XIVChat_Desktop {
    public static class MessageFormatter {
        public enum CrossWorldIconMode {
            Icon,
            Text,
        }

        public class Options {
            public double LineHeight { get; set; }
            public bool ProcessMarkdown { get; set; }
            public bool ShowTimestamp { get; set; }
            public CrossWorldIconMode CrossWorldIconMode { get; set; } = CrossWorldIconMode.Icon;
        }

        private static readonly BitmapFrame FontIcon = BitmapFrame.Create(new Uri("pack://application:,,,/Resources/fonticon_ps4.tex.png"));

        private static readonly Markdown Markdown = new() {
            HyperlinkCommand = MainWindow.OpenLink,
        };

        public static IEnumerable<Inline> ChunksToTextBlock(ServerMessage message, Options options) {
            var elements = new List<Inline>();

            if (options.ShowTimestamp) {
                var timestampString = message.Timestamp.ToLocalTime().ToString("t", CultureInfo.CurrentUICulture);
                elements.Add(new Run($"[{timestampString}]") {
                    Foreground = new SolidColorBrush(Colors.White),
                });
            }

            foreach (var chunk in message.Chunks) {
                switch (chunk) {
                    case TextChunk textChunk:
                        var colour = textChunk.Foreground ?? textChunk.FallbackColour ?? 0;

                        var r = (byte) ((colour >> 24) & 0xFF);
                        var g = (byte) ((colour >> 16) & 0xFF);
                        var b = (byte) ((colour >> 8) & 0xFF);
                        var a = (byte) (colour & 0xFF);

                        var brush = new SolidColorBrush(Color.FromArgb(a, r, g, b));
                        var style = textChunk.Italic ? FontStyles.Italic : FontStyles.Normal;

                        if (options.ProcessMarkdown) {
                            var inlines = Markdown.RunSpanGamut(textChunk.Content);

                            foreach (var inline in inlines) {
                                inline.Foreground = brush;
                                if (inline.FontStyle == FontStyles.Normal) {
                                    inline.FontStyle = style;
                                }

                                elements.Add(inline);
                            }
                        } else {
                            elements.Add(new Run(textChunk.Content) {
                                Foreground = brush,
                                FontStyle = style,
                            });
                        }

                        break;
                    case IconChunk iconChunk:
                        if (options.CrossWorldIconMode == CrossWorldIconMode.Text && iconChunk.index == 88) {
                            // find the last non-timestamp run
                            var lastRun = elements.FindLast(elem => (!options.ShowTimestamp || elements.Count > 1) && elem is Run);
                            var foreground = lastRun?.Foreground;
                            var fontStyle = lastRun?.FontStyle ?? FontStyles.Normal;

                            elements.Add(new Run("@") {
                                Foreground = foreground,
                                FontStyle = fontStyle,
                            });

                            break;
                        }

                        var bounds = GetBounds(iconChunk.index);
                        if (bounds == null) {
                            break;
                        }

                        var width = options.LineHeight / bounds.Value.Height * bounds.Value.Width;

                        var cropped = new CroppedBitmap(FontIcon, bounds.Value);
                        var image = new Image {
                            Source = cropped,
                            Width = width,
                            Height = options.LineHeight,
                        };
                        elements.Add(new InlineUIContainer(image) {
                            BaselineAlignment = BaselineAlignment.Bottom,
                        });
                        break;
                }
            }

            // add a trailing empty run if there's a hyperlink
            // this works around a crash
            if (elements.Any(elem => elem is Hyperlink)) {
                elements.Add(new Run(""));
            }

            return elements;
        }

        private static Int32Rect? GetBounds(byte id) => id switch {
            1 => new Int32Rect(0, 342, 40, 40),
            2 => new Int32Rect(40, 342, 40, 40),
            3 => new Int32Rect(80, 342, 40, 40),
            4 => new Int32Rect(120, 342, 40, 40),
            5 => new Int32Rect(160, 342, 40, 40),
            6 => new Int32Rect(0, 382, 40, 40),
            7 => new Int32Rect(40, 382, 40, 40),
            8 => new Int32Rect(80, 382, 40, 40),
            9 => new Int32Rect(120, 382, 40, 40),
            10 => new Int32Rect(160, 382, 40, 40),
            11 => new Int32Rect(0, 422, 40, 40),
            12 => new Int32Rect(40, 422, 40, 40),
            13 => new Int32Rect(80, 422, 40, 40),
            14 => new Int32Rect(120, 422, 40, 40),
            15 => new Int32Rect(160, 422, 40, 40),
            16 => new Int32Rect(120, 542, 40, 40),
            17 => new Int32Rect(160, 542, 40, 40),
            18 => new Int32Rect(0, 462, 108, 40),
            19 => new Int32Rect(108, 462, 108, 40),
            20 => new Int32Rect(120, 502, 40, 40),
            21 => new Int32Rect(0, 502, 56, 40),
            22 => new Int32Rect(56, 502, 64, 40),
            23 => new Int32Rect(160, 502, 40, 40),
            24 => new Int32Rect(0, 542, 56, 40),
            25 => new Int32Rect(56, 542, 64, 40),
            51 => new Int32Rect(248, 342, 40, 40),
            52 => new Int32Rect(288, 342, 40, 40),
            53 => new Int32Rect(328, 342, 40, 40),
            54 => new Int32Rect(200, 342, 24, 40),
            55 => new Int32Rect(224, 342, 24, 40),
            56 => new Int32Rect(200, 382, 40, 40),
            57 => new Int32Rect(240, 382, 40, 40),
            58 => new Int32Rect(280, 382, 40, 40),
            59 => new Int32Rect(200, 422, 40, 40),
            60 => new Int32Rect(240, 422, 40, 40),
            61 => new Int32Rect(280, 422, 40, 40),
            62 => new Int32Rect(320, 382, 40, 40),
            63 => new Int32Rect(320, 422, 40, 40),
            64 => new Int32Rect(368, 342, 40, 40),
            65 => new Int32Rect(408, 342, 40, 40),
            66 => new Int32Rect(448, 342, 40, 40),
            67 => new Int32Rect(360, 382, 40, 40),
            68 => new Int32Rect(400, 382, 40, 40),
            70 => new Int32Rect(360, 422, 40, 40),
            71 => new Int32Rect(400, 422, 40, 40),
            72 => new Int32Rect(440, 422, 40, 40),
            73 => new Int32Rect(440, 382, 40, 40),
            74 => new Int32Rect(216, 462, 40, 40),
            75 => new Int32Rect(256, 462, 40, 40),
            76 => new Int32Rect(296, 462, 40, 40),
            77 => new Int32Rect(336, 462, 40, 40),
            78 => new Int32Rect(376, 462, 40, 40),
            79 => new Int32Rect(416, 462, 40, 40),
            80 => new Int32Rect(456, 462, 40, 40),
            81 => new Int32Rect(200, 502, 40, 40),
            82 => new Int32Rect(240, 502, 40, 40),
            83 => new Int32Rect(280, 502, 40, 40),
            84 => new Int32Rect(320, 502, 40, 40),
            85 => new Int32Rect(360, 502, 40, 40),
            86 => new Int32Rect(400, 502, 40, 40),
            87 => new Int32Rect(440, 502, 40, 40),
            88 => new Int32Rect(200, 542, 40, 40),
            89 => new Int32Rect(240, 542, 40, 40),
            90 => new Int32Rect(280, 542, 40, 40),
            91 => new Int32Rect(320, 542, 40, 40),
            92 => new Int32Rect(360, 542, 40, 40),
            93 => new Int32Rect(400, 542, 40, 40),
            94 => new Int32Rect(440, 542, 40, 40),
            95 => new Int32Rect(0, 582, 40, 40),
            96 => new Int32Rect(40, 582, 40, 40),
            97 => new Int32Rect(80, 582, 40, 40),
            98 => new Int32Rect(120, 582, 40, 40),
            99 => new Int32Rect(160, 582, 40, 40),
            100 => new Int32Rect(200, 582, 40, 40),
            101 => new Int32Rect(240, 582, 40, 40),
            102 => new Int32Rect(280, 582, 40, 40),
            103 => new Int32Rect(320, 582, 40, 40),
            104 => new Int32Rect(360, 582, 40, 40),
            105 => new Int32Rect(400, 582, 40, 40),
            106 => new Int32Rect(440, 582, 40, 40),
            107 => new Int32Rect(0, 622, 40, 40),
            108 => new Int32Rect(40, 622, 40, 40),
            109 => new Int32Rect(80, 622, 40, 40),
            110 => new Int32Rect(120, 622, 40, 40),
            111 => new Int32Rect(160, 622, 40, 40),
            112 => new Int32Rect(200, 622, 40, 40),
            _ => null,
        };
    }
}
