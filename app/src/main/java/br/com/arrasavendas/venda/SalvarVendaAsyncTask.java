package br.com.arrasavendas.venda;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import br.com.arrasavendas.RemotePath;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.os.AsyncTask;

public class SalvarVendaAsyncTask extends AsyncTask<Void,Void,HttpResponse>{

	private final Context ctx;

	interface OnComplete{
		void run(HttpResponse response);
	}
	
	private OnComplete onComplete;
	private JSONObject venda;

	public SalvarVendaAsyncTask(JSONObject venda,OnComplete onComplete,Context ctx) {
		this.venda = venda;
		this.onComplete = onComplete;
		this.ctx = ctx;
	}
	
	@Override
	protected HttpResponse doInBackground(Void... params) {
		try {
			
			return makeRequest(venda);
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	
	private HttpResponse makeRequest(JSONObject obj) throws Exception {

		SharedPreferences sp = ctx.getSharedPreferences("br.com.arrasaamiga.auth", Activity.MODE_PRIVATE);
		String accessToken = sp.getString("access_token", "");

		// instantiates httpclient to make request
		DefaultHttpClient httpclient = new DefaultHttpClient();

		// url with the post data
		HttpPost httpost = new HttpPost(RemotePath.VendaPath.getUrl());

		StringEntity se = new StringEntity(obj.toString(),"UTF-8");
		httpost.setEntity(se);

		httpost.setHeader("Authorization","Bearer " + accessToken);
		httpost.setHeader("Accept", "application/json");
		httpost.setHeader("Content-type", "application/json");

		// Handles what is returned from the page
		return httpclient.execute(httpost);
	}
	
	@Override
	protected void onPostExecute(HttpResponse result) {
		if (this.onComplete!=null){
			onComplete.run(result);
		}
	}


	
}