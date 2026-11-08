package com.ekagra.tracking;

/** The window the user is looking at: its program (e.g. "chrome.exe") and title bar text. */
public record WindowInfo(String processName, String title) {
}
