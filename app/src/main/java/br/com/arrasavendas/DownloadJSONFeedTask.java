package br.com.arrasavendas;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

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

import br.com.arrasavendas.model.FinanceiroDAO;
import br.com.arrasavendas.providers.DownloadedImagesProvider;
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
            if (!TextUtils.isEmpty(jsonString)) {

                switch (this.feed) {
                    case EstoquePath:
                    case VendaPath:
                        salvarJSON(jsonString);
                        break;
                    case CaixaPath:
                        exportarInformacoesDoCaixa(jsonString);
                        break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void exportarInformacoesDoCaixa(String jsonString) {
        FinanceiroDAO dao = new FinanceiroDAO(this.ctx);
        dao.deleteAll();
        dao.save(jsonString);
    }

    private void salvarJSON(String jsonString) throws IOException, JSONException {

        switch (this.feed) {
            case EstoquePath:
                this.ctx.getContentResolver().delete(EstoqueProvider.CONTENT_URI, null, null);
                break;
            case VendaPath:
                this.ctx.getContentResolver().delete(VendasProvider.CONTENT_URI, null, null);
                break;
        }


        JSONArray itens = new JSONArray(jsonString);

        for (int i = 0; i < itens.length(); ++i) {
            ContentValues values;

            switch (this.feed) {
                case EstoquePath:

                    JSONObject estoque = itens.getJSONObject(i);

                    String produtoNome = estoque.getString("produto_nome");
                    // excluindo acentos
                    String produtoNomeASCII = Normalizer.normalize(produtoNome, Normalizer.Form.NFD)
                            .replaceAll("[^\\p{ASCII}]", "");

                    values = new ContentValues();
                    values.put(EstoqueProvider._ID, estoque.getLong("estoque_id"));
                    values.put(EstoqueProvider.PRODUTO, produtoNome);
                    values.put(EstoqueProvider.PRODUTO_ASCII, produtoNomeASCII);
                    values.put(EstoqueProvider.PRODUTO_ID, estoque.getInt("produto_id"));
                    values.put(EstoqueProvider.PRECO_A_VISTA, estoque.getLong("produto_precoAVistaEmCentavos"));
                    values.put(EstoqueProvider.PRECO_A_PRAZO, estoque.getLong("produto_precoAPrazoEmCentavos"));
                    values.put(EstoqueProvider.UNIDADE, estoque.getString("unidade"));
                    values.put(EstoqueProvider.QUANTIDADE, estoque.getInt("quantidade"));

                    JSONArray produtoFotos = estoque.getJSONArray("produto_fotos");

                    for (int j = 0; j < produtoFotos.length(); j++) {
                        ContentValues cv = new ContentValues();
                        cv.put(DownloadedImagesProvider.IMAGE_NAME, produtoFotos.getString(j));
                        cv.put(DownloadedImagesProvider.PRODUTO_ID, estoque.getInt("produto_id"));
                        cv.put(DownloadedImagesProvider.PRODUTO_NOME, produtoNome);
                        cv.put(DownloadedImagesProvider.PRODUTO_ASCII, produtoNomeASCII);
                        cv.put(DownloadedImagesProvider.UNIDADE, estoque.getString("unidade"));
                        cv.put(DownloadedImagesProvider.IS_IGNORED, 0);

                        this.ctx.getContentResolver().insert(DownloadedImagesProvider.CONTENT_URI, cv);
                    }
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
                        values.put(VendasProvider.TURNO_ENTREGA, venda.getString("turnoEntrega"));
                        values.put(VendasProvider.STATUS, venda.getString("status"));
                        values.put(VendasProvider.CARRINHO, venda.getString("itens"));
                        values.put(VendasProvider.ANEXOS_JSON_ARRAY, venda.getString("anexos_json_array"));

                        this.ctx.getContentResolver().insert(VendasProvider.CONTENT_URI, values);
                    }

                    break;
            }

        }

    }

    private String downloadJSON() throws IOException {

        Application app = (Application) ctx.getApplicationContext();

        if (app.isAuthenticated()) {
            String accessToken = app.getAccessToken();

            HttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(this.feed.getUrl());
            httpGet.setHeader("Accept", "application/json");
            httpGet.setHeader("Authorization", "Bearer " + accessToken);

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
        }
        return null;

    }

    protected void onPostExecute(Void result) {
        if (onCompleteListener != null)
            onCompleteListener.run();
    }

}
