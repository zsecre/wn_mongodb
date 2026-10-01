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

public class GridAdapter extends RecyclerView.Adapter<GridAdapter.VH> {
    public interface OnItemClick { void onClick(MediaItem item); }
    private final List<MediaItem> items = new ArrayList<MediaItem>();
    private final OnItemClick listener;

    public GridAdapter(OnItemClick listener) { this.listener = listener; }

    public void setItems(List<MediaItem> newItems) {
        items.clear();
        if (newItems != null) {
            for (MediaItem item : newItems) {
                boolean exists = false;
                for (MediaItem existing : items) {
                    if (existing.getId() != null && existing.getId().equals(item.getId()) 
                            && existing.getSource() != null && existing.getSource().equals(item.getSource())) {
                        exists = true; break;
                    }
                }
                if (!exists) items.add(item);
            }
        }
        notifyDataSetChanged();
    }

    public void addItems(List<MediaItem> more) {
        if (more == null || more.isEmpty()) return;
        List<MediaItem> toAdd = new ArrayList<MediaItem>();
        for (MediaItem item : more) {
            boolean exists = false;
            for (MediaItem existing : items) {
                if (existing.getId() != null && existing.getId().equals(item.getId()) 
                        && existing.getSource() != null && existing.getSource().equals(item.getSource())) {
                    exists = true; break;
                }
            }
            if (!exists) toAdd.add(item);
        }
        if (!toAdd.isEmpty()) {
            int start = items.size();
            items.addAll(toAdd);
            notifyItemRangeInserted(start, toAdd.size());
        }
    }

    public GridAdapter.VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grid_card, parent, false);
        return new VH(v);
    }

    public void onBindViewHolder(GridAdapter.VH h, int pos) {
        final MediaItem item = items.get(pos);
        Glide.with(h.itemView).load(item.getPosterPath())
            .transition(DrawableTransitionOptions.withCrossFade())
            .error(R.drawable.placeholder_poster).centerCrop().into(h.poster);
        h.title.setText(item.getTitle());
        h.rating.setText(item.getRatingText());
        h.type.setText(item.getType());
        h.itemView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { listener.onClick(item); }
        });
    }

    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView poster; TextView title, rating, type;
        VH(View v) {
            super(v);
            poster = v.findViewById(R.id.grid_poster);
            title  = v.findViewById(R.id.grid_title);
            rating = v.findViewById(R.id.grid_rating);
            type   = v.findViewById(R.id.grid_type);
        }
    }
}
