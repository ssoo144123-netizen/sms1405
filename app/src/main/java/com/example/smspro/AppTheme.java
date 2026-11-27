package com.example.smspro;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;

public class AppTheme {

    public static final String[] NAMES = {
            "آبی", "سبز", "بنفش", "نارنجی", "قرمز", "فیروزه‌ای",
            "صورتی", "نیلی", "قهوه‌ای", "خاکستری آبی", "سبز لیمویی", "طلایی"
    };
    private static final int[] COLORS = {
            0xFF1976D2, 0xFF388E3C, 0xFF7B1FA2, 0xFFF57C00,
            0xFFD32F2F, 0xFF00796B, 0xFFC2185B, 0xFF303F9F,
            0xFF5D4037, 0xFF455A64, 0xFF689F38, 0xFFFFA000
    };

    public static boolean dirty = false;

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("theme", Context.MODE_PRIVATE);
    }

    public static int colorIndex(Context c) { return prefs(c).getInt("color", 0); }
    public static boolean dark(Context c) { return prefs(c).getBoolean("dark", false); }
    public static int primary(Context c) { return COLORS[colorIndex(c)]; }
    public static int bg(Context c) { return dark(c) ? 0xFF121212 : 0xFFFFFFFF; }
    public static int text(Context c) { return dark(c) ? 0xFFEEEEEE : 0xFF000000; }
    public static int bubbleIn(Context c) { return dark(c) ? 0xFF2C2C2C : 0xFFE8E8E8; }
    public static int onPrimary() { return 0xFFFFFFFF; }

    public static void save(Context c, int index, boolean isDark) {
        prefs(c).edit().putInt("color", index).putBoolean("dark", isDark).apply();
        dirty = true;
    }

    public static void applyBase(Activity a) {
        a.setTheme(dark(a) ? android.R.style.Theme_Material : android.R.style.Theme_Material_Light);
    }

    public static void paint(Activity a, View root) {
        if (root != null) root.setBackgroundColor(bg(a));
        ActionBar ab = a.getActionBar();
        if (ab != null) ab.setBackgroundDrawable(new ColorDrawable(primary(a)));
        if (Build.VERSION.SDK_INT >= 21) a.getWindow().setStatusBarColor(primary(a));
    }
}
