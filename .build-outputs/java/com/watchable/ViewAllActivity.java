package com.watchable;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.watchable.R;
import java.util.List;

public class ViewAllActivity extends AppCompatActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewall);

        String title = getIntent().getStringExtra(Constants.EXTRA_TITLE);
        final List<MediaItem> items = (List<MediaItem>) getIntent().getSerializableExtra(Constants.EXTRA_ITEMS);

        ((TextView) findViewById(R.id.viewall_title)).setText(title != null ? title : "");

        findViewById(R.id.viewall_back_btn).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        RecyclerView rv = findViewById(R.id.viewall_recycler);
        rv.setLayoutManager(new GridLayoutManager(this, 3));

        GridAdapter adapter = new GridAdapter(new GridAdapter.OnItemClick() {
            public void onClick(MediaItem item) {
                Intent i = new Intent(ViewAllActivity.this, DetailsActivity.class);
                i.putExtra(Constants.EXTRA_ITEM, item);
                startActivity(i);
            }
        });
        rv.setAdapter(adapter);
        if (items != null) adapter.setItems(items);
    }
}
