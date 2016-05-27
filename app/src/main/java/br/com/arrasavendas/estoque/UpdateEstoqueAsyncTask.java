package br.com.arrasavendas.estoque;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.util.Response;

public class UpdateEstoqueAsyncTask extends AsyncTask<Void, Void, Response> {

    private final long estoqueId;
    private final JSONObject jsonObj;
    private OnComplete onComplete;

    public UpdateEstoqueAsyncTask(long estoqueId, long quantidade, OnComplete onComplete) {
        this.jsonObj = new JSONObject();
        try {
            this.jsonObj.put("quantidade", quantidade);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.estoqueId = estoqueId;
        this.onComplete = onComplete;
    }

    @Override
    protected Response doInBackground(Void... params) {
        try {

            String accessToken = Application.getInstance().getAccessToken();
            String entityPath = RemotePath.getEntityPath(RemotePath.EstoquePath, this.estoqueId);


            URL url = new URL(entityPath);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setDoInput(true);
            httpConnection.setDoOutput(true);
            httpConnection.setRequestMethod("PUT");
            httpConnection.setUseCaches(false);
            httpConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
            httpConnection.setRequestProperty("Accept", "application/json");
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.connect();


            DataOutputStream dos = new DataOutputStream(httpConnection.getOutputStream());
            byte[] bytes = jsonObj.toString().getBytes("UTF-8");
            dos.write(bytes);
            dos.flush();
            dos.close();

            String line = null;
            BufferedReader reader = null;
            int responseCode = httpConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK)
                reader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
            else
                reader = new BufferedReader(new InputStreamReader(httpConnection.getErrorStream()));

            StringBuilder stringBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            String message = stringBuilder.toString();
            Response response = new Response(message, responseCode);
            httpConnection.disconnect();

            return response;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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