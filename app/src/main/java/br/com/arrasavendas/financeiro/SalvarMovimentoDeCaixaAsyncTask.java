package br.com.arrasavendas.financeiro;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.MultipartUtility;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.Utilities;
import br.com.arrasavendas.model.MovimentoCaixa;

public class SalvarMovimentoDeCaixaAsyncTask extends AsyncTask<Void,Void,SalvarMovimentoDeCaixaAsyncTask.Response>{

    private final MovimentoCaixa movimentoCaixa;
    private final Context ctx;

    interface OnComplete{
		void run(Response response);
	}

	private OnComplete onComplete;

	public static class Response {
		private String message;
		private int status;

		public Response(String message, int status) {
			this.message = message;
			this.status = status;
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

	public SalvarMovimentoDeCaixaAsyncTask(MovimentoCaixa movimentoCaixa, Context ctx,OnComplete onComplete) {
		this.movimentoCaixa = movimentoCaixa;
        this.ctx =ctx;
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
            URL url = new  URL(path);
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

            Response response = new Response(httpConnection.getResponseMessage(),
                    httpConnection.getResponseCode());

            httpConnection.disconnect();

            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

	}
	
	@Override
	protected void onPostExecute(Response result) {
		if (this.onComplete!=null){
			onComplete.run(result);
		}
	}


	
}