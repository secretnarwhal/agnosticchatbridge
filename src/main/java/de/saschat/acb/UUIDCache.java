package de.saschat.acb;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class UUIDCache {
    private record OnlineItem(String id, String name) {}
    private record FileItem(String uuid, String name) {}
    private static Map<String, String> uuidCache = new HashMap<>();
    private static final Gson gs = new Gson();

    private static String resolveDirect(String name) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/"+name);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();

            String text = new String(conn.getInputStream().readAllBytes());
            OnlineItem resp = gs.fromJson(text, OnlineItem.class);

            if(resp.id != null)
                return resp.id();
        } catch (Exception ex) {
        }
        return null;
    }
    private static String resolveLocal(String name) {
        try {
            JsonArray array = gs.fromJson(new FileReader("usercache.json"), JsonArray.class);
            for (Object o : array) {
                OnlineItem onlineItem = gs.fromJson((JsonObject) o, OnlineItem.class);
                if (onlineItem.name.equals(name))
                    return onlineItem.id;

            }
        } catch (Exception ex) {
        }
        return null;
    }
    public static String resolveUUID(String name) {
        String resolved = uuidCache.get(name);
        if(resolved == null)
            resolved = resolveLocal(name);
        if(resolved == null)
            resolved = resolveDirect(name);
        uuidCache.put(name, resolved);
        return resolved;
    }
}
