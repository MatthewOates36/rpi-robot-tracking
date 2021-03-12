package org.team1619.web;

import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.team1619.SettingsHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

public class WebServer {

    private final Map<String, List<Object>> values;

    public WebServer() throws IOException {

        values = new HashMap<>();

        var app = new ContextManager();

        app.resourceFolder("/pages/", "/pages/");
        app.resourceFolder("/assets/", "/assets/");
        app.tempRedirect("", "/pages/tuner/tuner.html");
        app.get("/api/ui-config/", (req, res) -> res.body(SettingsHandler.getUiConfig().toString()).status(200));
        app.post("/api/update-setting/", (req, res) -> {
            var json = new JSONObject(req.body());

            for(var key : json.keySet()) {
                SettingsHandler.put(key, json.get(key));
            }

            res.status(200);
        });
        app.get("/api/get-value/{value-name}", (req, res) -> res.body(new JSONArray(values.getOrDefault(req.uriParam("value-name"), List.of())).toString()).status(200));

        var server = HttpServer.create(new InetSocketAddress(5801), 8);
        server.createContext("/", app);
        server.start();
    }

    public void setValue(String key, List<Object> valueList) {
        values.put(key, valueList);
    }

    public void setValue(String key, Object... valueList) {
        setValue(key, Arrays.asList(valueList));
    }
}
