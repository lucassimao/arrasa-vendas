package br.com.arrasavendas.venda;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.util.Response;

public class SalvarVendaAsyncTask extends AsyncTask<JSONObject, Void, Response> {

    private OnComplete onComplete;

    public SalvarVendaAsyncTask(OnComplete onComplete) {
        this.onComplete = onComplete;
    }

    @Override
    protected Response doInBackground(JSONObject... params) {
        try {

            return makeRequest(params[0]);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Response makeRequest(JSONObject obj) throws Exception {

        Application app = Application.getInstance();
        String accessToken = app.getAccessToken();


        URL url = new URL(RemotePath.VendaPath.getUrl());
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

        httpConnection.setDoInput(true);
        httpConnection.setDoOutput(true);
        httpConnection.setRequestMethod("POST");
        httpConnection.setUseCaches(false);
        httpConnection.setRequestProperty("Content-Type", "application/json");
        httpConnection.setRequestProperty("Accept", "application/json");
        httpConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

        httpConnection.connect();

        DataOutputStream dos = new DataOutputStream(httpConnection.getOutputStream());
        byte[] bytes = obj.toString().getBytes("UTF-8");
        dos.write(bytes);
        dos.flush();
        dos.close();


        String line = null;
        BufferedReader reader = null;

        if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_CREATED)
            reader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
        else
            reader = new BufferedReader(new InputStreamReader(httpConnection.getErrorStream()));

        StringBuilder stringBuilder = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }

        String message = stringBuilder.toString();
        int responseCode = httpConnection.getResponseCode();
        Response response = new Response(message, responseCode);

        httpConnection.disconnect();
        return response;
    }

    @Override
    protected void onPostExecute(Response result) {
        if (this.onComplete != null) {
            onComplete.run(result);
        }
    }

    interface OnComplete {
        void run(Response response);
    }


}