/*
This file is part of leafdigital kanjirecog.

kanjirecog is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

kanjirecog is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with kanjirecog.  If not, see <http://www.gnu.org/licenses/>.

Copyright 2011 Samuel Marshall.
*/
package com.leafdigital.kanji.android;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * Service that just displays the notification icon.
 *
 * TODO This should be a keyboard instead.
 */
public class IconService extends Service {
    private static final String CHANNEL_ID = "IconNotification";
    public static final int NOTIFICATION_ID = 1;
    private IBinder binder = new Binder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        NotificationManager notifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notifications != null) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.notificationchannel), NotificationManager.IMPORTANCE_DEFAULT);
                notifications.createNotificationChannel(channel);
            }
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.statusicon)
                .setTicker(getString(R.string.notificationtitle))
                .setWhen(0L)
                .setOngoing(true)
                .setContentTitle(getString(R.string.notificationtitle))
                .setContentText(getString(R.string.notificationtext))
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0))
                .build();
            notifications.notify(NOTIFICATION_ID, notification);
        } else {
            Log.e("Kanji draw", "Notification manager is null");
        }
    }

    public void onDestroy() {
        NotificationManager notifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notifications != null) {
            notifications.cancel(NOTIFICATION_ID);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notifications.deleteNotificationChannel(CHANNEL_ID);
            }
        } else {
            Log.e("Kanji draw", "Notification manager is null");
        }
    }
}
