package br.com.arrasavendas.entregas.asyncTask;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.MultipartUtility;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.Utilities;
import br.com.arrasavendas.util.Response;


public class UploadAnexoAsyncTask extends AsyncTask<Uri, Void, UploadAnexoAsyncTask.ResponseUpload> {

    private final Context ctx;
    private OnComplete onComplete;
    private Long vendaId;

    public UploadAnexoAsyncTask(Long vendaId, OnComplete onComplete, Context ctx) {
        this.vendaId = vendaId;
        this.onComplete = onComplete;
        this.ctx = ctx;
    }

    @Override
    protected ResponseUpload doInBackground(Uri... anexos) {

        ContentResolver contentResolver = ctx.getContentResolver();
        Application app = (Application) ctx.getApplicationContext();
        String accessToken = app.getAccessToken();
        String path = RemotePath.getAnexosPath(this.vendaId);

        try {

            Uri uri = anexos[0];
            InputStream inputStream = (InputStream) contentResolver.openInputStream(uri);
            String type = contentResolver.getType(uri);
            File file = new File(Utilities.getPath(ctx, uri));
            int indexExtension = file.getName().lastIndexOf(".");
            String fileNameWithoutExtension = file.getName().substring(0, indexExtension);
            String newfileName = fileNameWithoutExtension + System.currentTimeMillis() + Utilities.getExtension(file.getName());

            MultipartUtility mu = new MultipartUtility(path, "UTF-8", accessToken);
            mu.addHeaderField("clienteId", app.getId());
            mu.addFilePart("anexo", newfileName, inputStream, type);

            Response httpResponse = mu.finish();
            return new ResponseUpload(httpResponse.getMessage(), httpResponse.getStatus(), newfileName);

        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseUpload(e.getMessage(),-1,null);
        }

    }

    @Override
    protected void onPostExecute(ResponseUpload httpResponse) {
        if (this.onComplete != null) {
            onComplete.run(httpResponse);
        }
    }

    public interface OnComplete {
        void run(ResponseUpload response);
    }

    public static class ResponseUpload extends br.com.arrasavendas.util.Response {
        private String fileName;

        public ResponseUpload(String message, int status, String fileName) {
            super(message, status);
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }


}