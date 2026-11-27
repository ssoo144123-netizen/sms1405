package com.example.smspro;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlacklistActivity extends Activity {

    private final List<String> items = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private EditText edtNumber;

    @Override
    protected void onCreate(Bundle state) {
        AppTheme.applyBase(this);
        super.onCreate(state);
        setContentView(R.layout.activity_blacklist);
        AppTheme.paint(this, findViewById(R.id.root));

        edtNumber = findViewById(R.id.edtNumber);
        ListView list = findViewById(R.id.listBlocked);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        list.setAdapter(adapter);

        list.setOnItemClickListener((p, v, pos, id) -> {
            String num = items.get(pos);
            Blacklist.remove(this, num);
            Util.toast(this, "از بلاک‌لیست حذف شد: " + num);
            refresh();
        });

        refresh();
    }

    private void refresh() {
        items.clear();
        items.addAll(Blacklist.getAll(this));
        Collections.sort(items);
        adapter.notifyDataSetChanged();
    }

    public void onAdd(View v) {
        String num = edtNumber.getText().toString().trim();
        if (Blacklist.normalize(num).isEmpty()) {
            Util.toast(this, "شماره نامعتبر است");
            return;
        }
        Blacklist.add(this, num);
        edtNumber.setText("");
        Util.toast(this, "به بلاک‌لیست اضافه شد");
        refresh();
    }
}
