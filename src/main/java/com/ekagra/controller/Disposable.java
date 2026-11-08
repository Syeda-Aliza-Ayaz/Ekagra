package com.ekagra.controller;

/** Controllers that register listeners implement this so they can unregister when the screen closes. */
public interface Disposable {
    void dispose();
}
