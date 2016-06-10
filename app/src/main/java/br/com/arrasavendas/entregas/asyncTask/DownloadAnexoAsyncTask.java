package br.com.arrasavendas.entregas.asyncTask;

import android.os.AsyncTask;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.util.Response;

/**
 * Created by lsimaocosta on 22/02/16.
 */
public class DownloadAnexoAsyncTask extends AsyncTask<String, Void, DownloadAnexoAsyncTask.HttpResponse> {

    private OnComplete onComplete;

    public DownloadAnexoAsyncTask(OnComplete onComplete) {
        super();
        this.onComplete = onComplete;
    }

    @Override
    protected HttpResponse doInBackground(String... params) {

        Application app = (Application) Application.context();
        String accessToken = app.getAccessToken();
        String imageURL = RemotePath.getAnexoImageURL(params[0]);

        try {
            URL url = new URL(imageURL);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setDoInput(true);
            httpConnection.setDoOutput(true);
            httpConnection.setRequestMethod("GET");
            httpConnection.setUseCaches(false);
            httpConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.connect();

            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataInputStream dis = new DataInputStream(httpConnection.getInputStream());
                byte[] bytes = new byte[1024];
                int count = 0;

                while ((count = dis.read(bytes)) != -1) {
                    baos.write(bytes, 0, count);
                }

                return new HttpResponse(httpConnection.getResponseMessage(),
                        responseCode, baos);
            } else
                return new HttpResponse(httpConnection.getResponseMessage(), responseCode, null);


        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(HttpResponse httpResponse) {
        if (this.onComplete != null) {
            onComplete.run(httpResponse);
        }
    }

    public interface OnComplete {
        void run(HttpResponse response);
    }

    public static class HttpResponse extends Response {

        private final byte[] bytes;

        public HttpResponse(String message, int status, ByteArrayOutputStream baos) {
            super(message, status);
            if (baos != null)
                this.bytes = baos.toByteArray();
            else
                this.bytes = null;
        }

        public byte[] getBytes() {
            return bytes;
        }
    }
}
