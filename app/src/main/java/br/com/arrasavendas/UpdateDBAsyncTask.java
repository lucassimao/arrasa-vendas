package br.com.arrasavendas;

import android.content.Context;
import android.content.Intent;
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
import br.com.arrasavendas.service.EstoqueService;
import br.com.arrasavendas.service.VendaService;
import br.com.arrasavendas.util.Response;

public class UpdateDBAsyncTask extends AsyncTask<Void, Void, Response> {

    private final static String TAG = "UpdateDBAsyncTask";
    private Context ctx;
    private OnCompleteListener listener;

    public UpdateDBAsyncTask(Context ctx, OnCompleteListener onCompleteListener) {
        super();
        this.ctx = ctx;
        this.listener = onCompleteListener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Response doInBackground(Void... params) {

        Response response = null;
        boolean clearFlag = true;

        synchronized (Application.class) {

            try {
                response = downloadJSON();

                if (response!= null && !TextUtils.isEmpty(response.getMessage())) {
                    String jsonString = response.getMessage();

                    JSONObject data = new JSONObject(jsonString);

                    if (data.has("estoques")) {
                        JSONArray estoques = data.getJSONArray("estoques");
                        Log.d(TAG, "salvando " + estoques.length() + " registros de estoque ...");
                        saveEstoques(estoques);
                    }

                    if (data.has("vendas")) {
                        JSONArray vendas = data.getJSONArray("vendas");
                        Log.d(TAG, "salvando " + vendas.length() + " registros de venda ...");
                        saveVendas(vendas);

                        // aproveitando p/ verificar se houve alguma altualização nos endereços
                        Intent intent = new Intent(ctx, SyncEnderecosService.class);
                        ctx.startService(intent);
                    }

                    if (data.has("caixa")) {
                        String resumoCaixa = data.getString("caixa");
                        Log.d(TAG, "salvando informações do caixa ...");
                        salvarJSONCaixa(resumoCaixa);
                    }

                    Log.d(TAG, "importação concluida! ");
                }

            } catch (SocketTimeoutException | ConnectException e) {
                e.printStackTrace();
                String message = ctx.getString(R.string.connection_error_msg);
                clearFlag = false;
                return new Response(message, -1);

            } catch (Exception e) {
                e.printStackTrace();
                clearFlag = false;
                return new Response(e.getMessage(), -1);
            }

            if (clearFlag)
                Application.setDBUpdated();
        }

        return response;
    }

    private void salvarJSONCaixa(String jsonString) {
        FinanceiroDAO dao = new FinanceiroDAO(this.ctx);
        dao.deleteAll();
        dao.save(jsonString);
        dao.close();
    }

    private void saveEstoques(JSONArray estoques) throws IOException, JSONException {

        if (estoques.length() > 0) {
            EstoqueService service = new EstoqueService(ctx);
            service.save(estoques);
        }
    }

    private void saveVendas(JSONArray vendas) throws IOException, JSONException {

        if (vendas.length() > 0) {
            VendaService service = new VendaService(ctx);
            service.save(vendas);
        }

    }

    private final Response downloadJSON() throws IOException {

        Application app = (Application) ctx.getApplicationContext();
        Response response = null;

        if (app.isAuthenticated()) {
            Uri.Builder builder = Uri.parse(RemotePath.SyncPath.getUrl()).buildUpon();

            long vendaLastUpdated = Application.getVendasLastUpdated();
            Log.d(TAG, "Venda lastUpdated : " + vendaLastUpdated);
            if (vendaLastUpdated >= 0)
                builder.appendQueryParameter("vendaLastUpdated", String.valueOf(vendaLastUpdated));

            long estoqueLastUpdated = Application.getEstoquesLastUpdated();
            Log.d(TAG, "Estoque lastUpdated : " + estoqueLastUpdated);
            if (estoqueLastUpdated >= 0)
                builder.appendQueryParameter("estoqueLastUpdated", String.valueOf(estoqueLastUpdated));

            String accessToken = app.getAccessToken();
            Uri uri = builder.build();

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
                    int length = payload.getBytes().length;
                    String msg = "Download concluido: " + length + " bytes de " + uri.toString();
                    Log.d(TAG, msg);
                    break;
                case HttpURLConnection.HTTP_NO_CONTENT:
                    payload = null;
                    Log.d(TAG, " DB esta atualizado ... não precisava ser chamado");
                    break;
                default:
                    payload = readStream(connection.getErrorStream());
                    Log.d(TAG, "Erro em " + RemotePath.SyncPath + ": " + payload);
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

    protected void onPostExecute(Response result) {
        if (listener != null)
            listener.run(result);
    }

    public interface OnCompleteListener {
        void run(Response response);
    }

}
