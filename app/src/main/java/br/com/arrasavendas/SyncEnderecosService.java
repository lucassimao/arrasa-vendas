package br.com.arrasavendas;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import br.com.arrasavendas.providers.ClientesProvider;

/**
 * Created by lsimaocosta on 02/11/15.
 */
public class SyncEnderecosService extends IntentService {

    public SyncEnderecosService() {
        super("SyncEnderecosService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(":: SyncEnderecos :: ", "baixando os enderecos");

        //TODO tratar a inexistencia do TOKEN
        Application app = (Application) getApplicationContext();
        String accessToken = app.getAccessToken();

        try {

            long lastTimestamp = getLastTimestamp();

            String serverUrl = RemotePath.EnderecosPath.getUrl();
            String str = String.format("%s?lastDownloadedTimestamp=%d", serverUrl, lastTimestamp);

            Log.d(":: SyncEnderecos :: ", "Timestamp do ultimo cliente: "+lastTimestamp);

            URL url = new URL(str);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            //connection.setReadTimeout(15000);
            connection.setConnectTimeout(1500);
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.setUseCaches(false);
            connection.connect();

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {

                InputStream inputStream = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }

                JSONArray jsonArray = new JSONArray(sb.toString());
                Log.d(":: SyncEnderecos :: ", String.format("%d novos clientes",jsonArray.length()));
                if (jsonArray.length() > 0)
                    salvarNovosEnderecos(jsonArray);

            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private long getLastTimestamp() {
        String[] projection = {String.format("MAX(%s) as max_last_updated_timestamp",ClientesProvider.LAST_UPDATED_TIMESTAMP)};

        Cursor cursor = getContentResolver().query(ClientesProvider.CONTENT_URI,projection,null,null,null);
        cursor.moveToFirst();
        if (cursor.getCount() > 0){
            long timestamp = cursor.getLong(cursor.getColumnIndex("max_last_updated_timestamp"));
            cursor.close();
            return timestamp;
        }

        return 0;
    }

    private void salvarNovosEnderecos(JSONArray jsonArray) {
        for (int i = 0; i < jsonArray.length(); ++i) {

            try {

                JSONObject endereco = jsonArray.getJSONObject(i);

                long id = endereco.getLong("id");
                String cliente = endereco.getString("cliente");
                String celular = endereco.getString("celular");
                String dddCelular = endereco.getString("ddd_celular");
                String telefone = endereco.getString("telefone");
                String dddTelefone = endereco.getString("ddd_telefone");
                String complemento = endereco.getString("endereco");
                String uf = endereco.getString("uf");
                int idUf = endereco.getInt("id_uf");
                String cidade = endereco.getString("cidade");
                int idCidade = endereco.getInt("id_cidade");
                String bairro = endereco.getString("bairro");
                long lastUpdatedTimestamp = endereco.getLong("last_updated_unix_timestamp");

                ContentValues cv = new ContentValues();
                cv.put(ClientesProvider.CLIENTE_ID,id);
                cv.put(ClientesProvider.NOME,cliente);
                cv.put(ClientesProvider.CELULAR,celular);
                cv.put(ClientesProvider.DDD_CELULAR,dddCelular);
                cv.put(ClientesProvider.TELEFONE,telefone);
                cv.put(ClientesProvider.DDD_TELEFONE,dddTelefone);
                cv.put(ClientesProvider.ENDERECO,complemento);
                cv.put(ClientesProvider.ID_UF,idUf);
                cv.put(ClientesProvider.UF,uf);
                cv.put(ClientesProvider.CIDADE,cidade);
                cv.put(ClientesProvider.ID_CIDADE,idCidade);
                cv.put(ClientesProvider.BAIRRO,bairro);
                cv.put(ClientesProvider.LAST_UPDATED_TIMESTAMP,lastUpdatedTimestamp);

                getContentResolver().insert(ClientesProvider.CONTENT_URI, cv);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }
}