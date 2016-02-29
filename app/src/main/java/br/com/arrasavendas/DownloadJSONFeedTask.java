package br.com.arrasavendas;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;

import br.com.arrasavendas.financeiro.FinanceiroDAO;
import br.com.arrasavendas.providers.DownloadedImagesProvider;
import br.com.arrasavendas.providers.EstoqueProvider;
import br.com.arrasavendas.providers.VendasProvider;
import br.com.arrasavendas.service.EstoqueService;

public class DownloadJSONFeedTask extends AsyncTask<RemotePath, Void, Void> {

    private Context ctx;
    private Runnable onCompleteListener;


    public DownloadJSONFeedTask(Context ctx, Runnable onCompleteListener) {
        super();
        this.ctx = ctx;
        this.onCompleteListener = onCompleteListener;
    }

    @Override
    protected Void doInBackground(RemotePath... params) {

        for(RemotePath remotePath : params) {

            try {

                String jsonString = downloadJSON(remotePath);

                if (!TextUtils.isEmpty(jsonString)) {

                    switch (remotePath) {
                        case EstoquePath:
                            salvarJSONEstoque(jsonString);
                            break;
                        case VendaPath:
                            salvarJSONVenda(jsonString);
                            break;
                        case CaixaPath:
                            salvarJSONCaixa(jsonString);
                            break;
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private void salvarJSONCaixa(String jsonString) {
        FinanceiroDAO dao = new FinanceiroDAO(this.ctx);
        dao.deleteAll();
        dao.save(jsonString);
        dao.close();
    }

    private void salvarJSONEstoque(String jsonString) throws IOException, JSONException {

        this.ctx.getContentResolver().delete(EstoqueProvider.CONTENT_URI, null, null);
        this.ctx.getContentResolver().delete(DownloadedImagesProvider.CONTENT_URI, null, null);

        JSONArray itens = new JSONArray(jsonString);
        EstoqueService service = new EstoqueService(ctx);

        for (int i = 0; i < itens.length(); ++i) {

            JSONObject estoque = itens.getJSONObject(i);
            service.save(estoque);
        }

    }

    private void salvarJSONVenda(String jsonString) throws IOException, JSONException {

        this.ctx.getContentResolver().delete(VendasProvider.CONTENT_URI, null, null);

        JSONArray itens = new JSONArray(jsonString);

        for (int i = 0; i < itens.length(); ++i) {
            JSONObject venda = itens.getJSONObject(i);

            if (!venda.isNull("dataEntrega")) {

                ContentValues values = new ContentValues();
                values.put(VendasProvider._ID, venda.getInt("id"));
                values.put(VendasProvider.VENDEDOR, venda.getString("vendedor"));
                values.put(VendasProvider.CLIENTE, venda.getString("cliente"));
                values.put(VendasProvider.LAST_UPDATED_TIMESTAMP, venda.getLong("last_updated"));
                values.put(VendasProvider.DATA_ENTREGA, venda.getLong("dataEntrega"));
                values.put(VendasProvider.FORMA_PAGAMENTO, venda.getString("formaPagamento"));
                values.put(VendasProvider.TURNO_ENTREGA, venda.getString("turnoEntrega"));
                values.put(VendasProvider.STATUS, venda.getString("status"));
                values.put(VendasProvider.CARRINHO, venda.getString("itens"));
                values.put(VendasProvider.ANEXOS_JSON_ARRAY, venda.getString("anexos_json_array"));

                this.ctx.getContentResolver().insert(VendasProvider.CONTENT_URI, values);
            }

        }

    }

    private String downloadJSON(RemotePath feed) throws IOException {

        Application app = (Application) ctx.getApplicationContext();

        if (app.isAuthenticated()) {
            String accessToken = app.getAccessToken();
            Long lastUpdated = getLastUpdated(feed);

            Log.d("DownloadJSONFeedTask",feed + " lastUpdated for " + feed + ": " +lastUpdated);

            Uri uri = Uri.parse(feed.getUrl()).
                                buildUpon().
                                appendQueryParameter("lastUpdated",lastUpdated.toString()).
                                build();

            HttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(uri.toString());
            httpGet.setHeader("Accept", "application/json");
            httpGet.setHeader("Authorization", "Bearer " + accessToken);

            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();

            switch (statusCode) {
                case HttpStatus.SC_OK:
                    StringBuilder stringBuilder = new StringBuilder();
                    HttpEntity entity = response.getEntity();

                    InputStream content = entity.getContent();
                    BufferedReader reader;
                    reader = new BufferedReader(new InputStreamReader(content));
                    String line;

                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    String json = stringBuilder.toString();

                    Log.d("DownloadJSONFeedTask",
                            "Download concluido: " + json.getBytes().length + " bytes de " + uri.toString() );

                    return json;

                case HttpStatus.SC_NO_CONTENT:
                    Log.d("DownloadJSONFeedTask",feed + " não precisa ser atualizado");

                    return null;
            }

        }
        return null;
    }

    private Long getLastUpdated(RemotePath feed) {

        String coluna=null;
        Uri contentUri = null;

        switch(feed){
            case EstoquePath:
                coluna = EstoqueProvider.LAST_UPDATED_TIMESTAMP;
                contentUri = EstoqueProvider.CONTENT_URI;
                break;
            case VendaPath:
                coluna = VendasProvider.LAST_UPDATED_TIMESTAMP;
                contentUri = VendasProvider.CONTENT_URI;
                break;
            case CaixaPath:
                FinanceiroDAO dao = new FinanceiroDAO(this.ctx);
                Long lastUpdated = dao.lastUpdated();
                dao.close();
                return lastUpdated;
            default:
                return 0L;
        }

        String[] projection = {String.format("MAX(%s)",coluna)};
        ContentResolver contentResolver = this.ctx.getContentResolver();
        Cursor cursor = contentResolver.query(contentUri,projection,null,null,null);
        cursor.moveToFirst();

        if (cursor.getCount() > 0){
            long timestamp = cursor.getLong(0);
            cursor.close();
            return timestamp;
        }else
            return 0L;
    }

    protected void onPostExecute(Void result) {
        if (onCompleteListener != null)
            onCompleteListener.run();
    }

}
