package com.palosj.waystonesptpt.client;

import java.util.Map;
import java.util.WeakHashMap;

public final class PlayerPanelLifecycle<K, V> {
    private final Map<K, V> panels = new WeakHashMap<>();

    public void attach(K key, V panel) {
        panels.put(key, panel);
    }

    public V get(K key) {
        return panels.get(key);
    }

    public void detach(K key) {
        panels.remove(key);
    }

    public boolean isEmpty() {
        return panels.isEmpty();
    }

    public int size() {
        return panels.size();
    }
}
