package com.example.smspro;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

// برای قابل انتخاب شدن به عنوان پیامک‌رسان پیش‌فرض لازم است
public class HeadlessSmsSendService extends Service {
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
