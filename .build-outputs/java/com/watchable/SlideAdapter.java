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

public class SlideAdapter extends RecyclerView.Adapter<SlideAdapter.VH> {
    public interface OnClick { void onClick(MediaItem item); }
    private final List<MediaItem> items;
    private final OnClick listener;

    public SlideAdapter(List<MediaItem> items, OnClick listener) {
        this.items = new ArrayList<MediaItem>(items);
        this.listener = listener;
    }

    public void setItems(List<MediaItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
    }

    public SlideAdapter.VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slide, parent, false);
        return new VH(v);
    }

    public void onBindViewHolder(SlideAdapter.VH h, int pos) {
        final MediaItem item = items.get(pos);
        Glide.with(h.itemView).load(item.getBackdropPath())
            .transition(DrawableTransitionOptions.withCrossFade())
            .error(R.drawable.placeholder_backdrop).centerCrop().into(h.image);
        h.badge.setText(item.getType());
        h.title.setText(item.getTitle());
        h.rating.setText(item.getRatingText());
        h.itemView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { listener.onClick(item); }
        });
    }

    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView image; TextView badge, title, rating;
        VH(View v) {
            super(v);
            image  = v.findViewById(R.id.slide_image);
            badge  = v.findViewById(R.id.slide_badge);
            title  = v.findViewById(R.id.slide_title);
            rating = v.findViewById(R.id.slide_rating);
        }
    }
}
