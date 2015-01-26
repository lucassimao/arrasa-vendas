package br.com.arrasavendas.entregas;

import android.content.Context;
import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.RemotePath;


public class ExcluirVendaAsyncTask extends AsyncTask<Void, Void, HttpResponse> {

    private final Context ctx;

    interface OnComplete {
        void run(HttpResponse response);
    }

    private OnComplete onComplete;
    private Long vendaId;

    public ExcluirVendaAsyncTask(Long vendaId, OnComplete onComplete, Context ctx) {
        this.vendaId = vendaId;
        this.onComplete = onComplete;
        this.ctx = ctx;
    }

    @Override
    protected HttpResponse doInBackground(Void... params) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpDelete httpdelete = new HttpDelete(RemotePath.getVendaEntityPath(this.vendaId));

        Application app = (Application) ctx.getApplicationContext();
        String accessToken = app.getAccessToken();

        try {
            httpdelete.setHeader("Authorization", "Bearer " + accessToken);
            httpdelete.setHeader("Accept", "application/json");
            return httpclient.execute(httpdelete);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }


    @Override
    protected void onPostExecute(HttpResponse result) {
        if (this.onComplete != null) {
            onComplete.run(result);
        }
    }


}