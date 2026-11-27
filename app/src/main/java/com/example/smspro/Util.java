package com.example.smspro;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Util {

    public static String date(long ms) {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(new Date(ms));
    }

    public static void toast(Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
    }

    public static String saveBackup(List<String> entries) throws IOException {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        dir.mkdirs();
        String name = "smspro_backup_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".txt";
        File f = new File(dir, name);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8")) {
            for (String e : entries) {
                w.write(e + "\n----------\n\n");
            }
        }
        return f.getAbsolutePath();
    }

    // نام مخاطب از دفترچه تلفن؛ اگر پیدا نشد یا دسترسی نبود، همان شماره
    public static String contactName(Context c, String number) {
        if (number == null || number.isEmpty()) return "ناشناس";
        if (c.checkSelfPermission(android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) return number;
        try (Cursor cur = c.getContentResolver().query(
                Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)),
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null)) {
            if (cur != null && cur.moveToFirst()) return cur.getString(0);
        } catch (Exception ignored) { }
        return number;
    }

    public static void call(Context c, String number) {
        Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
        c.startActivity(i);
    }

    // ارسال پیامک و ثبت در ارسال‌شده‌ها
    public static boolean send(Context c, String to, String body) {
        try {
            SmsManager sm = SmsManager.getDefault();
            sm.sendMultipartTextMessage(to, null, sm.divideMessage(body), null, null);
        } catch (Exception e) {
            return false;
        }
        try {
            ContentValues v = new ContentValues();
            v.put(Telephony.Sms.ADDRESS, to);
            v.put(Telephony.Sms.BODY, body);
            v.put(Telephony.Sms.DATE, System.currentTimeMillis());
            v.put(Telephony.Sms.READ, 1);
            v.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT);
            c.getContentResolver().insert(Telephony.Sms.Sent.CONTENT_URI, v);
        } catch (Exception ignored) { }
        return true;
    }
}
