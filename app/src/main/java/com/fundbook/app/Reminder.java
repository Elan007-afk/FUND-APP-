package com.fundbook.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Locale;

/**
 * Fires every Saturday at the chosen time (default 9:00 AM), shows a notification,
 * then schedules the next Saturday. Also re-schedules after reboot and app update.
 */
public class Reminder extends BroadcastReceiver {

    static final String CHANNEL = "saturday_collection";
    static final String ACTION_SATURDAY = "com.fundbook.app.SATURDAY";

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("fundbook", Context.MODE_PRIVATE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_SATURDAY.equals(action)) {
            show(context);
        }
        schedule(context);
    }

    /** Schedules the next Saturday reminder (replaces any existing one). */
    static void schedule(Context c) {
        SharedPreferences p = prefs(c);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, p.getInt("h", 9));
        cal.set(Calendar.MINUTE, p.getInt("m", 0));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int diff = (Calendar.SATURDAY - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7;
        cal.add(Calendar.DAY_OF_YEAR, diff);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 7);
        }

        Intent i = new Intent(c, Reminder.class);
        i.setAction(ACTION_SATURDAY);
        PendingIntent pi = PendingIntent.getBroadcast(
                c, 100, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
    }

    /** Shows the collection-day notification now. */
    static void show(Context c) {
        NotificationManager nm =
                (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(
                CHANNEL, "Saturday collection", NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(ch);

        SharedPreferences p = prefs(c);
        int count = p.getInt("pc", 0);
        float amount = p.getFloat("pa", 0f);

        String text;
        if (count > 0) {
            text = String.format(Locale.US,
                    "%d pending, \u20B9%,.0f to collect. Tap to update payments.", count, amount);
        } else {
            text = "Time to collect this week's fund. Tap to mark payments.";
        }

        Intent open = new Intent(c, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                c, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Saturday fund collection")
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

        nm.notify(1, n);
    }
}
