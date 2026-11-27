package com.example.smspro;

import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends Activity {

    static class Msg {
        long id;
        String body;
        long date;
        int type;
        boolean read;
        boolean selected;
    }

    private String address = "";
    private final List<Msg> msgs = new ArrayList<>();
    private MsgAdapter adapter;
    private ListView list;
    private EditText edtBody;
    private EditText edtFind;

    @Override
    protected void onCreate(Bundle state) {
        AppTheme.applyBase(this);
        super.onCreate(state);
        setContentView(R.layout.activity_chat);
        AppTheme.paint(this, findViewById(R.id.root));

        String a = getIntent().getStringExtra("address");
        address = a == null ? "" : a;
        if (getActionBar() != null) getActionBar().setTitle(Util.contactName(this, address));

        list = findViewById(R.id.listMsgs);
        edtBody = findViewById(R.id.edtBody);
        edtFind = findViewById(R.id.edtFind);
        adapter = new MsgAdapter();
        list.setAdapter(adapter);
        list.setOnItemLongClickListener((p, v, pos, id) -> {
            msgMenu(msgs.get(pos));
            return true;
        });

        edtFind.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a1, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a1, int b, int c) { load(); }
            @Override public void afterTextChanged(Editable e) { }
        });

        markAllRead();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        String q = edtFind == null ? "" : edtFind.getText().toString().trim();
        String where = q.isEmpty() ? "address=?" : "address=? AND body LIKE ?";
        String[] args = q.isEmpty() ? new String[]{address}
                : new String[]{address, "%" + q + "%"};

        msgs.clear();
        try (Cursor c = getContentResolver().query(Telephony.Sms.CONTENT_URI,
                new String[]{Telephony.Sms._ID, Telephony.Sms.BODY, Telephony.Sms.DATE,
                        Telephony.Sms.TYPE, Telephony.Sms.READ},
                where, args, Telephony.Sms.DATE + " ASC")) {
            if (c != null) {
                while (c.moveToNext()) {
                    Msg m = new Msg();
                    m.id = c.getLong(0);
                    m.body = c.getString(1);
                    m.date = c.getLong(2);
                    m.type = c.getInt(3);
                    m.read = c.getInt(4) == 1;
                    msgs.add(m);
                }
            }
        }
        adapter.notifyDataSetChanged();
        if (!msgs.isEmpty()) list.setSelection(msgs.size() - 1);
    }

    private void markAllRead() {
        ContentValues v = new ContentValues();
        v.put(Telephony.Sms.READ, 1);
        try {
            getContentResolver().update(Telephony.Sms.CONTENT_URI, v,
                    "address=? AND read=0", new String[]{address});
        } catch (Exception ignored) { }
    }

    private List<Msg> selected() {
        List<Msg> r = new ArrayList<>();
        for (Msg m : msgs) if (m.selected) r.add(m);
        return r;
    }

    public void onSelectAll(View v) {
        boolean allOn = selected().size() == msgs.size();
        for (Msg m : msgs) m.selected = !allOn;
        adapter.notifyDataSetChanged();
    }

    public void onDelete(View v) {
        List<Msg> sel = selected();
        if (sel.isEmpty()) { Util.toast(this, "پیامکی انتخاب نشده"); return; }
        for (Msg m : sel) {
            getContentResolver().delete(Telephony.Sms.CONTENT_URI,
                    "_id=?", new String[]{String.valueOf(m.id)});
        }
        Util.toast(this, sel.size() + " پیامک حذف شد");
        load();
    }

    public void onMarkRead(View v) {
        List<Msg> sel = selected();
        if (sel.isEmpty()) { Util.toast(this, "پیامکی انتخاب نشده"); return; }
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.READ, 1);
        for (Msg m : sel) {
            getContentResolver().update(Telephony.Sms.CONTENT_URI, values,
                    "_id=?", new String[]{String.valueOf(m.id)});
        }
        load();
    }

    // فقط پیامک‌های تیک‌خورده ذخیره می‌شوند
    public void onSave(View v) {
        List<Msg> sel = selected();
        if (sel.isEmpty()) { Util.toast(this, "پیامکی انتخاب نشده"); return; }
        List<String> entries = new ArrayList<>();
        for (Msg m : sel) {
            boolean sent = m.type == Telephony.Sms.MESSAGE_TYPE_SENT;
            entries.add("[" + Util.date(m.date) + "] " + (sent ? "من" : address) + ":\n" + m.body);
        }
        try {
            String path = Util.saveBackup(entries);
            Util.toast(this, "ذخیره شد: " + path);
        } catch (IOException e) {
            Util.toast(this, "خطا در ذخیره: " + e.getMessage());
        }
    }

    // ارسال: اول پیامک ارسال می‌شود؛ ثبت در ارسال‌شده‌ها فقط در صورت پیش‌فرض بودن موفق است
    public void onSend(View v) {
        String body = edtBody.getText().toString().trim();
        if (body.isEmpty()) { Util.toast(this, "متن را وارد کنید"); return; }
        if (address.isEmpty()) { Util.toast(this, "شماره نامعتبر است"); return; }
        if (checkSelfPermission(android.Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Util.toast(this, "دسترسی ارسال پیامک داده نشده");
            return;
        }
        try {
            SmsManager sm = SmsManager.getDefault();
            sm.sendMultipartTextMessage(address, null, sm.divideMessage(body), null, null);
        } catch (Exception e) {
            Util.toast(this, "خطا در ارسال: " + e.getMessage());
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(Telephony.Sms.ADDRESS, address);
            values.put(Telephony.Sms.BODY, body);
            values.put(Telephony.Sms.DATE, System.currentTimeMillis());
            values.put(Telephony.Sms.READ, 1);
            values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT);
            getContentResolver().insert(Telephony.Sms.Sent.CONTENT_URI, values);
        } catch (Exception ignored) { }

        edtBody.setText("");
        Util.toast(this, "پیامک ارسال شد");
        load();
    }

    private class MsgAdapter extends BaseAdapter {
        @Override public int getCount() { return msgs.size(); }
        @Override public Object getItem(int p) { return msgs.get(p); }
        @Override public long getItemId(int p) { return msgs.get(p).id; }

        @Override
        public View getView(int p, View convertView, ViewGroup parent) {
            View v = LayoutInflater.from(ChatActivity.this).inflate(R.layout.item_message, parent, false);
            final Msg m = msgs.get(p);

            CheckBox cb = v.findViewById(R.id.cb);
            TextView txt = v.findViewById(R.id.txtMsg);
            TextView meta = v.findViewById(R.id.txtMeta);

            cb.setOnCheckedChangeListener(null);
            cb.setChecked(m.selected);
            cb.setOnCheckedChangeListener((b, on) -> m.selected = on);

            boolean sent = m.type == Telephony.Sms.MESSAGE_TYPE_SENT;
            txt.setText(m.body == null ? "" : m.body);
            txt.setBackgroundColor(sent ? AppTheme.primary(ChatActivity.this) : AppTheme.bubbleIn(ChatActivity.this));
            txt.setTextColor(sent ? AppTheme.onPrimary() : AppTheme.text(ChatActivity.this));

            meta.setText(Util.date(m.date) + (m.read ? "" : "  • نخوانده"));
            meta.setTextColor(AppTheme.text(ChatActivity.this));
            return v;
        }
    }

    public void onBlock(View v) {
        if (address.isEmpty()) { Util.toast(this, "شماره نامعتبر است"); return; }
        new android.app.AlertDialog.Builder(this)
                .setMessage("این شماره بلاک شود و همه پیامک‌هایش حذف شوند؟")
                .setPositiveButton("بلاک", (d, w) -> {
                    Blacklist.add(this, address);
                    getContentResolver().delete(Telephony.Sms.CONTENT_URI,
                            "address=?", new String[]{address});
                    Util.toast(this, "شماره بلاک شد");
                    finish();
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

    public void onCall(View v) {
        if (address.isEmpty()) { Util.toast(this, "شماره نامعتبر است"); return; }
        Util.call(this, address);
    }

    private void msgMenu(final Msg m) {
        String[] items = {"کپی", "فوروارد", "حذف", "علامت خوانده", "جزئیات"};
        new android.app.AlertDialog.Builder(this)
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0: {
                            android.content.ClipboardManager cm =
                                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("sms", m.body));
                            Util.toast(this, "کپی شد");
                            break;
                        }
                        case 1:
                            forwardDialog(m.body);
                            break;
                        case 2:
                            getContentResolver().delete(Telephony.Sms.CONTENT_URI,
                                    "_id=?", new String[]{String.valueOf(m.id)});
                            load();
                            break;
                        case 3: {
                            ContentValues values = new ContentValues();
                            values.put(Telephony.Sms.READ, 1);
                            getContentResolver().update(Telephony.Sms.CONTENT_URI, values,
                                    "_id=?", new String[]{String.valueOf(m.id)});
                            load();
                            break;
                        }
                        case 4: {
                            boolean sent = m.type == Telephony.Sms.MESSAGE_TYPE_SENT;
                            new android.app.AlertDialog.Builder(this)
                                    .setTitle("جزئیات پیام")
                                    .setMessage("طرف گفتگو: " + address
                                            + "\nزمان: " + Util.date(m.date)
                                            + "\nنوع: " + (sent ? "ارسالی" : "دریافتی")
                                            + "\nخوانده شده: " + (m.read ? "بله" : "خیر"))
                                    .setPositiveButton("بستن", null)
                                    .show();
                            break;
                        }
                    }
                })
                .show();
    }

    private void forwardDialog(final String body) {
        final EditText edt = new EditText(this);
        edt.setHint("شماره مقصد");
        edt.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        new android.app.AlertDialog.Builder(this)
                .setTitle("فوروارد پیام")
                .setView(edt)
                .setPositiveButton("ارسال", (d, w) -> {
                    String num = edt.getText().toString().trim();
                    if (num.isEmpty()) return;
                    Util.toast(this, Util.send(this, num, body) ? "فوروارد شد" : "خطا در فوروارد");
                })
                .setNegativeButton("انصراف", null)
                .show();
    }
}
