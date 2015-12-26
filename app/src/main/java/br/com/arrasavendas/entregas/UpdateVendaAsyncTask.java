package br.com.arrasavendas.entregas;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.RemotePath;

public class UpdateVendaAsyncTask extends AsyncTask<Void,Void,HttpResponse>{

    private final long vendaId;

    interface OnComplete{
		void run(HttpResponse response);
	}

	private OnComplete onComplete;
	private JSONObject venda;

	public UpdateVendaAsyncTask(long vendaId, JSONObject venda, OnComplete onComplete) {
		this.venda = venda;
		this.onComplete = onComplete;
		this.vendaId = vendaId;
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

        Application app = Application.getInstance();
        String accessToken = app.getAccessToken();

		// instantiates httpclient to make request
		DefaultHttpClient httpclient = new DefaultHttpClient();

		// url with the post data
		HttpPut httpust = new HttpPut(RemotePath.VendaPath.getEntityPath(RemotePath.VendaPath, this.vendaId));

		StringEntity se = new StringEntity(obj.toString(),"UTF-8");
		httpust.setEntity(se);

		httpust.setHeader("Authorization", "Bearer " + accessToken);
		httpust.setHeader("Accept", "application/json");
		httpust.setHeader("Content-type", "application/json");

		// Handles what is returned from the page
		return httpclient.execute(httpust);
	}
	
	@Override
	protected void onPostExecute(HttpResponse result) {

        if (this.onComplete!=null){
			onComplete.run(result);
		}
	}


	
}