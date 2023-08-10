package org.weasis.dicom.viewer2d;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.win32.StdCallLibrary;

import java.util.Arrays;
import java.util.List;

public class WindowsMessageUtil {
    public static final int WM_COPYDATA = 0x004A;

    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class);

        HWND FindWindowA(String lpClassName, String lpWindowName);

        LPARAM SendMessageA(HWND hWnd, int Msg, WinDef.WPARAM wParam, COPYDATASTRUCT lParam);
    }
    public static class COPYDATASTRUCT extends Structure {
        public Pointer dwData;
        public int cbData;
        public Pointer lpData;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("dwData", "cbData", "lpData");
        }
    }

    public static void SendWindowsMessage(String msg, String windowName) {
        HWND hwnd = User32.INSTANCE.FindWindowA(null, windowName);
        if (hwnd == null) {
            return;
        }

        byte[] data = Native.toByteArray(msg, "GBK");
        Memory lpszString = new Memory(data.length);
        lpszString.write(0, data, 0, data.length);

        COPYDATASTRUCT cds = new COPYDATASTRUCT();
        cds.dwData = new Pointer(0);
        cds.cbData = data.length;
        cds.lpData = lpszString;

        User32.INSTANCE.SendMessageA(hwnd, WM_COPYDATA, null, cds);
    }
}
