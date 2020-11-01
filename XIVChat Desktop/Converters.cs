using System;
using System.Globalization;
using System.Windows.Data;

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
}
