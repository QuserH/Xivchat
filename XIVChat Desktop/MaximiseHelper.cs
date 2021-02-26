using System;
using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Interop;

namespace XIVChat_Desktop {
    public static class MaximiseHelper {
        public static void FixMaximise(object? sender, EventArgs e) {
            if (!(sender is Window window)) {
                return;
            }

            var handle = new WindowInteropHelper(window).Handle;
            HwndSource.FromHwnd(handle)?.AddHook(WindowProc);
        }

        private static IntPtr WindowProc(IntPtr hwnd, int msg, IntPtr wParam, IntPtr lParam, ref bool handled) {
            switch (msg) {
                case 0x0024:
                    WmGetMinMaxInfo(hwnd, lParam);
                    break;
            }

            return IntPtr.Zero;
        }

        private static void WmGetMinMaxInfo(IntPtr hwnd, IntPtr lParam) {
            GetCursorPos(out var mousePos);

            var currentScreenPtr = MonitorFromPoint(mousePos, MonitorOptions.MONITOR_DEFAULTTONEAREST);

            var lMmi = (MINMAXINFO) Marshal.PtrToStructure(lParam, typeof(MINMAXINFO))!;

            var currentScreen = new MONITORINFO();
            if (!GetMonitorInfo(currentScreenPtr, currentScreen)) {
                return;
            }

            // x,y coords when maximised
            lMmi.ptMaxPosition.X = currentScreen.rcWork.Left - currentScreen.rcMonitor.Left;
            lMmi.ptMaxPosition.Y = currentScreen.rcWork.Top - currentScreen.rcMonitor.Top;
            // max width and height allowed
            lMmi.ptMaxSize.X = currentScreen.rcWork.Right - currentScreen.rcWork.Left;
            lMmi.ptMaxSize.Y = currentScreen.rcWork.Bottom - currentScreen.rcWork.Top;

            Marshal.StructureToPtr(lMmi, lParam, true);
        }

        [DllImport("user32.dll")]
        [return: MarshalAs(UnmanagedType.Bool)]
        internal static extern bool GetCursorPos(out POINT lpPoint);


        [DllImport("user32.dll", SetLastError = true)]
        private static extern IntPtr MonitorFromPoint(POINT pt, MonitorOptions dwFlags);

        private enum MonitorOptions : uint {
            MONITOR_DEFAULTTONULL = 0x00000000,
            MONITOR_DEFAULTTOPRIMARY = 0x00000001,
            MONITOR_DEFAULTTONEAREST = 0x00000002,
        }


        [DllImport("user32.dll")]
        private static extern bool GetMonitorInfo(IntPtr hMonitor, MONITORINFO lpmi);

        [StructLayout(LayoutKind.Sequential)]
        public struct POINT {
            public int X;
            public int Y;
        }


        [StructLayout(LayoutKind.Sequential)]
        public struct MINMAXINFO {
            public POINT ptReserved;
            public POINT ptMaxSize;
            public POINT ptMaxPosition;
            public POINT ptMinTrackSize;
            public POINT ptMaxTrackSize;
        };


        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Auto)]
        public class MONITORINFO {
            public int cbSize = Marshal.SizeOf(typeof(MONITORINFO));
            public RECT rcMonitor = new();
            public RECT rcWork = new();
            public int dwFlags = 0;
        }


        [StructLayout(LayoutKind.Sequential)]
        public struct RECT {
            public int Left, Top, Right, Bottom;
        }
    }
}
