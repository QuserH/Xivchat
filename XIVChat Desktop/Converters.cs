using System;
using System.Globalization;
using System.Text;
using System.Windows.Data;
using XIVChatCommon;

namespace XIVChat_Desktop {
    public class DoubleConverter : IValueConverter {
        public object? Convert(object value, Type targetType, object parameter, CultureInfo culture) {
            return value.ToString();
        }

        public object? ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) {
            if (double.TryParse(value.ToString(), out var res)) {
                return res;
            }

            return null;
        }
    }

    public class UShortConverter : IValueConverter {
        public object? Convert(object value, Type targetType, object parameter, CultureInfo culture) {
            return value.ToString();
        }

        public object? ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) {
            if (ushort.TryParse(value.ToString(), out var res)) {
                return res;
            }

            return null;
        }
    }

    public class UIntConverter : IValueConverter {
        public object? Convert(object value, Type targetType, object parameter, CultureInfo culture) {
            return value.ToString();
        }

        public object? ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) {
            if (uint.TryParse(value.ToString(), out var res)) {
                return res;
            }

            return null;
        }
    }

    public class SenderPlayerConverter : IValueConverter {
        public object? Convert(object value, Type targetType, object parameter, CultureInfo culture) {
            if (!(value is ServerMessage.SenderPlayer sender)) {
                return null;
            }

            var s = new StringBuilder();

            s.Append(sender.Name);

            var worldName = Util.WorldName(sender.Server);
            if (worldName != null) {
                s.Append(" (");
                s.Append(worldName);
                s.Append(")");
            }

            return s.ToString();
        }

        public object? ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) {
            throw new NotImplementedException();
        }
    }
}
