package br.com.arrasavendas;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

/**
 * Created by lsimaocosta on 04/01/15.
 */
public class LoginAsyncTask extends AsyncTask<LoginAsyncTask.LoginSenha,Void, HttpResponse> {


    private final Runnable onSuccessfullLoginListener,onUnSuccessfullLoginListener;
    private final Context context;

    public LoginAsyncTask(Runnable onSuccessfullLoginListener,Runnable onUnSuccessfullLoginListener,Context ctx) {
        super();
        this.onSuccessfullLoginListener = onSuccessfullLoginListener;
        this.onUnSuccessfullLoginListener=onUnSuccessfullLoginListener;
        this.context = ctx;
    }

    static class LoginSenha{
        String login, senha;

        public LoginSenha(String login, String senha) {
            this.login = login;
            this.senha = senha;
        }
    }

    @Override
    protected HttpResponse doInBackground(LoginSenha... loginSenha) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(RemotePath.LoginPath.getUrl());

        StringEntity se = null;

        try {
            JSONObject obj = new JSONObject();
            obj.put("username",loginSenha[0].login );
            obj.put("password",loginSenha[0].senha );

            se = new StringEntity(obj.toString(),"UTF-8");
            httpPost.setEntity(se);

            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-Type", "application/json");

            return  httpclient.execute(httpPost);

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    protected void onPostExecute(HttpResponse response) {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

            this.onSuccessfullLoginListener.run();

            StringBuilder stringBuilder = new StringBuilder();
            HttpEntity entity = response.getEntity();
            InputStream content = null;
            try {
                content = entity.getContent();

                BufferedReader reader;
                reader = new BufferedReader(new InputStreamReader(content));
                String line;

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                salvarAutenticacao(stringBuilder.toString());

            } catch (IOException e) {
                e.printStackTrace();
            }


        }else {
            this.onUnSuccessfullLoginListener.run();
        }

    }

    private void salvarAutenticacao(String json) {

        Application app = (Application) context.getApplicationContext();

        try {
            JSONObject obj = new JSONObject(json);
            app.salvarToken(obj.getString("username"), obj.getString("roles"), obj.getString("access_token"));

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


}
