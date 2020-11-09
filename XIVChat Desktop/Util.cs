using System;
using System.Collections.Generic;
using System.Windows.Threading;

namespace XIVChat_Desktop {
    public static class Util {
        public static IEnumerable<List<T>> Chunks<T>(this List<T> locations, int nSize) {
            for (int i = 0; i < locations.Count; i += nSize) {
                yield return locations.GetRange(i, Math.Min(nSize, locations.Count - i));
            }
        }

        public static void Dispatch(this DispatcherObject dispatcherObj, Action action) {
            dispatcherObj.Dispatcher.BeginInvoke(action);
        }

        public static void Dispatch(this DispatcherObject dispatcherObj, DispatcherPriority priority, Action action) {
            dispatcherObj.Dispatcher.BeginInvoke(priority, action);
        }

        public static bool IsAsciiPunctuation(this char c) {
            // 2.1 Characters and lines
            // An ASCII punctuation character is !, ", #, $, %, &, ', (, ), *, +, ,, -, ., /, :, ;, <, =, >, ?, @, [, \, ], ^, _, `, {, |, }, or ~.
            switch (c) {
                case '!':
                case '"':
                case '#':
                case '$':
                case '%':
                case '&':
                case '\'':
                case '(':
                case ')':
                case '*':
                case '+':
                case ',':
                case '-':
                case '.':
                case '/':
                case ':':
                case ';':
                case '<':
                case '=':
                case '>':
                case '?':
                case '@':
                case '[':
                case '\\':
                case ']':
                case '^':
                case '_':
                case '`':
                case '{':
                case '|':
                case '}':
                case '~':
                    return true;
            }

            return false;
        }

        public static bool IsWhitespace(this char c) {
            // 2.1 Characters and lines
            // A whitespace character is a space(U + 0020), tab(U + 0009), newline(U + 000A), line tabulation (U + 000B), form feed (U + 000C), or carriage return (U + 000D).
            return c <= ' ' && (c == ' ' || c == '\t' || c == '\n' || c == '\v' || c == '\f' || c == '\r');
        }
    }
}
