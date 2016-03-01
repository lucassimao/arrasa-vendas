package br.com.arrasavendas;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import br.com.arrasavendas.financeiro.FinanceiroDAO;
import br.com.arrasavendas.providers.DownloadedImagesProvider;
import br.com.arrasavendas.providers.EstoqueProvider;
import br.com.arrasavendas.providers.VendasProvider;
import br.com.arrasavendas.service.EstoqueService;
import br.com.arrasavendas.service.VendaService;
import br.com.arrasavendas.util.Response;

public class DownloadJSONAsyncTask extends AsyncTask<RemotePath, Void, Response> {

    private Context ctx;
    private OnCompleteListener listener;


    public DownloadJSONAsyncTask(Context ctx, OnCompleteListener onCompleteListener) {
        super();
        this.ctx = ctx;
        this.listener = onCompleteListener;
    }

    @Override
    protected Response doInBackground(RemotePath... params) {

        RemotePath remotePath = params[0];
        Response response = null;

        try {

            response = downloadJSON(remotePath);

            if (response != null) {

                String jsonString = response.getMessage();

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
            }

        }catch(SocketTimeoutException | ConnectException e) {
            e.printStackTrace();
            String message = ctx.getString(R.string.connection_error_msg);
            return new Response(message,-1);

        }
        catch (Exception e) {
            e.printStackTrace();
            return new Response(e.getMessage(),-1);
        }

        return response;
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

    private Response downloadJSON(RemotePath feed) throws IOException {

        Application app = (Application) ctx.getApplicationContext();
        Response response = null;

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
            connection.setConnectTimeout(5000);
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.connect();

            int statusCode = connection.getResponseCode();
            String payload = null;

            switch (statusCode) {
                case HttpURLConnection.HTTP_OK:

                    payload = readStream(connection.getInputStream());

                    Log.d("DownloadJSONFeedTask",
                            "Download concluido: " + payload.getBytes().length +
                                    " bytes de " + uri.toString());
                    break;
                case HttpURLConnection.HTTP_NO_CONTENT:
                    payload = null;
                    Log.d("DownloadJSONFeedTask", feed + " não precisa ser atualizado");
                    break;
                default:
                    payload = readStream(connection.getErrorStream());
                    Log.d("DownloadJSONFeedTask", "Erro em " + feed +": " + payload);
            }

            response = new Response(payload, statusCode);
        }

        return response;
    }

    @NonNull
    private String readStream(InputStream content) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader;
        reader = new BufferedReader(new InputStreamReader(content));
        String line = null;

        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
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

    protected void onPostExecute(Response result) {
        if (listener != null)
            listener.run(result);
    }

    public interface OnCompleteListener {
        void run(Response response);
    }

}
