package br.com.arrasavendas;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import br.com.arrasavendas.util.Response;

/**
 * Created by lsimaocosta on 04/01/15.
 */
public class LoginAsyncTask extends AsyncTask<LoginAsyncTask.LoginSenha, Void, Response> {


    private final OnLogin listener;
    private final Context ctx;

    public LoginAsyncTask(OnLogin listener, Context ctx) {
        super();
        this.listener = listener;
        this.ctx = ctx;
    }

    @Override
    protected Response doInBackground(LoginSenha... loginSenha) {


        try {
            URL url = new URL(RemotePath.LoginPath.getUrl());
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

            httpConnection.setDoInput(true);
            httpConnection.setDoOutput(true);
            httpConnection.setConnectTimeout(5000);
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

            int responseCode = httpConnection.getResponseCode();
            Response response = null;
            BufferedReader reader = null;
            InputStreamReader in = null;
            StringBuilder stringBuilder = new StringBuilder();


            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                in = new InputStreamReader(httpConnection.getInputStream());
            } else {
                in = new InputStreamReader(httpConnection.getErrorStream());
            }

            reader = new BufferedReader(in);
            String line = null;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            String message = stringBuilder.toString();
            response = new Response(message, responseCode);
            httpConnection.disconnect();

            return response;

        } catch (ConnectException | SocketTimeoutException e) {
            e.printStackTrace();
            String message = ctx.getString(R.string.connection_error_msg);
            return new Response(message,-1);

        } catch (Exception e) {
            e.printStackTrace();
            return new Response(e.getMessage(),-1);
        }
    }

    @Override
    protected void onPostExecute(Response response) {
        if (listener != null)
            listener.run(response);

    }

    public interface OnLogin {
        void run(Response response);
    }

    static class LoginSenha {
        String login, senha;

        public LoginSenha(String login, String senha) {
            this.login = login;
            this.senha = senha;
        }
    }


}
