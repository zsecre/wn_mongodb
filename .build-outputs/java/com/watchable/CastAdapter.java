package com.watchable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.watchable.R;
import java.util.List;

public class CastAdapter extends RecyclerView.Adapter<CastAdapter.VH> {
    private final List<CastMember> items;
    public CastAdapter(List<CastMember> items) { this.items = items; }

    public CastAdapter.VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cast, parent, false);
        return new VH(v);
    }

    public void onBindViewHolder(CastAdapter.VH h, int pos) {
        CastMember cm = items.get(pos);
        h.name.setText(cm.getName());
        if (cm.getProfilePath() != null && !cm.getProfilePath().isEmpty())
            Glide.with(h.itemView).load(cm.getProfilePath()).circleCrop().error(R.drawable.placeholder_avatar).into(h.avatar);
        else
            h.avatar.setImageResource(R.drawable.placeholder_avatar);
    }

    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView avatar; TextView name;
        VH(View v) {
            super(v);
            avatar = v.findViewById(R.id.cast_avatar);
            name   = v.findViewById(R.id.cast_name);
        }
    }
}
