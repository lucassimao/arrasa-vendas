package br.com.arrasavendas;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import br.com.arrasavendas.providers.CidadesProvider;
import br.com.arrasavendas.gcm.RegistrationIntentService;

import static br.com.arrasavendas.Utilities.ImageFolder.ANEXOS;
import static br.com.arrasavendas.Utilities.ImageFolder.PRODUTOS;

/**
 * Created by lsimaocosta on 13/01/15.
 */
public class Application extends android.app.Application {

    public static final String ARRASAVENDAS_AUTH_PREFS_KEY = "br.com.arrasavendas.auth";
    public static final int ENTREGAS_LOADER = 1;
    public final static int CIDADES_LOADER = 10;
    private static final String TAG = Application.class.getName();
    private static Application mApp = null;
    private String currentUser = null;
    private String accessToken;
    private String roles;

    public static Context context() {
        return mApp.getApplicationContext();
    }

    public static Application getInstance() {
        return mApp;
    }

    public static void salvarAuthToken(String username, String roles, String access_token) {
        SharedPreferences sp = context().getSharedPreferences(ARRASAVENDAS_AUTH_PREFS_KEY, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        editor.putString("username", username);
        editor.putString("roles", roles);
        editor.putString("access_token", access_token);

        editor.commit();

        Application.mApp.currentUser = username;
        Application.mApp.accessToken = access_token;
        Application.mApp.roles = roles;

        Application.mApp.registerGCM();
        Intent intent = new Intent(mApp, SyncEnderecosService.class);
        Application.mApp.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate ....");

        loadAuthenticationInfo();
        criarImagesProdutosDir();
        importCities();
        registrarSyncEnderecoAlarm();
        mApp = this;
        // se ja tiver logado, conecta logo no GCM
        registerGCM();
    }

    private void registerGCM() {
        if (isAuthenticated() && checkPlayServices()) {
            Log.d(TAG,"Play services ok, registrando app ...");
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return (resultCode == ConnectionResult.SUCCESS);
  /*      if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;*/
    }

    private void importCities() {
        Cursor c = getContentResolver().query(CidadesProvider.CONTENT_URI, null, null, null, null);
        // se não houver nenhuma cidade, preeenche
        if (c.getCount() == 0) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Application", "importing cities ...");
                    long start = System.currentTimeMillis();
                    Set<ContentValues> set = new HashSet<ContentValues>();
                    long qty = 0;

                    try {
                        InputStream is = getAssets().open("cidades");
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        String line = null;

                        while ((line = br.readLine()) != null) {
                            String[] values = line.split(",");

                            ContentValues cv = new ContentValues();
                            cv.put(CidadesProvider._ID, Integer.valueOf(values[0]));
                            cv.put(CidadesProvider.NOME, values[1]);
                            cv.put(CidadesProvider.UF, values[2]);
                            set.add(cv);
                        }
                        qty = getContentResolver().bulkInsert(CidadesProvider.CONTENT_URI,  set.toArray(new ContentValues[1]));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    long end = System.currentTimeMillis();
                    Log.d(TAG,  qty + " cities imported in " + (end - start) + "ms of " + set.size());

                }
            },"importar cidades").start();


        }

        c.close();
    }

    private void criarImagesProdutosDir() {

        File f = new File(PRODUTOS.getPath(this));
        if (!f.exists()) {
            f.mkdirs();
        }

        f = new File(ANEXOS.getPath(this));
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public boolean isAdmin() {
        String currentRoles = getRoles();
        return (!TextUtils.isEmpty(currentRoles) && currentRoles.contains("ROLE_ADMIN"));
    }

    public boolean isAuthenticated() {
        return !TextUtils.isEmpty(this.accessToken);
    }

    public String getRoles() {
        return this.roles;
    }

    private void loadAuthenticationInfo() {
        SharedPreferences sp = getSharedPreferences(ARRASAVENDAS_AUTH_PREFS_KEY, MODE_PRIVATE);
        this.currentUser = sp.getString("username", null);
        this.accessToken = sp.getString("access_token", null);
        this.roles = sp.getString("roles", null);

    }

    private void registrarSyncEnderecoAlarm() {
        Log.d(TAG," registrando o alarme p/ o SyncEnderecoService ....");

        long INTERVAL_3_HOURS = AlarmManager.INTERVAL_HOUR * 3;
        int requestCode = 1;

        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, SyncEnderecosService.class);
        PendingIntent alarmIntent = PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, INTERVAL_3_HOURS, alarmIntent);
    }
}
