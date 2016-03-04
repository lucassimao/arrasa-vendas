package br.com.arrasavendas.financeiro;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.entregas.UploadAnexoAsyncTask;
import br.com.arrasavendas.model.MovimentoCaixa;
import br.com.arrasavendas.util.Response;

public class SalvarMovimentoDeCaixaAsyncTask extends AsyncTask<Void, Void, Response> {

    private final MovimentoCaixa movimentoCaixa;
    private final Context ctx;
    private OnComplete onComplete;

    public SalvarMovimentoDeCaixaAsyncTask(MovimentoCaixa movimentoCaixa, Context ctx, OnComplete onComplete) {
        this.movimentoCaixa = movimentoCaixa;
        this.ctx = ctx;
        this.onComplete = onComplete;
    }

    @Override
    protected Response doInBackground(Void... params) {
        try {

            return makeRequest(movimentoCaixa);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Response makeRequest(MovimentoCaixa movimentoCaixa) throws Exception {

        Application app = (Application) ctx.getApplicationContext();
        String accessToken = app.getAccessToken();
        String path = RemotePath.MovimentoCaixaPath.getUrl();

        try {
            URL url = new URL(path);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setDoInput(true);
            httpConnection.setDoOutput(true);
            httpConnection.setRequestMethod("POST");
            httpConnection.setUseCaches(false);
            httpConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.connect();

            JSONObject obj = movimentoCaixa.toJSONObject();
            DataOutputStream dos = new DataOutputStream(httpConnection.getOutputStream());
            byte[] bytes = obj.toString().getBytes("UTF-8");
            dos.write(bytes);
            dos.flush();
            dos.close();

            InputStreamReader isr = null;
            BufferedReader br = null;
            String line = null;
            StringBuilder sb = new StringBuilder();
            int responseCode = httpConnection.getResponseCode();


            if (responseCode == HttpURLConnection.HTTP_CREATED)
                isr = new InputStreamReader(httpConnection.getInputStream());
            else
                isr = new InputStreamReader(httpConnection.getErrorStream());

            br = new BufferedReader(isr);
            while( (line=br.readLine())!=null)
                sb.append(line);


            Response response = new Response(sb.toString(),responseCode);
            httpConnection.disconnect();

            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

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