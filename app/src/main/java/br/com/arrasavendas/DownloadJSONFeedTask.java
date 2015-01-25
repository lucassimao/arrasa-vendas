package br.com.arrasavendas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.SharedPreferences;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;

import br.com.arrasavendas.providers.EstoqueProvider;
import br.com.arrasavendas.providers.VendasProvider;

public class DownloadJSONFeedTask extends AsyncTask<Void, Void, Void> {

	private Context ctx;
	private Runnable onCompleteListener;
	private RemotePath feed;
	
	
	public DownloadJSONFeedTask(RemotePath feed, Context ctx, Runnable onCompleteListener) {
		super();
		this.ctx = ctx;
		this.feed = feed;
		this.onCompleteListener = onCompleteListener;
	}

	@Override
	protected Void doInBackground(Void... params) {

		try {

			String jsonString = downloadJSON();
			salvarJSON(jsonString);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;

	}

	private void salvarJSON(String jsonString) throws IOException, JSONException {
		
		if (this.feed == RemotePath.EstoqueList){
			// apaga primeiro a tabela ...
			this.ctx.getContentResolver().delete(EstoqueProvider.CONTENT_URI, null,null);			
		}
		
		if (this.feed == RemotePath.VendaPath){
			// apaga tabela de vendas ...
			this.ctx.getContentResolver().delete(VendasProvider.CONTENT_URI, null,null);		
		}
		


		JSONArray itens = new JSONArray(jsonString);

		for (int i = 0; i < itens.length(); ++i) {
			ContentValues values;
			
			switch (this.feed) {
				case EstoqueList:
					
					JSONArray estoque = itens.getJSONArray(i);
					
					int id = estoque.getInt(0);
					String produto = estoque.getString(1);
					String precoAVistaEmCentavos = estoque.getString(2);
					String precoAPrazoEmCentavos = estoque.getString(3);
					String unidade = estoque.getString(4);
					Integer qtde = estoque.getInt(5);
					
					values = new ContentValues();
					values.put(EstoqueProvider.PRODUTO, produto);
                    values.put(EstoqueProvider.PRODUTO_ID, id);
                    values.put(EstoqueProvider.PRECO_A_VISTA, precoAVistaEmCentavos);
                    values.put(EstoqueProvider.PRECO_A_PRAZO, precoAPrazoEmCentavos);
                    values.put(EstoqueProvider.UNIDADE, unidade);
                    values.put(EstoqueProvider.QUANTIDADE, qtde);

					this.ctx.getContentResolver().insert(EstoqueProvider.CONTENT_URI, values);
					
					break;
				case VendaPath:
					
					JSONObject venda = itens.getJSONObject(i);

                    if (!venda.isNull("dataEntrega")) {

                        values = new ContentValues();
                        values.put(VendasProvider._ID, venda.getInt("id"));
                        values.put(VendasProvider.VENDEDOR, venda.getString("vendedor"));
                        values.put(VendasProvider.CLIENTE, venda.getString("cliente"));
                        values.put(VendasProvider.DATA_ENTREGA, venda.getLong("dataEntrega"));
                        values.put(VendasProvider.FORMA_PAGAMENTO, venda.getString("formaPagamento"));
                        //Log.d("<< DOWNLOAD >>", venda.getString("cliente") + " : " + venda.getLong("dataEntrega"));
                        values.put(VendasProvider.TURNO_ENTREGA, venda.getString("turnoEntrega"));
                        values.put(VendasProvider.STATUS, venda.getString("status"));
                        values.put(VendasProvider.CARRINHO, venda.getString("itens"));

                        this.ctx.getContentResolver().insert(VendasProvider.CONTENT_URI, values);
                    }
					
					break;
			}
			
		}

	}

	private String downloadJSON() throws IOException {

		SharedPreferences sp = ctx.getSharedPreferences("br.com.arrasaamiga.auth", Activity.MODE_PRIVATE);
		//TODO tratar a inexistencia do TOKEN
		String accessToken = sp.getString("access_token","");


		HttpClient client = new DefaultHttpClient();
		
		HttpGet httpGet = new HttpGet(this.feed.getUrl());
		httpGet.setHeader("Accept", "application/json");
        httpGet.setHeader("Authorization","Bearer " + accessToken);

		HttpResponse response = client.execute(httpGet);
		StatusLine statusLine = response.getStatusLine();
		int statusCode = statusLine.getStatusCode();

		if (statusCode == HttpStatus.SC_OK) {
			StringBuilder stringBuilder = new StringBuilder();
			HttpEntity entity = response.getEntity();
			InputStream content = entity.getContent();
			BufferedReader reader;
			reader = new BufferedReader(new InputStreamReader(content));
			String line;

			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
			}
			return stringBuilder.toString();
		}
		return null;

	}

	protected void onPostExecute(Void result) {
		
		if (onCompleteListener != null)
			onCompleteListener.run();
	}

}
