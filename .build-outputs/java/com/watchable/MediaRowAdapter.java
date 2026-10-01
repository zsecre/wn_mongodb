package com.watchable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.watchable.R;
import java.util.ArrayList;
import java.util.List;

public class MediaRowAdapter extends RecyclerView.Adapter<MediaRowAdapter.VH> {
    public interface OnItemClick { void onClick(MediaItem item); }
    private final List<MediaItem> items;
    private final OnItemClick listener;

    public MediaRowAdapter(List<MediaItem> items, OnItemClick listener) {
        this.items = new ArrayList<MediaItem>(items);
        this.listener = listener;
    }

    public void updateItems(List<MediaItem> newItems) {
        items.clear(); items.addAll(newItems); notifyDataSetChanged();
    }

    public MediaRowAdapter.VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_card, parent, false);
        return new VH(v);
    }

    public void onBindViewHolder(MediaRowAdapter.VH h, int pos) {
        final MediaItem item = items.get(pos);
        Glide.with(h.itemView).load(item.getPosterPath())
            .transition(DrawableTransitionOptions.withCrossFade())
            .error(R.drawable.placeholder_poster).centerCrop().into(h.poster);
        h.title.setText(item.getTitle());
        h.rating.setText(item.getRatingText());
        h.itemView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { listener.onClick(item); }
        });
    }

    public int getItemCount() { return Math.min(items.size(), 10); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView poster; TextView title, rating;
        VH(View v) {
            super(v);
            poster = v.findViewById(R.id.card_poster);
            title  = v.findViewById(R.id.card_title);
            rating = v.findViewById(R.id.card_rating);
        }
    }
}
