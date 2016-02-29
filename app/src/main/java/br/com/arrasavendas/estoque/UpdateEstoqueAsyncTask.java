package br.com.arrasavendas.estoque;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import br.com.arrasavendas.Application;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.util.Response;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateEstoqueAsyncTask extends AsyncTask<Void,Void,Response>{

	private final long estoqueId;
	private final JSONObject jsonObj;

    interface OnComplete{
		void run(Response response);
	}

	private OnComplete onComplete;

	public UpdateEstoqueAsyncTask(long estoqueId,long quantidade,OnComplete onComplete) {
        this.jsonObj = new JSONObject();
        try {
            this.jsonObj.put("quantidade", quantidade*-1);
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

			for(String s :httpConnection.getHeaderFields().keySet())
				Log.d("HEADERS",s + ": " + httpConnection.getHeaderField(s));

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

		if (this.onComplete!=null){
			onComplete.run(result);
		}
	}


	
}