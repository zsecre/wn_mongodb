package com.watchable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.watchable.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.VH> {
    public interface OnClick { void onClick(Episode ep); }
    private final List<Episode> items = new ArrayList<Episode>();
    private final OnClick listener;
    private int playingEpisode = -1;
    private int playingSeason = -1;
    private String posterPath = null;

    public EpisodeAdapter(OnClick listener) { this.listener = listener; }

    public void setItems(List<Episode> list) {
        items.clear(); items.addAll(list); notifyDataSetChanged();
    }
    
    public void setPosterPath(String path) {
        this.posterPath = path;
    }

    public void setPlayingEpisode(int season, int ep) {
        this.playingSeason = season;
        this.playingEpisode = ep;
        notifyDataSetChanged();
    }

    public EpisodeAdapter.VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_episode, parent, false);
        return new VH(v);
    }

    public void onBindViewHolder(EpisodeAdapter.VH h, int pos) {
        final Episode ep = items.get(pos);
        h.epNum.setText("EP " + ep.getEpisodeNumber());
        h.title.setText(ep.getName());
        h.date.setText(ep.getAirDate());

        String imagePath = ep.getStillPath();
        if ((imagePath == null || imagePath.isEmpty() || "null".equals(imagePath)) && posterPath != null && !posterPath.isEmpty() && !"null".equals(posterPath)) {
            if (posterPath.startsWith("http://") || posterPath.startsWith("https://")) {
                imagePath = posterPath;
            } else {
                imagePath = "https://image.tmdb.org/t/p/w500" + posterPath;
            }
        }

        if (imagePath != null && !imagePath.isEmpty()) {
            Glide.with(h.itemView).load(imagePath).error(R.drawable.placeholder_backdrop).centerCrop().into(h.thumb);
        } else {
            h.thumb.setImageResource(R.drawable.placeholder_backdrop);
        }

        if (ep.getEpisodeNumber() == playingEpisode && (playingSeason == -1 || ep.getSeasonNumber() == playingSeason)) {
            h.title.setTextColor(0xFF00FFFF);
            h.card.setStrokeColor(0xFF00FFFF);
            h.card.setStrokeWidth(2);
            h.playingOverlay.setVisibility(View.VISIBLE);
        } else {
            h.title.setTextColor(0xFFE4E4E7);
            h.card.setStrokeColor(0xFF27272A);
            h.card.setStrokeWidth(1);
            h.playingOverlay.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { listener.onClick(ep); }
        });
    }

    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView thumb;
        TextView epNum, title, date;
        LinearLayout playingOverlay;
        VH(View v) {
            super(v);
            card = v.findViewById(R.id.ep_card);
            thumb = v.findViewById(R.id.ep_thumb);
            epNum = v.findViewById(R.id.ep_number);
            title = v.findViewById(R.id.ep_title);
            date  = v.findViewById(R.id.ep_date);
            playingOverlay = v.findViewById(R.id.ep_playing_overlay);
        }
    }
}
