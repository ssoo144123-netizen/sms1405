package com.example.smspro;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class Blacklist {

    private static final String PREF = "blacklist";
    private static final String KEY = "numbers";

    // فقط ۱۰ رقم آخر نگه داشته می‌شود تا +98 و 09 یکسان شوند
    public static String normalize(String number) {
        if (number == null) return "";
        String digits = number.replaceAll("[^0-9]", "");
        if (digits.length() > 10) digits = digits.substring(digits.length() - 10);
        return digits;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static Set<String> getAll(Context c) {
        Set<String> s = prefs(c).getStringSet(KEY, null);
        return s == null ? new HashSet<String>() : new HashSet<String>(s);
    }

    public static void add(Context c, String number) {
        String n = normalize(number);
        if (n.isEmpty()) return;
        Set<String> s = getAll(c);
        s.add(n);
        prefs(c).edit().putStringSet(KEY, s).apply();
    }

    public static void remove(Context c, String number) {
        Set<String> s = getAll(c);
        s.remove(normalize(number));
        prefs(c).edit().putStringSet(KEY, s).apply();
    }

    public static boolean isBlocked(Context c, String number) {
        String n = normalize(number);
        return !n.isEmpty() && getAll(c).contains(n);
    }
}
