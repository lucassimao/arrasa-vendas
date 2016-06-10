package br.com.arrasavendas.entregas.asyncTask;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.util.Response;

public class UpdateVendaAsyncTask extends AsyncTask<Void,Void,Response>{

    private final long vendaId;

    public interface OnComplete{
		void run(Response response);
	}

	private OnComplete onComplete;
	private JSONObject venda;

	public UpdateVendaAsyncTask(long vendaId, JSONObject venda, OnComplete onComplete) {
		this.venda = venda;
		this.onComplete = onComplete;
		this.vendaId = vendaId;
	}
	
	@Override
	protected Response doInBackground(Void... params) {
		try {
			
			return makeRequest(venda);
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	
	private Response makeRequest(JSONObject obj) throws Exception {

        Application app = Application.getInstance();
        String accessToken = app.getAccessToken();
		String entityPath = RemotePath.VendaPath.getEntityPath(RemotePath.VendaPath, this.vendaId);

		URL url = new URL(entityPath);
		HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
		httpConnection.setDoInput(true);
		httpConnection.setDoOutput(true);
		httpConnection.setRequestMethod("PUT");
		httpConnection.setUseCaches(false);
		httpConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
		httpConnection.setRequestProperty("clienteId", app.getId());
		httpConnection.setRequestProperty("Accept", "application/json");
		httpConnection.setRequestProperty("Content-Type", "application/json");
		httpConnection.connect();


		DataOutputStream dos = new DataOutputStream(httpConnection.getOutputStream());
		byte[] bytes = obj.toString().getBytes("UTF-8");
		dos.write(bytes);
		dos.flush();
		dos.close();

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

	}
	
	@Override
	protected void onPostExecute(Response result) {

        if (this.onComplete!=null){
			onComplete.run(result);
		}
	}


	
}