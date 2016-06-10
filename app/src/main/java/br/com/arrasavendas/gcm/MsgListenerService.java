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

import br.com.arrasavendas.Application;
import br.com.arrasavendas.R;
import br.com.arrasavendas.service.VendaService;

import static br.com.arrasavendas.Application.ESTOQUES_LAST_UPDATED_KEY;
import static br.com.arrasavendas.Application.VENDAS_LAST_UPDATED_KEY;
import static br.com.arrasavendas.Application.CLIENTE_ID_KEY;


/**
 * Created by lsimaocosta on 27/03/16.
 */
public class MsgListenerService extends GcmListenerService {

    public static final String TITLE = "title";
    public static final String MESSAGE = "message";
    private static final String TAG = MsgListenerService.class.getName();


    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("message");

        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);
        Log.d(TAG, "Data: " + data.toString());

        switch (message) {
            case "DELETE":
                handleDelete(data);
                break;
            case "UPDATE":
                handleUpdate(data);
                break;
            default:
                String resumo = data.getString("resumo");
                sendNotification(resumo, message);
                break;
        }

    }

    private void handleDelete(Bundle data) {
        String entity = data.getString("entity", "");
        long id = Long.valueOf(data.getString("id"));

        String clienteId = data.getString(CLIENTE_ID_KEY, null);
        Application app = Application.getInstance();

        if (!app.getId().equals(clienteId)) {

            switch (entity) {
                case "Venda":
                    new VendaService(getApplication()).delete(id);
                    break;
                case "Estoque":
                    Log.d(TAG, "Exclusao p/ estoque #" + id + " não era pra aparecer aqui");
                    break;
                default:
                    Log.d(TAG, "Delete não identificou entidade " + entity);
            }

        }
    }

    private void handleUpdate(Bundle data) {
        String clienteId = data.getString(CLIENTE_ID_KEY, null);
        Application app = Application.getInstance();

        synchronized (Application.class) {

            if (data.containsKey(ESTOQUES_LAST_UPDATED_KEY)) {

                long lastUpdated = Long.valueOf(data.getString(ESTOQUES_LAST_UPDATED_KEY));

                if (!app.getId().equals(clienteId))
                    Application.setEstoquesLastUpdated(lastUpdated);
                else
                    Log.d(TAG, "estoquesLastUpdated ja conhecido ... ignorando");
            }

            if (data.containsKey(VENDAS_LAST_UPDATED_KEY)) {
                long lastUpdated = Long.valueOf(data.getString(VENDAS_LAST_UPDATED_KEY));

                if (!app.getId().equals(clienteId))
                    Application.setVendasLastUpdated(lastUpdated);
                else
                    Log.d(TAG, "vendasLastUpdated ja conhecido ... ignorando");
            }
        }
    }

    private final void sendNotification(String resumo, String message) {

        Random random = new Random(System.currentTimeMillis());

        Intent intent = new Intent(this, NotificationPopUp.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(TITLE, "Alertas Arrasa Amiga");
        intent.putExtra(MESSAGE, message);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, random.nextInt(),
                intent, PendingIntent.FLAG_ONE_SHOT);

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
