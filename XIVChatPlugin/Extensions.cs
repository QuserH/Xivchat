using System;
using System.Collections.Generic;
using System.Linq;
using System.Numerics;

namespace XIVChatPlugin {
    public static class Extensions {
        public static string ToHexString(this IEnumerable<byte> bytes, bool upper = false, string separator = "") {
            return string.Join(separator, bytes.Select(b => b.ToString(upper ? "X2" : "x2")));
        }

        //public static List<uint> ToColours(this byte[] bytes) {
        //    var colours = new List<uint>();

        //    uint colour = 0xFF;
        //    for (int i = 0; i < bytes.Length; i++) {
        //        var idx = i % 3;

        //        if (i != 0 && idx == 0) {
        //            colours.Add(colour);
        //            colour = 0xFF;
        //        }

        //        colour |= (uint)bytes[i] << ((4 - idx - 1) * 8);
        //    }

        //    colours.Add(colour);

        //    return colours;
        //}

        public static List<Vector4> ToColours(this byte[] bytes) {
            var colours = new List<Vector4>();

            var colour = new Vector4(0f, 0f, 0f, 1f);
            for (int i = 0; i < bytes.Length; i++) {
                var idx = i % 3;

                if (i != 0 && idx == 0) {
                    colours.Add(colour);
                    colour = new Vector4(0f, 0f, 0f, 1f);
                }

                switch (idx) {
                    case 0:
                        colour.X = bytes[i] / 255f;
                        break;
                    case 1:
                        colour.Y = bytes[i] / 255f;
                        break;
                    case 2:
                        colour.Z = bytes[i] / 255f;
                        break;
                    default:
                        throw new ApplicationException("unreachable code reached");
                }
            }

            colours.Add(colour);

            return colours;
        }
    }
}
