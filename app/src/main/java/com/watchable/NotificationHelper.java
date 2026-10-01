package com.watchable;

import android.content.Context;
import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Callback;
import okhttp3.Call;
import okhttp3.Response;
import java.io.IOException;

public class NotificationHelper {
    private static final OkHttpClient sharedClient = new OkHttpClient();

    public static void sendNotification(Context ctx, String dbUrl, MediaItem item, Integer currentSeason, Integer currentEpisode, String toUserId, String actionType, String actionText, String imdbId, String tmdbId) {
        String fromUserId = AppPreferences.get(ctx).getUserId();
        if (fromUserId == null || fromUserId.isEmpty()) {
            String uidKey = "user_id_uuid";
            android.content.SharedPreferences prefs = ctx.getSharedPreferences("video_stats", Context.MODE_PRIVATE);
            fromUserId = prefs.getString(uidKey, null);
            if (fromUserId == null || fromUserId.isEmpty()) {
                fromUserId = "wn_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                prefs.edit().putString(uidKey, fromUserId).apply();
            } else if (!fromUserId.startsWith("wn_")) {
                fromUserId = "wn_" + (fromUserId.length() > 8 ? fromUserId.substring(0, 8) : fromUserId);
                prefs.edit().putString(uidKey, fromUserId).apply();
            }
        }
        
        if (toUserId == null || toUserId.isEmpty() || toUserId.equals(fromUserId)) return;

        try {
            JSONObject payload = new JSONObject();
            payload.put("id", java.util.UUID.randomUUID().toString());
            payload.put("type", actionType);
            String uName = AppPreferences.get(ctx).getUserName();
            if (uName == null || uName.isEmpty()) uName = "User-" + fromUserId.substring(0, 6);
            payload.put("fromName", uName);
            payload.put("fromAvatar", AppPreferences.get(ctx).getUserAvatar());
            
            String showTitle = "Movie".equals(item.getType()) ? item.getTitle() : (item.getTitle() + " S" + currentSeason + " E" + currentEpisode);
            String msg = uName + " " + actionText + " your comment in " + showTitle;
            payload.put("msg", msg);
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("read", false);
            
            JSONObject mediaObj = new JSONObject();
            mediaObj.put("id", item.getId());
            mediaObj.put("title", item.getTitle());
            if (item.getPosterPath() != null) mediaObj.put("posterPath", item.getPosterPath());
            if (item.getBackdropPath() != null) mediaObj.put("backdropPath", item.getBackdropPath());
            mediaObj.put("voteAverage", item.getVoteAverage());
            mediaObj.put("type", item.getType());
            if (item.getSource() != null) mediaObj.put("source", item.getSource());
            if (imdbId != null) mediaObj.put("imdbId", imdbId);
            if (tmdbId != null) mediaObj.put("tmdbId", tmdbId);
            payload.put("mediaItem", mediaObj);
            
            if (currentSeason != null) payload.put("season", currentSeason);
            if (currentEpisode != null) payload.put("episode", currentEpisode);
            
            DatabaseService.get().sendNotification(toUserId, payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
