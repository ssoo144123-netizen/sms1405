package com.example.smspro;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

public class SettingsActivity extends Activity {

    private Spinner spColor;
    private CheckBox cbDark;

    @Override
    protected void onCreate(Bundle state) {
        AppTheme.applyBase(this);
        super.onCreate(state);
        setContentView(R.layout.activity_settings);
        AppTheme.paint(this, findViewById(R.id.root));

        spColor = findViewById(R.id.spColor);
        cbDark = findViewById(R.id.cbDark);

        spColor.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, AppTheme.NAMES));
        spColor.setSelection(AppTheme.colorIndex(this));
        cbDark.setChecked(AppTheme.dark(this));
    }

    public void onSave(View v) {
        AppTheme.save(this, spColor.getSelectedItemPosition(), cbDark.isChecked());
        finish();
    }

    public void onBlacklist(View v) {
        startActivity(new android.content.Intent(this, BlacklistActivity.class));
    }
}
