package com.example.smspro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SmsReceiver extends BroadcastReceiver {

    private static final String CHANNEL = "smspro_sms";

    @Override
    public void onReceive(Context context, Intent intent) {
        SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (msgs == null || msgs.length == 0) return;

        Map<String, StringBuilder> bodies = new LinkedHashMap<>();
        Map<String, Long> times = new HashMap<>();
        for (SmsMessage m : msgs) {
            String from = m.getDisplayOriginatingAddress();
            if (from == null) from = "";
            if (!bodies.containsKey(from)) {
                bodies.put(from, new StringBuilder());
                times.put(from, m.getTimestampMillis());
            }
            bodies.get(from).append(m.getDisplayMessageBody());
        }

        for (Map.Entry<String, StringBuilder> e : bodies.entrySet()) {
            String from = e.getKey();
            String body = e.getValue().toString();
            if (Blacklist.isBlocked(context, from)) continue;

            ContentValues v = new ContentValues();
            v.put(Telephony.Sms.ADDRESS, from);
            v.put(Telephony.Sms.BODY, body);
            v.put(Telephony.Sms.DATE, times.get(from));
            v.put(Telephony.Sms.READ, 0);
            v.put(Telephony.Sms.SEEN, 0);
            v.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);
            try {
                context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, v);
            } catch (Exception ignored) { }

            notifyUser(context, from, body);
        }
    }

    private void notifyUser(Context ctx, String from, String body) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Intent open = new Intent(ctx, ChatActivity.class);
        open.putExtra("address", from);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, from.hashCode(), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "پیامک", NotificationManager.IMPORTANCE_HIGH));
            b = new Notification.Builder(ctx, CHANNEL);
        } else {
            b = new Notification.Builder(ctx);
        }
        b.setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(from)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pi);
        nm.notify(from.hashCode(), b.build());
    }
}
