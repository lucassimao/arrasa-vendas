package br.com.arrasavendas;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import br.com.arrasavendas.util.Response;

/**
 * Created by lsimaocosta on 04/01/15.
 */
public class LoginAsyncTask extends AsyncTask<LoginAsyncTask.LoginSenha, Void, Response> {


    private final Runnable onSuccessfullLoginListener, onUnSuccessfullLoginListener;
    private final Context context;

    public LoginAsyncTask(Runnable onSuccessfullLoginListener, Runnable onUnSuccessfullLoginListener, Context ctx) {
        super();
        this.onSuccessfullLoginListener = onSuccessfullLoginListener;
        this.onUnSuccessfullLoginListener = onUnSuccessfullLoginListener;
        this.context = ctx;
    }

    @Override
    protected Response doInBackground(LoginSenha... loginSenha) {


        try {
            URL url = new URL(RemotePath.LoginPath.getUrl());
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

            httpConnection.setDoInput(true);
            httpConnection.setDoOutput(true);
            httpConnection.setRequestMethod("POST");
            httpConnection.setUseCaches(false);
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.setRequestProperty("Accept", "application/json");
            httpConnection.connect();

            JSONObject obj = new JSONObject();
            obj.put("username", loginSenha[0].login);
            obj.put("password", loginSenha[0].senha);

            DataOutputStream dos = new DataOutputStream(httpConnection.getOutputStream());
            byte[] bytes = obj.toString().getBytes("UTF-8");
            dos.write(bytes);
            dos.flush();
            dos.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            String message = stringBuilder.toString();
            int responseCode = httpConnection.getResponseCode();
            Response response = new Response(message, responseCode);

            httpConnection.disconnect();
            return response;

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Response response) {
        if (response.getStatus() == HttpURLConnection.HTTP_OK) {
            this.onSuccessfullLoginListener.run();
            salvarAutenticacao(response.getMessage());
        } else {
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

    static class LoginSenha {
        String login, senha;

        public LoginSenha(String login, String senha) {
            this.login = login;
            this.senha = senha;
        }
    }


}
