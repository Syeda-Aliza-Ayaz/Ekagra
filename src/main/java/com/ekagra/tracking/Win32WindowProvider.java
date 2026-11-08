package com.ekagra.tracking;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads the active window and idle time from the Windows API through JNA (FR-15, FR-17):
 * GetForegroundWindow, GetWindowText, GetWindowThreadProcessId and GetLastInputInfo.
 */
public class Win32WindowProvider implements WindowProvider {

    private final boolean available = Platform.isWindows();

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public Optional<WindowInfo> foregroundWindow() {
        if (!available) {
            return Optional.empty();
        }
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null) {
            return Optional.empty();
        }
        char[] buffer = new char[512];
        int len = User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
        String title = new String(buffer, 0, Math.max(0, len)).trim();

        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
        String process = ProcessHandle.of(pid.getValue())
            .flatMap(ph -> ph.info().command())
            .map(cmd -> Path.of(cmd).getFileName().toString().toLowerCase())
            .orElse("unknown");

        if (title.isEmpty() && "explorer.exe".equals(process)) {
            return Optional.empty();   // desktop / taskbar focused: not real activity
        }
        return Optional.of(new WindowInfo(process, title));
    }

    @Override
    public long idleMillis() {
        if (!available) {
            return 0;
        }
        WinUser.LASTINPUTINFO info = new WinUser.LASTINPUTINFO();
        if (!User32.INSTANCE.GetLastInputInfo(info)) {
            return 0;
        }
        long now = Integer.toUnsignedLong(Kernel32.INSTANCE.GetTickCount());
        long last = Integer.toUnsignedLong(info.dwTime);
        return Math.max(0, now - last);
    }
}
