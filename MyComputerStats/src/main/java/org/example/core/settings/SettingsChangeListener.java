package org.example.core.settings;

import java.util.ArrayList;
import java.util.List;

public class SettingsChangeListener {

    public interface SettingsChangedCallback {
        void onSettingsChanged(AppSettings settings);
    }

    private static SettingsChangeListener instance;
    private List<SettingsChangedCallback> listeners = new ArrayList<>();

    private SettingsChangeListener() {}

    public static SettingsChangeListener getInstance() {
        if (instance == null) {
            instance = new SettingsChangeListener();
        }
        return instance;
    }

    public void addListener(SettingsChangedCallback listener) {
        listeners.add(listener);
    }

    public void removeListener(SettingsChangedCallback listener) {
        listeners.remove(listener);
    }

    public void notifySettingsChanged(AppSettings settings) {
        for (SettingsChangedCallback listener : listeners) {
            listener.onSettingsChanged(settings);
        }
    }
}

