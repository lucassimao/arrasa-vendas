package br.com.arrasavendas;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import br.com.arrasavendas.financeiro.FinanceiroDAO;
import br.com.arrasavendas.providers.DownloadedImagesProvider;
import br.com.arrasavendas.providers.EstoqueProvider;
import br.com.arrasavendas.providers.VendasProvider;
import br.com.arrasavendas.service.EstoqueService;
import br.com.arrasavendas.service.VendaService;

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

        for (RemotePath remotePath : params) {

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
        VendaService service = new VendaService(ctx);

        for (int i = 0; i < itens.length(); ++i) {
            JSONObject venda = itens.getJSONObject(i);

            if (!venda.isNull("dataEntrega")) {
                service.save(venda);
            }

        }

    }

    private String downloadJSON(RemotePath feed) throws IOException {

        Application app = (Application) ctx.getApplicationContext();

        if (app.isAuthenticated()) {
            String accessToken = app.getAccessToken();
            Long lastUpdated = getLastUpdated(feed);

            Log.d("DownloadJSONFeedTask", feed + " lastUpdated for " + feed + ": " + lastUpdated);

            Uri uri = Uri.parse(feed.getUrl()).
                    buildUpon().
                    appendQueryParameter("lastUpdated", lastUpdated.toString()).
                    build();

            URL url = new URL(uri.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.connect();

            int statusCode = connection.getResponseCode();

            switch (statusCode) {
                case HttpURLConnection.HTTP_OK:
                    StringBuilder stringBuilder = new StringBuilder();

                    InputStream content = connection.getInputStream();
                    BufferedReader reader;
                    reader = new BufferedReader(new InputStreamReader(content));
                    String line = null;

                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    String json = stringBuilder.toString();

                    Log.d("DownloadJSONFeedTask",
                            "Download concluido: " + json.getBytes().length +
                                    " bytes de " + uri.toString());

                    return json;

                case HttpURLConnection.HTTP_NO_CONTENT:
                    Log.d("DownloadJSONFeedTask", feed + " não precisa ser atualizado");
                    return null;
            }

        }
        return null;
    }

    private Long getLastUpdated(RemotePath feed) {

        String coluna = null;
        Uri contentUri = null;

        switch (feed) {
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

        String[] projection = {String.format("MAX(%s)", coluna)};
        ContentResolver contentResolver = this.ctx.getContentResolver();
        Cursor cursor = contentResolver.query(contentUri, projection, null, null, null);
        cursor.moveToFirst();

        if (cursor.getCount() > 0) {
            long timestamp = cursor.getLong(0);
            cursor.close();
            return timestamp;
        } else
            return 0L;
    }

    protected void onPostExecute(Void result) {
        if (onCompleteListener != null)
            onCompleteListener.run();
    }

}
