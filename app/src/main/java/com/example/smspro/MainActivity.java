package com.example.smspro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    static class Conv {
        String address;
        String body;
        long date;
        int unread;
        int total;
        boolean selected;
    }

    private final List<Conv> all = new ArrayList<>();
    private final List<Conv> shown = new ArrayList<>();
    private ConvAdapter adapter;
    private Spinner spinner;
    private TextView info;
    private EditText edtSearch;

    @Override
    protected void onCreate(Bundle state) {
        AppTheme.applyBase(this);
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        AppTheme.paint(this, findViewById(R.id.root));

        info = findViewById(R.id.txtInfo);
        spinner = findViewById(R.id.spinnerFilter);
        edtSearch = findViewById(R.id.edtSearch);
        ListView list = findViewById(R.id.listView);

        adapter = new ConvAdapter();
        list.setAdapter(adapter);

        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"همه", "خوانده‌نشده", "خوانده‌شده"}));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { applyFilter(); }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { if (hasPerms()) load(); }
            @Override public void afterTextChanged(Editable e) { }
        });

        list.setOnItemClickListener((p, v, pos, id) -> {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("address", shown.get(pos).address);
            startActivity(i);
        });

        list.setOnItemLongClickListener((p, v, pos, id) -> {
            convMenu(shown.get(pos));
            return true;
        });
        if (!hasPerms()) requestPermissions(requiredPerms(), 1);
        if (!isDefaultSms()) askDefaultSms();
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        if (hasPerms()) load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppTheme.dirty) {
            AppTheme.dirty = false;
            recreate();
        } else if (hasPerms()) {
            load();
        }
    }

    private boolean hasPerms() {
        return checkSelfPermission(android.Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private String[] requiredPerms() {
        List<String> p = new ArrayList<>(Arrays.asList(
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.RECEIVE_SMS,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_CONTACTS));
        if (Build.VERSION.SDK_INT >= 33) p.add("android.permission.POST_NOTIFICATIONS");
        return p.toArray(new String[0]);
    }

    // گروه‌بندی بر اساس شماره؛ با جستجو فقط پیامک‌های مطابق بررسی می‌شوند
    private void load() {
        String q = edtSearch == null ? "" : edtSearch.getText().toString().trim();
        String sel = q.isEmpty() ? null : "body LIKE ? OR address LIKE ?";
        String[] args = q.isEmpty() ? null : new String[]{"%" + q + "%", "%" + q + "%"};

        Map<String, Conv> map = new LinkedHashMap<>();
        try (Cursor c = getContentResolver().query(Telephony.Sms.CONTENT_URI,
                new String[]{Telephony.Sms.ADDRESS, Telephony.Sms.BODY,
                        Telephony.Sms.DATE, Telephony.Sms.READ},
                sel, args, Telephony.Sms.DATE + " DESC")) {
            if (c != null) {
                while (c.moveToNext()) {
                    String addr = c.getString(0) == null ? "" : c.getString(0);
                    Conv cv = map.get(addr);
                    if (cv == null) {
                        cv = new Conv();
                        cv.address = addr;
                        cv.body = c.getString(1);
                        cv.date = c.getLong(2);
                        map.put(addr, cv);
                    }
                    cv.total++;
                    if (c.getInt(3) == 0) cv.unread++;
                }
            }
        }
        all.clear();
        all.addAll(map.values());
        applyFilter();
    }

    private void applyFilter() {
        int pos = spinner.getSelectedItemPosition();
        shown.clear();
        for (Conv c : all) {
            if (pos == 0 || (pos == 1 && c.unread > 0) || (pos == 2 && c.unread == 0)) {
                shown.add(c);
            }
        }
        adapter.notifyDataSetChanged();
        updateInfo();
    }

    private List<Conv> selectedConvs() {
        List<Conv> r = new ArrayList<>();
        for (Conv c : shown) if (c.selected) r.add(c);
        return r;
    }

    private void updateInfo() {
        info.setText("گفتگوها: " + shown.size() + "  |  انتخاب‌شده: " + selectedConvs().size());
    }

    public void onSelectAll(View v) {
        boolean allOn = selectedConvs().size() == shown.size();
        for (Conv c : shown) c.selected = !allOn;
        adapter.notifyDataSetChanged();
        updateInfo();
    }

    public void onDelete(View v) {
        final List<Conv> sel = selectedConvs();
        if (sel.isEmpty()) { Util.toast(this, "گفتگویی انتخاب نشده"); return; }
        new AlertDialog.Builder(this)
                .setMessage("حذف " + sel.size() + " گفتگو با همه پیامک‌هایش؟ قابل بازگشت نیست.")
                .setPositiveButton("حذف", (d, w) -> {
                    for (Conv c : sel) {
                        getContentResolver().delete(Telephony.Sms.CONTENT_URI,
                                "address=?", new String[]{c.address});
                    }
                    Util.toast(this, "حذف شد");
                    load();
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

    public void onMarkRead(View v) { setRead(true); }
    public void onMarkUnread(View v) { setRead(false); }

    private void setRead(boolean read) {
        List<Conv> sel = selectedConvs();
        if (sel.isEmpty()) { Util.toast(this, "گفتگویی انتخاب نشده"); return; }
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.READ, read ? 1 : 0);
        for (Conv c : sel) {
            getContentResolver().update(Telephony.Sms.CONTENT_URI, values,
                    "address=?", new String[]{c.address});
        }
        load();
    }

    // ذخیره فقط پیامک‌های گفتگوهای تیک‌خورده (و در صورت جستجو، همان پیامک‌های مطابق)
    public void onSave(View v) {
        List<Conv> sel = selectedConvs();
        if (sel.isEmpty()) { Util.toast(this, "گفتگویی انتخاب نشده"); return; }

        String q = edtSearch.getText().toString().trim();
        List<String> entries = new ArrayList<>();
        for (Conv c : sel) {
            StringBuilder sb = new StringBuilder("گفتگو با " + c.address + "\n");
            String where = q.isEmpty() ? "address=?" : "address=? AND body LIKE ?";
            String[] args = q.isEmpty() ? new String[]{c.address}
                    : new String[]{c.address, "%" + q + "%"};
            try (Cursor cur = getContentResolver().query(Telephony.Sms.CONTENT_URI,
                    new String[]{Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE},
                    where, args, Telephony.Sms.DATE + " ASC")) {
                if (cur != null) {
                    while (cur.moveToNext()) {
                        boolean sent = cur.getInt(2) == Telephony.Sms.MESSAGE_TYPE_SENT;
                        sb.append("[").append(Util.date(cur.getLong(1))).append("] ")
                                .append(sent ? "من" : c.address).append(": ")
                                .append(cur.getString(0)).append("\n");
                    }
                }
            }
            entries.add(sb.toString());
        }
        try {
            String path = Util.saveBackup(entries);
            Util.toast(this, "ذخیره شد: " + path);
        } catch (IOException e) {
            Util.toast(this, "خطا در ذخیره: " + e.getMessage());
        }
    }

    public void onNewChat(View v) {
        final EditText edt = new EditText(this);
        edt.setHint("شماره گیرنده");
        edt.setInputType(InputType.TYPE_CLASS_PHONE);
        new AlertDialog.Builder(this)
                .setTitle("گفتگوی جدید")
                .setView(edt)
                .setPositiveButton("باز کردن", (d, w) -> {
                    String num = edt.getText().toString().trim();
                    if (num.isEmpty()) return;
                    Intent i = new Intent(this, ChatActivity.class);
                    i.putExtra("address", num);
                    startActivity(i);
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

    public void onSetDefault(View v) {
        if (getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this))) {
            Util.toast(this, "این برنامه هم‌اکنون پیش‌فرض است");
            return;
        }
        Intent i = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        i.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
        startActivity(i);
    }

    public void onSettings(View v) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private class ConvAdapter extends BaseAdapter {
        @Override public int getCount() { return shown.size(); }
        @Override public Object getItem(int p) { return shown.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int p, View convertView, ViewGroup parent) {
            View v = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_conv, parent, false);
            final Conv c = shown.get(p);

            CheckBox cb = v.findViewById(R.id.cb);
            TextView title = v.findViewById(R.id.title);
            TextView sub = v.findViewById(R.id.sub);
            TextView badge = v.findViewById(R.id.badge);

            cb.setOnCheckedChangeListener(null);
            cb.setChecked(c.selected);
            cb.setOnCheckedChangeListener((b, on) -> {
                c.selected = on;
                updateInfo();
            });

            int fg = AppTheme.text(MainActivity.this);
            title.setText(Util.contactName(MainActivity.this, c.address));
            title.setTextColor(fg);
            sub.setText(c.body == null ? "" : c.body);
            sub.setTextColor(fg);

            if (c.unread > 0) {
                badge.setVisibility(View.VISIBLE);
                badge.setText(String.valueOf(c.unread));
                badge.setBackgroundColor(AppTheme.primary(MainActivity.this));
            } else {
                badge.setVisibility(View.GONE);
            }

            v.setBackgroundColor(AppTheme.bg(MainActivity.this));
            return v;
        }
    }

    private boolean isDefaultSms() {
        return getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
    }

    private void askDefaultSms() {
        new AlertDialog.Builder(this)
                .setTitle("پیامک‌رسان پیش‌فرض")
                .setMessage("برای دریافت، حذف و ثبت پیامک‌ها باید SmsPro پیامک‌رسان پیش‌فرض باشد. الان تنظیم کنید؟")
                .setPositiveButton("تنظیم", (d, w) -> {
                    Intent i = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                    i.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
                    startActivity(i);
                })
                .setNegativeButton("بعداً", null)
                .show();
    }

    private void convMenu(final Conv c) {
        String[] items = {"باز کردن", "تماس", "بلاک و حذف", "حذف گفتگو",
                "علامت خوانده", "علامت نخوانده", "ذخیره این گفتگو"};
        new AlertDialog.Builder(this)
                .setTitle(Util.contactName(this, c.address))
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0: {
                            Intent i = new Intent(this, ChatActivity.class);
                            i.putExtra("address", c.address);
                            startActivity(i);
                            break;
                        }
                        case 1:
                            Util.call(this, c.address);
                            break;
                        case 2:
                            Blacklist.add(this, c.address);
                            getContentResolver().delete(Telephony.Sms.CONTENT_URI,
                                    "address=?", new String[]{c.address});
                            Util.toast(this, "بلاک شد");
                            load();
                            break;
                        case 3:
                            getContentResolver().delete(Telephony.Sms.CONTENT_URI,
                                    "address=?", new String[]{c.address});
                            Util.toast(this, "گفتگو حذف شد");
                            load();
                            break;
                        case 4:
                        case 5: {
                            ContentValues values = new ContentValues();
                            values.put(Telephony.Sms.READ, which == 4 ? 1 : 0);
                            getContentResolver().update(Telephony.Sms.CONTENT_URI, values,
                                    "address=?", new String[]{c.address});
                            load();
                            break;
                        }
                        case 6:
                            for (Conv x : shown) x.selected = (x == c);
                            onSave(null);
                            break;
                    }
                })
                .show();
    }
}
