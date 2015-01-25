package br.com.arrasavendas.entregas;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import br.com.arrasavendas.RemotePath;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

public class UpdateDataEntregaVendaAsyncTask extends AsyncTask<Void,Void,HttpResponse>{

	private final Context ctx;

	interface OnComplete{
		void run(HttpResponse response);
	}
	
	private OnComplete onComplete;
	private Long vendaId;
	private Date novaDataEntrega;

	public UpdateDataEntregaVendaAsyncTask(Long vendaId, Date novaDataEntrega,OnComplete onComplete,Context ctx) {
		this.novaDataEntrega = novaDataEntrega;
		this.vendaId = vendaId;
		this.onComplete = onComplete;
		this.ctx = ctx;
	}
	
	@Override
	protected HttpResponse doInBackground(Void... params) {
		SharedPreferences sp = ctx.getSharedPreferences("br.com.arrasaamiga.auth", Activity.MODE_PRIVATE);
		String accessToken = sp.getString("access_token","");

		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPut httPut = new HttpPut(RemotePath.getVendaEntityPath(this.vendaId));

		StringEntity se = null;

		try {
			JSONObject obj = new JSONObject();
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'", Locale.getDefault());
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
			obj.put("dataEntrega",simpleDateFormat.format(novaDataEntrega) );
			
			se = new StringEntity(obj.toString());
			httPut.setEntity(se);

			httPut.setHeader("Authorization","Bearer " + accessToken);
			httPut.setHeader("Accept", "application/json");
			httPut.setHeader("Content-Type", "application/json");			

			return httpclient.execute(httPut);
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
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