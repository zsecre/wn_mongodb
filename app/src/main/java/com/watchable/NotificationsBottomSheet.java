package com.watchable;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationsBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView recycler;
    private TextView emptyView;
    private NotificationAdapter adapter;
    private List<JSONObject> notifications = new ArrayList<>();
    private OkHttpClient httpClient = new OkHttpClient();
    private String userId;

    public interface CountListener {
        void onCountReady(int unreadCount);
    }
    private CountListener countListener;

    public void setCountListener(CountListener listener) {
        this.countListener = listener;
    }

    public static NotificationsBottomSheet newInstance() {
        return new NotificationsBottomSheet();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) d;
            View bottomSheetInternal = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetInternal);
                int height = getResources().getDisplayMetrics().heightPixels / 2;
                ViewGroup.LayoutParams layoutParams = bottomSheetInternal.getLayoutParams();
                layoutParams.height = height;
                bottomSheetInternal.setLayoutParams(layoutParams);
                behavior.setPeekHeight(height);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_notifications_sheet, container, false);
        recycler = view.findViewById(R.id.notifications_recycler);
        emptyView = view.findViewById(R.id.notifications_empty);

        userId = AppPreferences.get(getContext()).getUserId();
        if (userId == null || userId.isEmpty()) {
            String uidKey = "user_id_uuid";
            android.content.SharedPreferences prefs = getContext().getSharedPreferences("video_stats", Context.MODE_PRIVATE);
            userId = prefs.getString(uidKey, null);
            if (userId == null || userId.isEmpty()) {
                userId = "wn_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                prefs.edit().putString(uidKey, userId).apply();
            } else if (!userId.startsWith("wn_")) {
                userId = "wn_" + (userId.length() > 8 ? userId.substring(0, 8) : userId);
                prefs.edit().putString(uidKey, userId).apply();
            }
        }

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter();
        recycler.setAdapter(adapter);

        view.findViewById(R.id.btn_mark_all_read).setOnClickListener(v -> markAllRead());
        view.findViewById(R.id.btn_delete_all).setOnClickListener(v -> deleteAll());

        fetchNotifications();

        return view;
    }

    private void fetchNotifications() {
        DatabaseService.get().fetchNotifications(userId, new DatabaseService.DataCallback<List<JSONObject>>() {
            @Override
            public void onSuccess(List<JSONObject> list) {
                updateUI(list);
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        android.widget.Toast.makeText(getContext(), "Failed to fetch notifications", android.widget.Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void updateUI(List<JSONObject> list) {
        new Handler(Looper.getMainLooper()).post(() -> {
            notifications.clear();
            notifications.addAll(list);
            adapter.notifyDataSetChanged();
            if (notifications.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recycler.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recycler.setVisibility(View.VISIBLE);
            }
            if (countListener != null) {
                int unread = 0;
                for (JSONObject o : notifications) {
                    if (!o.optBoolean("read", false)) unread++;
                }
                countListener.onCountReady(unread);
            }
        });
    }

    private void markAllRead() {
        for (JSONObject o : notifications) {
            try {
                o.put("read", true);
            } catch (Exception ignored) {}
        }
        DatabaseService.get().markAllNotificationsRead(userId);
        adapter.notifyDataSetChanged();
        if (countListener != null) countListener.onCountReady(0);
    }

    private void deleteAll() {
        DatabaseService.get().deleteAllNotifications(userId);
        notifications.clear();
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(View.VISIBLE);
        recycler.setVisibility(View.GONE);
        if (countListener != null) countListener.onCountReady(0);
    }

    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            ImageView avatar;
            TextView text, time;
            View unreadDot;
            VH(View v) {
                super(v);
                avatar = v.findViewById(R.id.notif_avatar);
                text = v.findViewById(R.id.notif_text);
                time = v.findViewById(R.id.notif_time);
                unreadDot = v.findViewById(R.id.notif_unread_dot);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            JSONObject obj = notifications.get(position);
            holder.text.setText(obj.optString("msg", ""));
            long ts = obj.optLong("timestamp", 0);
            CharSequence timeAgo = android.text.format.DateUtils.getRelativeTimeSpanString(ts, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS);
            holder.time.setText(timeAgo);
            
            String avatarUrl = obj.optString("fromAvatar", "");
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(holder.itemView).load(avatarUrl).circleCrop().into(holder.avatar);
            } else {
                holder.avatar.setImageResource(R.drawable.placeholder_poster);
            }

            boolean read = obj.optBoolean("read", false);
            holder.unreadDot.setVisibility(read ? View.GONE : View.VISIBLE);
            
            holder.itemView.setOnClickListener(v -> {
                if (!obj.optBoolean("read", false)) {
                    try {
                        obj.put("read", true);
                        notifyItemChanged(position);
                        String url = Constants.DB_URL + "notifications/" + userId + "/" + obj.getString("id") + "/read.json";
                        RequestBody body = RequestBody.create("true", MediaType.parse("application/json"));
                        Request req = new Request.Builder().url(url).put(body).build();
                        httpClient.newCall(req).enqueue(new Callback() {
                            @Override public void onFailure(Call call, IOException e) {}
                            @Override public void onResponse(Call call, Response response) {}
                        });
                        
                        if (countListener != null) {
                            int unread = 0;
                            for (JSONObject o : notifications) if (!o.optBoolean("read", false)) unread++;
                            countListener.onCountReady(unread);
                        }
                    } catch (Exception e) {}
                }
                openPlayer(obj);
                dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }
        
        private void openPlayer(JSONObject obj) {
            try {
                JSONObject mObj = obj.getJSONObject("mediaItem");
                MediaItem item = new MediaItem();
                item.setId(mObj.optString("id"));
                item.setTitle(mObj.optString("title"));
                item.setPosterPath(mObj.optString("posterPath"));
                item.setBackdropPath(mObj.optString("backdropPath"));
                item.setVoteAverage(mObj.optDouble("voteAverage"));
                item.setType(mObj.optString("type"));
                item.setSource(mObj.optString("source"));
                
                Intent i = new Intent(getContext(), PlayerActivity.class);
                i.putExtra(Constants.EXTRA_ITEM, item);
                if (mObj.has("imdbId")) {
                    i.putExtra("imdb_id", mObj.optString("imdbId"));
                }
                if (mObj.has("tmdbId")) {
                    i.putExtra("tmdb_id", mObj.optString("tmdbId"));
                }
                if (obj.has("season")) {
                    i.putExtra(Constants.EXTRA_SEASON, obj.getInt("season"));
                }
                if (obj.has("episode")) {
                    i.putExtra(Constants.EXTRA_EPISODE, obj.getInt("episode"));
                }
                i.putExtra("show_comments", true);
                startActivity(i);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
