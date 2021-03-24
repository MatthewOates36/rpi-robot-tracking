package org.team1619;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class SettingsHandler {

    private static final String SETTINGS_CONFIG_PATH = "settings-config.json";
    private static final String DATA_PATH = "settings-data.json";

    private static final JSONObject DATA = new JSONObject();

    private static final Set<Consumer<String>> UPDATE_LISTENERS = new HashSet<>();

    static {
        var config = getConfig();

        for(var i = 0; i < config.length(); i++) {
            var key = config.getJSONObject(i).getString("key");
            if(key.contains(":")) {
                throw new RuntimeException("Setting keys can't contain colons (" + key + ")");
            }
        }

        var data = getData();
        
        
        for(var key : data.keySet()) {
            DATA.put(key, data.get(key));
        }
    }

    private SettingsHandler() {

    }

    public static int getCameraWidth() {
//        return 640;
//        return 800;
        return 1280;
//        return 3264;
    }

    public static int getCameraHeight() {
//        return 480;
//        return 600;
        return 720;
//        return 2448;
    }

    public synchronized static void addUpdateListener(Consumer<String> listener) {
        UPDATE_LISTENERS.add(listener);
        for(var key : DATA.keySet()) {
            listener.accept(key);
        }
    }

    public synchronized static double getDouble(String key, double defaultValue) {
        return DATA.optDouble(key, defaultValue);
    }

    public synchronized static double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    public synchronized static String getString(String key, String defaultValue) {
        return DATA.optString(key, defaultValue);
    }

    public synchronized static String getString(String key) {
        return getString(key, null);
    }

    public synchronized static void put(String key, Object value) {
        DATA.put(key, value);

        var data = getData();
        data.put(key, value);
        setData(data);

        UPDATE_LISTENERS.forEach(listener -> listener.accept(key));
    }

    public synchronized static JSONArray getUiConfig() {
        var config = getConfig();
        var data = getData();

        for(var key : data.keySet()) {
            var settingKey = key;
            var initialKey = "value";
            if(settingKey.contains(":")) {
                var parts = settingKey.split(":");
                settingKey = parts[0];
                initialKey = parts[1];
            }

            for(var i = 0; i < config.length(); i++) {
                var element = config.getJSONObject(i);

                if(element.getString("key").equals(settingKey)) {
                    if(!element.has("initial")) {
                        element.put("initial", new JSONObject());
                    }

                    element.getJSONObject("initial").put(initialKey, data.get(key));

                    break;
                }
            }
        }

        return config;
    }

    public synchronized static JSONArray getConfig() {
        try {
            return new JSONArray(new String(SettingsHandler.class.getClassLoader().getResourceAsStream(SETTINGS_CONFIG_PATH).readAllBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized static JSONObject getData() {
        try {
            var path = Paths.get(DATA_PATH);

            if(!Files.exists(path)) {
                setData(new JSONObject());
            }

            return new JSONObject(new String(Files.readAllBytes(path)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized static void setData(JSONObject data) {
        try {
            Files.write(Paths.get(DATA_PATH), data.toString(4).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
