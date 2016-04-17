package br.com.arrasavendas.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.R;

/**
 * Created by lsimaocosta on 27/03/16.
 */
public class RegistrationIntentService extends IntentService{

    private static final String TAG = RegistrationIntentService.class.getName();
    private final String SENT_TOKEN_TO_SERVER = "SENT_TOKEN_TO_SERVER";

    public RegistrationIntentService(){
        this("RegistrationIntentService");
    }
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public RegistrationIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        InstanceID instanceID = InstanceID.getInstance(this);
        try {
            String senderID = getString(R.string.gcm_defaultSenderId);
            String token = instanceID.getToken(senderID,
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

            Log.i(TAG, "GCM Registration Token: " + token);

//            sendRegistrationToServer(token);
            subscribeTopics(token);

//            sharedPreferences.edit().putBoolean(SENT_TOKEN_TO_SERVER, true).apply();

        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to complete token refresh", e);
//            sharedPreferences.edit().putBoolean(SENT_TOKEN_TO_SERVER, false).apply();
        }

//        Intent registrationComplete = new Intent(QuickstartPreferences.REGISTRATION_COMPLETE);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        List<String> topics =  new LinkedList<>();
        Application app = Application.getInstance();
        String username = app.getCurrentUser();

        topics.add("/topics/all");
        topics.add("/topics/" + username.replace("@","%"));

        if (app.isAdmin())
            topics.add("/topics/admin");

        for (String topic : topics) {
            pubSub.subscribe(token, topic, null);
        }
    }

}
