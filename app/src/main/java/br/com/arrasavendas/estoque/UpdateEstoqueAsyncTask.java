package br.com.arrasavendas.estoque;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import br.com.arrasavendas.Application;
import br.com.arrasavendas.RemotePath;
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class UpdateEstoqueAsyncTask extends AsyncTask<Void,Void,HttpResponse>{

	private final long estoqueId;
	private final JSONObject jsonObj;

    interface OnComplete{
		void run(HttpResponse response);
	}

	private OnComplete onComplete;

	public UpdateEstoqueAsyncTask(long estoqueId,long quantidade,OnComplete onComplete) {
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
	protected HttpResponse doInBackground(Void... params) {
		try {

			String accessToken = Application.getInstance().getAccessToken();

			DefaultHttpClient httpclient = new DefaultHttpClient();

            String entityPath = RemotePath.getEntityPath(RemotePath.EstoquePath, this.estoqueId);
            HttpPut httput = new HttpPut(entityPath);

			StringEntity se = new StringEntity(jsonObj.toString(),"UTF-8");
            httput.setEntity(se);

            httput.setHeader("Authorization","Bearer " + accessToken);
            httput.setHeader("Accept", "application/json");
            httput.setHeader("Content-type", "application/json");

			return httpclient.execute(httput);
			
		} catch (HttpHostConnectException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onPostExecute(HttpResponse result) {

		if (this.onComplete!=null){
			onComplete.run(result);
		}
	}


	
}