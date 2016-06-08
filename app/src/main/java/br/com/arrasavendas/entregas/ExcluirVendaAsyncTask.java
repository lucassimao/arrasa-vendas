package br.com.arrasavendas.entregas;

import android.content.Context;
import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.util.Response;


public class ExcluirVendaAsyncTask extends AsyncTask<Void, Void, Response> {

    private final Context ctx;

    interface OnComplete {
        void run(Response response);
    }

    private OnComplete onComplete;
    private Long vendaId;

    public ExcluirVendaAsyncTask(Long vendaId, OnComplete onComplete, Context ctx) {
        this.vendaId = vendaId;
        this.onComplete = onComplete;
        this.ctx = ctx;
    }

    @Override
    protected Response doInBackground(Void... params) {

        Application app = Application.getInstance();
        String accessToken = app.getAccessToken();
        String entityPath = RemotePath.VendaPath.getEntityPath(RemotePath.VendaPath, vendaId);

        try {
            URL url = new URL(entityPath);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setDoInput(true);
            httpConnection.setDoOutput(false);
            httpConnection.setUseCaches(false);
            httpConnection.setRequestProperty("Content-Type", "none");
            httpConnection.setRequestProperty("clienteId", app.getId());
            httpConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
            httpConnection.setRequestMethod("DELETE");
            httpConnection.connect();


            int responseCode = httpConnection.getResponseCode();
            String message = null;

            if (responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
                BufferedReader reader = null;

                reader = new BufferedReader(new InputStreamReader(httpConnection.getErrorStream()));

                StringBuilder stringBuilder = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                message = stringBuilder.toString();
            }
            Response response = new Response(message, responseCode);
            httpConnection.disconnect();

            return response;

        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
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


}