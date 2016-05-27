package br.com.arrasavendas.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import java.util.Random;

import br.com.arrasavendas.R;

/**
 * Created by lsimaocosta on 27/03/16.
 */
public class MsgListenerService extends GcmListenerService {

    public static final String TITLE = "title";
    public static final String MESSAGE = "message";
    private static final String TAG = MsgListenerService.class.getName();
    private static final Random random = new Random(System.currentTimeMillis());


    @Override
    public void onMessageReceived(String from, Bundle data) {
        String resumo = data.getString("resumo");
        String message = data.getString("message");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);
        Log.d(TAG, "Data: " +data.toString());

        switch (message) {
            case "DELETE":
                break;
            case "UPDATE":
                break;
            default:
                sendNotification(resumo, message);
                break;
        }

    }


    private void sendNotification(String resumo, String message) {

        Intent intent = new Intent(this, NotificationPopUp.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(TITLE, "Alertas Arrasa Amiga");
        intent.putExtra(MESSAGE, message);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,random.nextInt(),
                intent,PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Arrasa Amiga")
                .setContentText(resumo)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(random.nextInt(), notificationBuilder.build());
    }
}
