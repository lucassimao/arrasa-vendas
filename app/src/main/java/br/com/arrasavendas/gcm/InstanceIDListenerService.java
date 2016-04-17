package br.com.arrasavendas.gcm;

import android.content.Intent;
import android.util.Log;

/**
 * Created by lsimaocosta on 27/03/16.
 */
public class InstanceIDListenerService extends com.google.android.gms.iid.InstanceIDListenerService {

    private static final String TAG = InstanceIDListenerService.class.getName();

    @Override
    public void onTokenRefresh() {
        Log.d(TAG,"onTokenRefresh");
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }
}
