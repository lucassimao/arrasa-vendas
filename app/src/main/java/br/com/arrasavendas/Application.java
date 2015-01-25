package br.com.arrasavendas;

import android.content.SharedPreferences;

/**
 * Created by lsimaocosta on 13/01/15.
 */
public class Application extends android.app.Application {

    private String currentUser = null;
    private String accessToken;


    @Override
    public void onCreate() {
        super.onCreate();
        loadAuthenticationInfo();
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
}
