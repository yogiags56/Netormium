package com.netormium.client.util;

/** Riwayat linear dengan dukungan Back dan Forward. */
public final class BrowserHistory {
    private final String[] entries;
    private int size;
    private int index;

    public BrowserHistory(int capacity) {
        if (capacity < 2) capacity = 2;
        entries = new String[capacity];
        size = 0;
        index = -1;
    }

    public void visit(String url) {
        if (url == null) return;
        while (size > index + 1) {
            entries[--size] = null;
        }
        if (size > 0 && url.equals(entries[size - 1])) {
            index = size - 1;
            return;
        }
        if (size == entries.length) {
            for (int i = 1; i < size; i++) entries[i - 1] = entries[i];
            entries[--size] = null;
            index--;
        }
        entries[size++] = url;
        index = size - 1;
    }

    public String back() {
        if (index > 0) return entries[--index];
        return null;
    }

    public String forward() {
        if (index >= 0 && index < size - 1) return entries[++index];
        return null;
    }

    public String[] entries() {
        String[] out = new String[size];
        for (int i = 0; i < size; i++) out[i] = entries[i];
        return out;
    }
    public int index() { return index; }
    public void restore(String[] values, int selected) {
        size = 0; index = -1;
        if (values == null) return;
        int start = Math.max(0, values.length - entries.length);
        for (int i = start; i < values.length; i++) if (values[i] != null) entries[size++] = values[i];
        if (size > 0) index = Math.max(0, Math.min(size - 1, selected - start));
    }
    public boolean removeAt(int position) {
        if (position < 0 || position >= size) return false;
        for (int i = position; i < size - 1; i++) entries[i] = entries[i + 1];
        entries[--size] = null;
        if (size == 0) index = -1;
        else if (position < index) index--;
        else if (position == index && index >= size) index = size - 1;
        return true;
    }

    public void clear() {
        for (int i = 0; i < size; i++) entries[i] = null;
        size = 0;
        index = -1;
    }
    public String current() {
        return index >= 0 ? entries[index] : null;
    }
}
