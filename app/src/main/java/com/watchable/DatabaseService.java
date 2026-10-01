package com.watchable;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DatabaseService {
    private static final String TAG = "DatabaseService";
    private static DatabaseService instance;
    private final OkHttpClient httpClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    public interface DataCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public static class StatsData {
        public long views = 0;
        public long likes = 0;
        public long dislikes = 0;
        public double ratingAverage = 0.0;
        public int userRating = 0;
        public boolean isLiked = false;
        public boolean isDisliked = false;
    }

    private DatabaseService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized DatabaseService get() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    private boolean isMongoDbMode() {
        String base = getBaseUrl();
        return !base.contains("firebasedatabase.app");
    }

    private String getBaseUrl() {
        String url = Constants.DB_URL;
        if (url == null || url.isEmpty()) {
            return "https://watchable-5a2ab-default-rtdb.asia-southeast1.firebasedatabase.app/";
        }
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url;
    }

    // ── Stats ────────────────────────────────────────────────────────────────────

    public void fetchStats(final String key, final String userId, final DataCallback<StatsData> cb) {
        if (key == null || key.isEmpty()) {
            if (cb != null) mainHandler.post(() -> cb.onSuccess(new StatsData()));
            return;
        }

        if (isMongoDbMode()) {
            String url = getBaseUrl() + "api/stats/" + key;
            Request req = new Request.Builder().url(url).build();
            httpClient.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (cb != null) mainHandler.post(() -> cb.onError(e));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) {
                        if (cb != null) mainHandler.post(() -> cb.onError(new Exception("HTTP " + response.code())));
                        return;
                    }
                    try {
                        String body = response.body().string();
                        JSONObject json = new JSONObject(body.isEmpty() ? "{}" : body);
                        StatsData data = new StatsData();
                        data.views = json.optLong("views", 0);
                        data.likes = json.optLong("likes", 0);
                        data.dislikes = json.optLong("dislikes", 0);
                        long sum = json.optLong("ratingSum", 0);
                        long count = json.optLong("ratingCount", 0);
                        data.ratingAverage = count > 0 ? ((double) sum / count) : 0.0;

                        JSONObject users = json.optJSONObject("users");
                        if (users != null && userId != null) {
                            JSONObject u = users.optJSONObject(userId);
                            if (u != null) {
                                data.isLiked = u.optBoolean("liked", false);
                                data.isDisliked = u.optBoolean("disliked", false);
                                data.userRating = u.optInt("rating", 0);
                            }
                        }
                        if (cb != null) mainHandler.post(() -> cb.onSuccess(data));
                    } catch (Exception e) {
                        if (cb != null) mainHandler.post(() -> cb.onError(e));
                    }
                }
            });
        } else {
            // Firebase mode
            String url = getBaseUrl() + "stats/" + key + ".json";
            Request req = new Request.Builder().url(url).build();
            httpClient.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (cb != null) mainHandler.post(() -> cb.onError(e));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) {
                        if (cb != null) mainHandler.post(() -> cb.onError(new Exception("HTTP " + response.code())));
                        return;
                    }
                    try {
                        String body = response.body().string();
                        if (body.equals("null") || body.isEmpty()) body = "{}";
                        JSONObject json = new JSONObject(body);
                        StatsData data = new StatsData();
                        data.views = json.optLong("views", 0);
                        data.likes = json.optLong("likes", 0);
                        data.dislikes = json.optLong("dislikes", 0);
                        long sum = json.optLong("ratingSum", 0);
                        long count = json.optLong("ratingCount", 0);
                        data.ratingAverage = count > 0 ? ((double) sum / count) : 0.0;

                        JSONObject users = json.optJSONObject("users");
                        if (users != null && userId != null) {
                            JSONObject u = users.optJSONObject(userId);
                            if (u != null) {
                                data.isLiked = u.optBoolean("liked", false);
                                data.isDisliked = u.optBoolean("disliked", false);
                                data.userRating = u.optInt("rating", 0);
                            }
                        }
                        if (cb != null) mainHandler.post(() -> cb.onSuccess(data));
                    } catch (Exception e) {
                        if (cb != null) mainHandler.post(() -> cb.onError(e));
                    }
                }
            });
        }
    }

    public void fetchBatchStats(final List<String> targetIds, final DataCallback<StatsData> cb) {
        if (targetIds == null || targetIds.isEmpty()) {
            if (cb != null) mainHandler.post(() -> cb.onSuccess(new StatsData()));
            return;
        }

        if (isMongoDbMode()) {
            // High-efficiency server-side MongoDB aggregation (no downloading whole database!)
            try {
                String url = getBaseUrl() + "api/stats/batch";
                JSONObject bodyJson = new JSONObject();
                JSONArray arr = new JSONArray();
                for (String tid : targetIds) arr.put(tid);
                bodyJson.put("targetIds", arr);

                RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, bodyJson.toString());
                Request req = new Request.Builder().url(url).post(body).build();
                httpClient.newCall(req).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        if (cb != null) mainHandler.post(() -> cb.onError(e));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful() || response.body() == null) {
                            if (cb != null) mainHandler.post(() -> cb.onError(new Exception("HTTP " + response.code())));
                            return;
                        }
                        try {
                            String resStr = response.body().string();
                            JSONObject json = new JSONObject(resStr.isEmpty() ? "{}" : resStr);
                            StatsData data = new StatsData();
                            data.views = json.optLong("views", 0);
                            data.likes = json.optLong("likes", 0);
                            data.dislikes = json.optLong("dislikes", 0);
                            data.ratingAverage = json.optDouble("ratingAverage", 0.0);
                            if (cb != null) mainHandler.post(() -> cb.onSuccess(data));
                        } catch (Exception e) {
                            if (cb != null) mainHandler.post(() -> cb.onError(e));
                        }
                    }
                });
            } catch (Exception e) {
                if (cb != null) mainHandler.post(() -> cb.onError(e));
            }
        } else {
            // Firebase mode: query stats.json safely
            String url = getBaseUrl() + "stats.json";
            Request req = new Request.Builder().url(url).build();
            httpClient.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (cb != null) mainHandler.post(() -> cb.onError(e));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) {
                        if (cb != null) mainHandler.post(() -> cb.onError(new Exception("HTTP " + response.code())));
                        return;
                    }
                    try {
                        String body = response.body().string();
                        if (body == null || body.equals("null") || body.trim().isEmpty()) {
                            if (cb != null) mainHandler.post(() -> cb.onSuccess(new StatsData()));
                            return;
                        }
                        JSONObject allStats = new JSONObject(body);
                        long views = 0, likes = 0, dislikes = 0, sum = 0, count = 0;
                        Iterator<String> keys = allStats.keys();
                        while (keys.hasNext()) {
                            String k = keys.next();
                            boolean matches = false;
                            for (String tid : targetIds) {
                                if (k.equals("MOVIE_" + tid) ||
                                    k.equals("ANIME_MOVIE_" + tid) ||
                                    k.startsWith("TV_" + tid + "_") ||
                                    k.startsWith("ANIME_TV_" + tid + "_") ||
                                    k.startsWith("TV_" + tid) ||
                                    k.startsWith("ANIME_TV_" + tid) ||
                                    k.startsWith("ANIME_MOVIE_" + tid) ||
                                    k.equals(tid)) {
                                    matches = true;
                                    break;
                                }
                            }
                            if (matches) {
                                JSONObject obj = allStats.optJSONObject(k);
                                if (obj != null) {
                                    views += obj.optLong("views", 0);
                                    likes += obj.optLong("likes", 0);
                                    dislikes += obj.optLong("dislikes", 0);
                                    sum += obj.optLong("ratingSum", 0);
                                    count += obj.optLong("ratingCount", 0);
                                }
                            }
                        }
                        StatsData data = new StatsData();
                        data.views = views;
                        data.likes = likes;
                        data.dislikes = dislikes;
                        data.ratingAverage = count > 0 ? ((double) sum / count) : 0.0;
                        if (cb != null) mainHandler.post(() -> cb.onSuccess(data));
                    } catch (Exception e) {
                        if (cb != null) mainHandler.post(() -> cb.onError(e));
                    }
                }
            });
        }
    }

    public void recordAction(final String key, final String action, final String userId, final String userName,
                             final Integer rating, final Boolean isLiked, final Boolean isDisliked,
                             final DataCallback<StatsData> cb) {
        if (key == null || key.isEmpty()) return;

        if (isMongoDbMode()) {
            try {
                String url = getBaseUrl() + "api/stats/" + key + "/action";
                JSONObject bodyJson = new JSONObject();
                bodyJson.put("action", action);
                bodyJson.put("userId", userId);
                bodyJson.put("userName", userName);
                if (rating != null) bodyJson.put("rating", rating);
                if (isLiked != null) bodyJson.put("isLiked", isLiked);
                if (isDisliked != null) bodyJson.put("isDisliked", isDisliked);

                RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, bodyJson.toString());
                Request req = new Request.Builder().url(url).post(body).build();
                httpClient.newCall(req).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        if (cb != null) mainHandler.post(() -> cb.onError(e));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful() || response.body() == null) {
                            if (cb != null) mainHandler.post(() -> cb.onError(new Exception("HTTP " + response.code())));
                            return;
                        }
                        try {
                            String resStr = response.body().string();
                            JSONObject json = new JSONObject(resStr.isEmpty() ? "{}" : resStr);
                            StatsData data = new StatsData();
                            data.views = json.optLong("views", 0);
                            data.likes = json.optLong("likes", 0);
                            data.dislikes = json.optLong("dislikes", 0);
                            long s = json.optLong("ratingSum", 0);
                            long c = json.optLong("ratingCount", 0);
                            data.ratingAverage = c > 0 ? ((double) s / c) : 0.0;
                            if (cb != null) mainHandler.post(() -> cb.onSuccess(data));
                        } catch (Exception e) {
                            if (cb != null) mainHandler.post(() -> cb.onError(e));
                        }
                    }
                });
            } catch (Exception e) {
                if (cb != null) mainHandler.post(() -> cb.onError(e));
            }
        } else {
            // Firebase mode patch
            String url = getBaseUrl() + "stats/" + key + ".json";
            Request fetchReq = new Request.Builder().url(url).build();
            httpClient.newCall(fetchReq).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (cb != null) mainHandler.post(() -> cb.onError(e));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        if (cb != null) mainHandler.post(() -> cb.onError(new Exception("HTTP " + response.code())));
                        return;
                    }
                    try {
                        String b = response.body() != null ? response.body().string() : "{}";
                        if (b == null || b.equals("null")) b = "{}";
                        JSONObject json = new JSONObject(b.startsWith("{") ? b : "{}");

                        JSONObject patchJson = new JSONObject();
                        JSONObject userPatchJson = new JSONObject();
                        userPatchJson.put("name", userName != null ? userName : "User");
                        userPatchJson.put("lastActive", System.currentTimeMillis());

                        if ("rating".equals(action) && rating != null) {
                            double sum = json.optDouble("ratingSum", 0);
                            int count = json.optInt("ratingCount", 0);
                            int trueOldRating = 0;
                            JSONObject usersObj = json.optJSONObject("users");
                            if (usersObj != null) {
                                JSONObject myU = usersObj.optJSONObject(userId);
                                if (myU != null) trueOldRating = myU.optInt("rating", 0);
                            }
                            if (trueOldRating > 0) {
                                sum = Math.max(0, sum - trueOldRating + rating);
                            } else {
                                sum += rating;
                                count++;
                            }
                            patchJson.put("ratingSum", sum);
                            patchJson.put("ratingCount", count);
                            userPatchJson.put("rating", rating);
                        } else if ("views".equals(action)) {
                            patchJson.put("views", json.optLong("views", 0) + 1);
                            userPatchJson.put("viewed", true);
                        } else if ("like_dislike".equals(action)) {
                            long curLikes = json.optLong("likes", 0);
                            long curDislikes = json.optLong("dislikes", 0);
                            boolean prevLiked = false, prevDisliked = false;
                            JSONObject usersObj = json.optJSONObject("users");
                            if (usersObj != null) {
                                JSONObject myU = usersObj.optJSONObject(userId);
                                if (myU != null) {
                                    prevLiked = myU.optBoolean("liked", false);
                                    prevDisliked = myU.optBoolean("disliked", false);
                                }
                            }
                            if (Boolean.TRUE.equals(isLiked) && !prevLiked) {
                                curLikes++;
                                if (prevDisliked) curDislikes = Math.max(0, curDislikes - 1);
                            } else if (Boolean.FALSE.equals(isLiked) && prevLiked) {
                                curLikes = Math.max(0, curLikes - 1);
                            }
                            if (Boolean.TRUE.equals(isDisliked) && !prevDisliked) {
                                curDislikes++;
                                if (prevLiked) curLikes = Math.max(0, curLikes - 1);
                            } else if (Boolean.FALSE.equals(isDisliked) && prevDisliked) {
                                curDislikes = Math.max(0, curDislikes - 1);
                            }
                            patchJson.put("likes", curLikes);
                            patchJson.put("dislikes", curDislikes);
                            userPatchJson.put("liked", Boolean.TRUE.equals(isLiked));
                            userPatchJson.put("disliked", Boolean.TRUE.equals(isDisliked));
                        }

                        JSONObject usersWrapper = new JSONObject();
                        usersWrapper.put(userId, userPatchJson);
                        patchJson.put("users", usersWrapper);

                        RequestBody patchBody = RequestBody.create(JSON_MEDIA_TYPE, patchJson.toString());
                        Request patchReq = new Request.Builder().url(url).patch(patchBody).build();
                        httpClient.newCall(patchReq).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                if (cb != null) mainHandler.post(() -> cb.onError(e));
                            }

                            @Override
                            public void onResponse(Call call, Response response) {
                                if (cb != null) fetchStats(key, userId, cb);
                            }
                        });
                    } catch (Exception e) {
                        if (cb != null) mainHandler.post(() -> cb.onError(e));
                    }
                }
            });
        }
    }

    // ── Comments ─────────────────────────────────────────────────────────────────

    public void fetchComments(final String key, final DataCallback<List<Comment>> cb) {
        if (key == null || key.isEmpty()) {
            if (cb != null) mainHandler.post(() -> cb.onSuccess(new ArrayList<>()));
            return;
        }

        String url = isMongoDbMode()
                ? (getBaseUrl() + "api/comments/" + key)
                : (getBaseUrl() + "comments/" + key + ".json");

        Request req = new Request.Builder().url(url).build();
        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (cb != null) mainHandler.post(() -> cb.onError(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    if (cb != null) mainHandler.post(() -> cb.onError(new Exception("HTTP " + response.code())));
                    return;
                }
                try {
                    String json = response.body().string();
                    if (json == null || json.equals("null") || json.isEmpty()) {
                        if (cb != null) mainHandler.post(() -> cb.onSuccess(new ArrayList<>()));
                        return;
                    }
                    JSONObject obj = new JSONObject(json);
                    List<Comment> result = new ArrayList<>();
                    Iterator<String> keys = obj.keys();
                    while (keys.hasNext()) {
                        String k = keys.next();
                        JSONObject cObj = obj.getJSONObject(k);
                        result.add(parseComment(k, cObj, null));
                    }
                    if (cb != null) mainHandler.post(() -> cb.onSuccess(result));
                } catch (Exception e) {
                    if (cb != null) mainHandler.post(() -> cb.onError(e));
                }
            }
        });
    }

    private Comment parseComment(String id, JSONObject obj, String parentId) {
        Comment c = new Comment(id, obj.optString("authorId", ""), obj.optString("authorName", "User"),
                obj.optString("authorAvatar", ""), obj.optString("timeAgo", ""), obj.optString("text", ""),
                obj.optInt("likeCount", 0));
        c.setParentId(parentId);
        JSONObject repliesObj = obj.optJSONObject("replies");
        if (repliesObj != null) {
            Iterator<String> rKeys = repliesObj.keys();
            while (rKeys.hasNext()) {
                String rId = rKeys.next();
                JSONObject replyObj = repliesObj.optJSONObject(rId);
                if (replyObj != null) {
                    c.addReply(parseComment(rId, replyObj, id));
                }
            }
        }
        return c;
    }

    public void saveComment(final String key, final Comment c, final Comment parent, final DataCallback<Boolean> cb) {
        if (key == null || key.isEmpty()) return;
        try {
            JSONObject cObj = new JSONObject();
            cObj.put("commentId", c.getId());
            cObj.put("authorId", c.getAuthorId());
            cObj.put("authorName", c.getAuthorName());
            cObj.put("authorAvatar", c.getAuthorAvatar());
            cObj.put("timeAgo", c.getTimeAgo());
            cObj.put("text", c.getText());
            cObj.put("likeCount", c.getLikeCount());
            if (parent != null) cObj.put("parentId", parent.getId());

            String url;
            Request req;
            if (isMongoDbMode()) {
                url = getBaseUrl() + "api/comments/" + key;
                RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, cObj.toString());
                req = new Request.Builder().url(url).post(body).build();
            } else {
                url = getBaseUrl() + "comments/" + key + "/" +
                        (parent == null ? c.getId() : (parent.getId() + "/replies/" + c.getId())) + ".json";
                RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, cObj.toString());
                req = new Request.Builder().url(url).put(body).build();
            }

            httpClient.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (cb != null) mainHandler.post(() -> cb.onError(e));
                }

                @Override
                public void onResponse(Call call, Response response) {
                    if (cb != null) mainHandler.post(() -> cb.onSuccess(response.isSuccessful()));
                }
            });
        } catch (Exception e) {
            if (cb != null) mainHandler.post(() -> cb.onError(e));
        }
    }

    public void updateComment(final String key, final String commentId, final String parentId, final String newText,
                              final Integer likeCount, final DataCallback<Boolean> cb) {
        if (key == null || commentId == null) return;
        try {
            if (isMongoDbMode()) {
                String url = getBaseUrl() + "api/comments/" + key + "/" + commentId;
                JSONObject update = new JSONObject();
                if (newText != null) update.put("text", newText);
                if (likeCount != null) update.put("likeCount", likeCount);

                RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, update.toString());
                Request req = new Request.Builder().url(url).put(body).build();
                httpClient.newCall(req).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        if (cb != null) mainHandler.post(() -> cb.onError(e));
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        if (cb != null) mainHandler.post(() -> cb.onSuccess(response.isSuccessful()));
                    }
                });
            } else {
                String subPath = parentId == null ? commentId : (parentId + "/replies/" + commentId);
                if (newText != null) {
                    String url = getBaseUrl() + "comments/" + key + "/" + subPath + "/text.json";
                    RequestBody b = RequestBody.create(JSON_MEDIA_TYPE, JSONObject.quote(newText));
                    httpClient.newCall(new Request.Builder().url(url).put(b).build()).enqueue(new Callback() {
                        @Override public void onFailure(Call call, IOException e) {}
                        @Override public void onResponse(Call call, Response response) {}
                    });
                }
                if (likeCount != null) {
                    String url = getBaseUrl() + "comments/" + key + "/" + subPath + "/likeCount.json";
                    RequestBody b = RequestBody.create(JSON_MEDIA_TYPE, String.valueOf(likeCount));
                    httpClient.newCall(new Request.Builder().url(url).put(b).build()).enqueue(new Callback() {
                        @Override public void onFailure(Call call, IOException e) {}
                        @Override public void onResponse(Call call, Response response) {}
                    });
                }
                if (cb != null) mainHandler.post(() -> cb.onSuccess(true));
            }
        } catch (Exception e) {
            if (cb != null) mainHandler.post(() -> cb.onError(e));
        }
    }

    public void deleteComment(final String key, final Comment c, final Comment parent, final DataCallback<Boolean> cb) {
        if (key == null || c == null) return;
        String url;
        if (isMongoDbMode()) {
            url = getBaseUrl() + "api/comments/" + key + "/" + c.getId();
        } else {
            url = getBaseUrl() + "comments/" + key + "/" +
                    (parent == null ? c.getId() : (parent.getId() + "/replies/" + c.getId())) + ".json";
        }
        Request req = new Request.Builder().url(url).delete().build();
        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (cb != null) mainHandler.post(() -> cb.onError(e));
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (cb != null) mainHandler.post(() -> cb.onSuccess(response.isSuccessful()));
            }
        });
    }

    // ── Notifications ────────────────────────────────────────────────────────────

    public void fetchNotifications(final String userId, final DataCallback<List<JSONObject>> cb) {
        if (userId == null || userId.isEmpty()) {
            if (cb != null) mainHandler.post(() -> cb.onSuccess(new ArrayList<>()));
            return;
        }

        String url = isMongoDbMode()
                ? (getBaseUrl() + "api/notifications/" + userId)
                : (getBaseUrl() + "notifications/" + userId + ".json");

        Request req = new Request.Builder().url(url).build();
        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (cb != null) mainHandler.post(() -> cb.onError(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    if (cb != null) mainHandler.post(() -> cb.onError(new Exception("HTTP " + response.code())));
                    return;
                }
                try {
                    String body = response.body().string();
                    if (body == null || body.equals("null") || body.isEmpty()) {
                        if (cb != null) mainHandler.post(() -> cb.onSuccess(new ArrayList<>()));
                        return;
                    }
                    JSONObject obj = new JSONObject(body);
                    List<JSONObject> list = new ArrayList<>();
                    Iterator<String> keys = obj.keys();
                    while (keys.hasNext()) {
                        list.add(obj.getJSONObject(keys.next()));
                    }
                    Collections.sort(list, new Comparator<JSONObject>() {
                        @Override
                        public int compare(JSONObject o1, JSONObject o2) {
                            return Long.compare(o2.optLong("timestamp", 0), o1.optLong("timestamp", 0));
                        }
                    });
                    if (cb != null) mainHandler.post(() -> cb.onSuccess(list));
                } catch (Exception e) {
                    if (cb != null) mainHandler.post(() -> cb.onError(e));
                }
            }
        });
    }

    public void getUnreadNotificationCount(final String userId, final DataCallback<Integer> cb) {
        fetchNotifications(userId, new DataCallback<List<JSONObject>>() {
            @Override
            public void onSuccess(List<JSONObject> result) {
                int unread = 0;
                for (JSONObject o : result) {
                    if (!o.optBoolean("read", false)) unread++;
                }
                if (cb != null) cb.onSuccess(unread);
            }

            @Override
            public void onError(Exception e) {
                if (cb != null) cb.onError(e);
            }
        });
    }

    public void markAllNotificationsRead(final String userId) {
        if (userId == null || userId.isEmpty()) return;
        if (isMongoDbMode()) {
            String url = getBaseUrl() + "api/notifications/" + userId + "/read-all";
            RequestBody b = RequestBody.create(JSON_MEDIA_TYPE, "{}");
            httpClient.newCall(new Request.Builder().url(url).put(b).build()).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response response) {}
            });
        } else {
            fetchNotifications(userId, new DataCallback<List<JSONObject>>() {
                @Override
                public void onSuccess(List<JSONObject> result) {
                    for (JSONObject o : result) {
                        String nid = o.optString("id");
                        if (!nid.isEmpty()) {
                            String u = getBaseUrl() + "notifications/" + userId + "/" + nid + "/read.json";
                            RequestBody b = RequestBody.create(JSON_MEDIA_TYPE, "true");
                            httpClient.newCall(new Request.Builder().url(u).put(b).build()).enqueue(new Callback() {
                                @Override public void onFailure(Call call, IOException e) {}
                                @Override public void onResponse(Call call, Response response) {}
                            });
                        }
                    }
                }
                @Override public void onError(Exception e) {}
            });
        }
    }

    public void deleteAllNotifications(final String userId) {
        if (userId == null || userId.isEmpty()) return;
        String url = isMongoDbMode()
                ? (getBaseUrl() + "api/notifications/" + userId)
                : (getBaseUrl() + "notifications/" + userId + ".json");
        httpClient.newCall(new Request.Builder().url(url).delete().build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) {}
        });
    }

    public void sendNotification(final String toUserId, final JSONObject payload) {
        if (toUserId == null || payload == null) return;
        try {
            String url;
            Request req;
            if (isMongoDbMode()) {
                url = getBaseUrl() + "api/notifications/" + toUserId;
                RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, payload.toString());
                req = new Request.Builder().url(url).post(body).build();
            } else {
                String id = payload.optString("id", String.valueOf(System.currentTimeMillis()));
                url = getBaseUrl() + "notifications/" + toUserId + "/" + id + ".json";
                RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, payload.toString());
                req = new Request.Builder().url(url).put(body).build();
            }
            httpClient.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response response) {}
            });
        } catch (Exception ignored) {}
    }

    // ── Users ────────────────────────────────────────────────────────────────────

    public void syncUserProfile(final String userId, final String name, final String avatar) {
        if (userId == null || userId.isEmpty()) return;
        try {
            JSONObject obj = new JSONObject();
            if (name != null && !name.trim().isEmpty()) obj.put("name", name.trim());
            if (avatar != null && !avatar.trim().isEmpty()) obj.put("avatar", avatar.trim());
            obj.put("lastActive", System.currentTimeMillis());

            String url;
            Request req;
            if (isMongoDbMode()) {
                url = getBaseUrl() + "api/users/" + userId;
                RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, obj.toString());
                req = new Request.Builder().url(url).put(body).build();
            } else {
                url = getBaseUrl() + "users/" + userId + ".json";
                RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, obj.toString());
                req = new Request.Builder().url(url).patch(body).build();
            }
            httpClient.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response response) {}
            });
        } catch (Exception ignored) {}
    }

    // ── App Update ───────────────────────────────────────────────────────────────

    public void fetchAppUpdate(final DataCallback<AppUpdateModel> cb) {
        String url = isMongoDbMode()
                ? (getBaseUrl() + "api/app_update")
                : (getBaseUrl() + "app_update.json");

        Request req = new Request.Builder().url(url).build();
        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (cb != null) mainHandler.post(() -> cb.onError(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    if (cb != null) mainHandler.post(() -> cb.onError(new Exception("HTTP " + response.code())));
                    return;
                }
                try {
                    String json = response.body().string();
                    com.google.gson.JsonObject gObj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
                    AppUpdateModel model = AppUpdateModel.fromJson(gObj);
                    if (cb != null) mainHandler.post(() -> cb.onSuccess(model));
                } catch (Exception e) {
                    if (cb != null) mainHandler.post(() -> cb.onError(e));
                }
            }
        });
    }
}
