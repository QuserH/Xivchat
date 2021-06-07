using System;
using System.Collections.Generic;

namespace XIVChatPlugin {
    internal static class Util {
        public static int IndexOfCount(this string source, char toFind, int position) {
            var index = -1;
            for (var i = 0; i < position; i++) {
                index = source.IndexOf(toFind, index + 1);

                if (index == -1) {
                    return -1;
                }
            }

            return index;
        }

        public static byte[] Terminate(this byte[] bytes) {
            var terminated = new byte[bytes.Length + 1];
            Array.Copy(bytes, terminated, bytes.Length);
            terminated[terminated.Length - 1] = 0;
            return terminated;
        }

        public static unsafe byte[] ReadTerminated(byte* mem) {
            var bytes = new List<byte>();
            while (*mem != 0) {
                bytes.Add(*mem);
                mem += 1;
            }

            return bytes.ToArray();
        }
    }
}
