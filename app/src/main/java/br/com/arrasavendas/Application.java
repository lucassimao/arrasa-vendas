package br.com.arrasavendas;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by lsimaocosta on 13/01/15.
 */
public class Application extends android.app.Application {

    private String currentUser = null;
    private String accessToken;
    private static Application mApp = null;


    @Override
    public void onCreate() {
        super.onCreate();
        loadAuthenticationInfo();
        mApp = this;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    private void loadAuthenticationInfo() {
        SharedPreferences sp = getSharedPreferences("br.com.arrasavendas.auth", MODE_PRIVATE);
        this.currentUser = sp.getString("username",null);
        this.accessToken = sp.getString("access_token",null);
    }
    public static Context context()
    {
        return mApp.getApplicationContext();
    }

    public void salvarToken(String username, String roles, String access_token) {
        SharedPreferences sp = context().getSharedPreferences("br.com.arrasavendas.auth", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sp.edit();

        editor.putString("username",username);
        editor.putString("roles",roles);
        editor.putString("access_token",access_token);

        editor.commit();
    }
}
