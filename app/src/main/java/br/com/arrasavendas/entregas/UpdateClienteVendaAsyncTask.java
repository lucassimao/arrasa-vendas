package br.com.arrasavendas.entregas;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.venda.StatusVenda;
import br.com.arrasavendas.venda.TurnoEntrega;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import br.com.arrasavendas.venda.Cliente;

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
		SharedPreferences sp = ctx.getSharedPreferences("br.com.arrasaamiga.auth", Activity.MODE_PRIVATE);
		String accessToken = sp.getString("access_token","");

		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPut httPut = new HttpPut(RemotePath.getVendaEntityPath(this.vendaId));

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