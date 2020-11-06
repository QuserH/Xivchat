using System;
using System.Collections.Generic;

namespace XIVChat_Desktop {
    public static class Util {
        public static IEnumerable<List<T>> Chunks<T>(this List<T> locations, int nSize) {
            for (int i = 0; i < locations.Count; i += nSize) {
                yield return locations.GetRange(i, Math.Min(nSize, locations.Count - i));
            }
        }
    }
}
