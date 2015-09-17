package br.com.arrasavendas.entregas;

import android.content.Context;
import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.model.Cliente;
import br.com.arrasavendas.model.StatusVenda;
import br.com.arrasavendas.model.TurnoEntrega;

public class UpdateClienteVendaAsyncTask extends AsyncTask<Void,Void,HttpResponse>{

	private final Context ctx;

	interface OnComplete{

        void run(HttpResponse response);

    }

    private OnComplete onComplete;
    private final TurnoEntrega turnoEntrega;
    private final StatusVenda statusVenda;
    private Cliente cliente;
	private Long vendaId;

	public UpdateClienteVendaAsyncTask(Long vendaId, Cliente cliente, TurnoEntrega turno, StatusVenda statusVenda, OnComplete onComplete,Context ctx) {
		this.cliente = cliente;
		this.vendaId = vendaId;
        this.turnoEntrega = turno;
		this.onComplete = onComplete;
        this.statusVenda = statusVenda;
		this.ctx = ctx;
	}
	
	@Override
	protected HttpResponse doInBackground(Void... params) {
        Application app = (Application) ctx.getApplicationContext();
        String accessToken = app.getAccessToken();

		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPut httPut = new HttpPut(RemotePath.getEntityPath(RemotePath.VendaPath, this.vendaId));

		StringEntity se = null;

		try {
			JSONObject obj = new JSONObject();
			obj.put("cliente",cliente.toJson() );
            obj.put("turnoEntrega",turnoEntrega.name());
            obj.put("status",statusVenda.name());

			se = new StringEntity(obj.toString(),"UTF-8");
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
			// TODO Auto-generated catch block
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