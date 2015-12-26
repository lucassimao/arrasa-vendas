package br.com.arrasavendas.entregas;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.HttpResponse;
import br.com.arrasavendas.MultipartUtility;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.Utilities;

import static br.com.arrasavendas.entregas.UploadAnexoAsyncTask.Response;


public class UploadAnexoAsyncTask extends AsyncTask<Uri, Void, Response> {

    private final Context ctx;
    private OnComplete onComplete;
    private Long vendaId;

    interface OnComplete {
        void run(Response response);
    }

    public static class Response {
        private String message;
        private int status;
        private String fileName;

        public Response(String message, int status, String fileName) {
            this.message = message;
            this.status = status;
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getMessage() {
            return message;
        }
        public void setMessage(String message) {
            this.message = message;
        }
        public int getStatus() {
            return status;
        }
        public void setStatus(int status) {
            this.status = status;
        }
    }


    public UploadAnexoAsyncTask(Long vendaId, OnComplete onComplete, Context ctx) {
        this.vendaId = vendaId;
        this.onComplete = onComplete;
        this.ctx = ctx;
    }

    @Override
    protected Response doInBackground(Uri... anexos) {

        ContentResolver contentResolver = ctx.getContentResolver();
        Application app = (Application) ctx.getApplicationContext();
        String accessToken = app.getAccessToken();
        String path = RemotePath.getAnexosPath(this.vendaId);

        try {

            Uri uri = anexos[0];
            InputStream inputStream = (InputStream) contentResolver.openInputStream(uri);
            String type = contentResolver.getType(uri);
            File file = new File(Utilities.getPath(ctx,uri));
            int indexExtension = file.getName().lastIndexOf(".");
            String fileNameWithoutExtension = file.getName().substring(0, indexExtension);
            String newfileName = fileNameWithoutExtension + System.currentTimeMillis() + "." + Utilities.getExtension(file.getName());

            MultipartUtility mu = new MultipartUtility(path, "UTF-8", accessToken);
            mu.addFilePart("anexo", newfileName, inputStream, type);

            HttpResponse httpResponse =  mu.finish();
            return new Response(httpResponse.getMessage(),httpResponse.getStatus(),newfileName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    @Override
    protected void onPostExecute(Response httpResponse) {
        if (this.onComplete != null) {
            onComplete.run(httpResponse);
        }
    }


}