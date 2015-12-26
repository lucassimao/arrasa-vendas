package br.com.arrasavendas;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

import static br.com.arrasavendas.Utilities.ImageFolder.ANEXOS;
import static br.com.arrasavendas.Utilities.ImageFolder.PRODUTOS;

/**
 * Created by lsimaocosta on 13/01/15.
 */
public class Application extends android.app.Application {

    public static final String ARRASAVENDAS_AUTH_PREFS_KEY = "br.com.arrasavendas.auth";
    private String currentUser = null;
    private String accessToken;
    private static Application mApp = null;


    @Override
    public void onCreate() {
        super.onCreate();
        loadAuthenticationInfo();
        criarImagesProdutosDir();
        mApp = this;
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

    private void loadAuthenticationInfo() {
        SharedPreferences sp = getSharedPreferences(ARRASAVENDAS_AUTH_PREFS_KEY, MODE_PRIVATE);
        this.currentUser = sp.getString("username",null);
        this.accessToken = sp.getString("access_token",null);
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
    }
}
