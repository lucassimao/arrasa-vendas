package br.com.arrasavendas;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import br.com.arrasavendas.providers.CidadesProvider;

import static br.com.arrasavendas.Utilities.ImageFolder.ANEXOS;
import static br.com.arrasavendas.Utilities.ImageFolder.PRODUTOS;

/**
 * Created by lsimaocosta on 13/01/15.
 */
public class Application extends android.app.Application {

    public static final String ARRASAVENDAS_AUTH_PREFS_KEY = "br.com.arrasavendas.auth";
    private String currentUser = null;
    private String accessToken;
    private String roles;
    private static Application mApp = null;


    public static final int ENTREGAS_LOADER = 1;
    public final static int CIDADES_LOADER=10;


    @Override
    public void onCreate() {
        super.onCreate();
        loadAuthenticationInfo();
        criarImagesProdutosDir();
        importCities();
        mApp = this;

/*        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());*/
    }

    private void importCities() {
        Cursor c = getContentResolver().query(CidadesProvider.CONTENT_URI,null,null,null,null);
        if (c.getCount() == 0){

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Application","importing cities ...");
                    long start= System.currentTimeMillis();
                    int count = 0;

                    try {
                        InputStream is = getAssets().open("cidades");
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        String line = null;

                        while((line = br.readLine())!=null){
                            ++count;
                            String[] values = line.split(",");

                            ContentValues cv = new ContentValues();
                            cv.put(CidadesProvider._ID,Integer.valueOf(values[0]));
                            cv.put(CidadesProvider.NOME,values[1]);
                            cv.put(CidadesProvider.UF,values[2]);
                            getContentResolver().insert(CidadesProvider.CONTENT_URI,cv);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    long end = System.currentTimeMillis();
                    Log.d("Application",count +" cities imported in " + (end-start) + "ms" );

                }
            }).start();


        }

        c.close();
    }

    private void criarImagesProdutosDir() {

        File f = new File(PRODUTOS.getPath(this));
        if (!f.exists()){
            f.mkdirs();
        }

        f = new File(ANEXOS.getPath(this));
        if (!f.exists()){
            f.mkdirs();
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public boolean isAdmin(){
        String currentRoles = getRoles();
        return  (!TextUtils.isEmpty(currentRoles) && currentRoles.contains("ROLE_ADMIN"));
    }

    public boolean isAuthenticated(){
        return !TextUtils.isEmpty(this.accessToken);
    }

    public String getRoles(){return this.roles; };

    private void loadAuthenticationInfo() {
        SharedPreferences sp = getSharedPreferences(ARRASAVENDAS_AUTH_PREFS_KEY, MODE_PRIVATE);
        this.currentUser = sp.getString("username",null);
        this.accessToken = sp.getString("access_token",null);
        this.roles = sp.getString("roles",null);

    }
    public static Context context()
    {
        return mApp.getApplicationContext();
    }

    public static Application getInstance(){ return mApp; }

    public static void salvarToken(String username, String roles, String access_token) {
        SharedPreferences sp = context().getSharedPreferences(ARRASAVENDAS_AUTH_PREFS_KEY, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sp.edit();

        editor.putString("username",username);
        editor.putString("roles",roles);
        editor.putString("access_token",access_token);

        editor.commit();

        Application.mApp.currentUser = username;
        Application.mApp.accessToken= access_token;
        Application.mApp.roles = roles;
    }
}
