package com.watchable;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AppPreferences {
    private static final String PREFS   = "watchable_prefs";
    private static final String K_NAME  = "user_name";
    private static final String K_ID    = "user_id";
    private static final String K_DARK  = "dark_mode";
    private static final String K_LIST  = "watchable_collection";
    private static final String K_HIST  = "watchable_history";
    private static final String K_BOARD = "onboarded";
    private static final String K_NOTICE= "dont_show_server_notice";

    private static final String K_AVATAR= "user_avatar";

    private final SharedPreferences sp;
    private final Gson gson = new Gson();
    private static AppPreferences inst;

    private AppPreferences(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static AppPreferences get(Context ctx) {
        if (inst == null) inst = new AppPreferences(ctx);
        return inst;
    }

    public String getUserName() { return sp.getString(K_NAME, null); }
    public void setUserName(String n) { 
        sp.edit().putString(K_NAME, n).apply(); 
        syncProfileToFirebase();
    }
    public boolean hasUserName() { return getUserName() != null; }
    
    public String getUserAvatar() { return sp.getString(K_AVATAR, null); }
    public void setUserAvatar(String a) { 
        sp.edit().putString(K_AVATAR, a).apply(); 
        syncProfileToFirebase();
    }

    public void syncProfileToFirebase() {
        try {
            String uid = getUserId();
            String name = getUserName();
            String avatar = getUserAvatar();
            if (uid == null || uid.isEmpty()) return;

            DatabaseService.get().syncUserProfile(uid, name, avatar);
        } catch (Exception ignored) {}
    }

    public String getUserId() {
        String id = sp.getString(K_ID, null);
        if (id == null || id.isEmpty()) {
            id = "wn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            sp.edit().putString(K_ID, id).apply();
        } else if (!id.startsWith("wn_")) {
            id = "wn_" + (id.length() > 8 ? id.substring(0, 8) : id);
            sp.edit().putString(K_ID, id).apply();
        }
        return id;
    }

    public boolean isDarkMode() { return sp.getBoolean(K_DARK, true); }
    public void setDarkMode(boolean v) { sp.edit().putBoolean(K_DARK, v).apply(); }
    public boolean isOnboarded() { return sp.getBoolean(K_BOARD, false); }
    public void setOnboarded(boolean v) { sp.edit().putBoolean(K_BOARD, v).apply(); }
    public boolean isDontShowServerNotice() { return sp.getBoolean(K_NOTICE, false); }
    public void setDontShowServerNotice(boolean v) { sp.edit().putBoolean(K_NOTICE, v).apply(); }

    public List<MediaItem> getWatchlist() {
        Type t = new TypeToken<List<MediaItem>>(){}.getType();
        List<MediaItem> list = gson.fromJson(sp.getString(K_LIST,"[]"), t);
        return list != null ? list : new ArrayList<MediaItem>();
    }
    public void saveWatchlist(List<MediaItem> list) {
        sp.edit().putString(K_LIST, gson.toJson(list)).apply();
    }
    public boolean isInWatchlist(MediaItem item) {
        for (MediaItem i : getWatchlist())
            if (i.getId().equals(item.getId()) && i.getType().equals(item.getType())) return true;
        return false;
    }
    public void toggleWatchlist(MediaItem item) {
        List<MediaItem> list = getWatchlist();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(item.getId()) && list.get(i).getType().equals(item.getType())) {
                list.remove(i); found = true; break;
            }
        }
        if (!found) list.add(0, item);
        saveWatchlist(list);
    }

    public List<MediaItem> getHistory() {
        Type t = new TypeToken<List<MediaItem>>(){}.getType();
        List<MediaItem> list = gson.fromJson(sp.getString(K_HIST,"[]"), t);
        return list != null ? list : new ArrayList<MediaItem>();
    }
    public void addToHistory(MediaItem item) {
        List<MediaItem> list = getHistory();
        for (int i = list.size()-1; i >= 0; i--)
            if (list.get(i).getId().equals(item.getId()) && list.get(i).getType().equals(item.getType()))
                list.remove(i);
        list.add(0, item);
        if (list.size() > 40) list = list.subList(0,40);
        sp.edit().putString(K_HIST, gson.toJson(list)).apply();
    }
    public void clearAll() { sp.edit().clear().apply(); }
}
