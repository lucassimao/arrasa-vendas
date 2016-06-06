package br.com.arrasavendas.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.arrasavendas.providers.VendasProvider;

/**
 * Created by lsimaocosta on 29/02/16.
 */
public class VendaService {

    private static final String TAG = VendaService.class.getSimpleName();
    private final Context ctx;

    public VendaService(Context ctx) {
        this.ctx = ctx;
    }

    public final void delete(Long id) {
        Log.d(TAG, "excluindo venda #" + id);
        Uri uri = VendasProvider.CONTENT_URI.buildUpon().appendPath(id.toString()).build();
        ctx.getContentResolver().delete(uri, null, null);
    }

    public final void update(Long id, JSONObject venda) throws JSONException {
        Uri uri = VendasProvider.CONTENT_URI.buildUpon().appendPath(id.toString()).build();
        ContentValues values = convertJSONObject2ContentValue(venda);

        ctx.getContentResolver().update(uri, values, null, null);
    }

    public final void save(JSONObject venda) throws JSONException {
        ContentValues values = convertJSONObject2ContentValue(venda);
        this.ctx.getContentResolver().insert(VendasProvider.CONTENT_URI, values);
    }

    public final void save(JSONArray itens) {

        try {

            if (isTableVendaEmpty()) { // estando vazio, faz bulk insert
                Log.d(TAG,"Tabela de vendas vazia, fazendo bulk insert!");

                ContentValues[] contentValues = new ContentValues[itens.length()];
                for (int i = 0; i < itens.length(); ++i)
                    contentValues[i] = convertJSONObject2ContentValue(itens.getJSONObject(i));

                ctx.getContentResolver().bulkInsert(VendasProvider.CONTENT_URI, contentValues);

            } else
                for (int i = 0; i < itens.length(); ++i) {
                    JSONObject jsonObject = itens.getJSONObject(i);
                    long vendaId = jsonObject.getLong("id");

                    Uri uri = VendasProvider.CONTENT_URI.buildUpon().
                            appendPath(String.valueOf(vendaId)).build();
                    Cursor c = ctx.getContentResolver().query(uri, new String[]{VendasProvider._ID}, null, null, null);

                    // se a venda não existe, salva; caso contrario, atualiza
                    if (c.getCount() == 0) {
                        Log.d(TAG,"VENDA #"+vendaId+ " nao existe, save!");
                        save(jsonObject);
                    }
                    else {
                        Log.d(TAG,"VENDA #"+vendaId+ " ja existe, update!");
                        update(vendaId, jsonObject);
                    }
                    c.close();
                }

        } catch (JSONException e) {
            Log.d(TAG, "Erro ao converter json object da venda em content value: ");
            e.printStackTrace();
        }
    }

    private boolean isTableVendaEmpty() {
        Cursor cursor = ctx.getContentResolver().query(VendasProvider.CONTENT_URI, new String[]{VendasProvider._ID}, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count == 0;
    }


    @NonNull
    private ContentValues convertJSONObject2ContentValue(JSONObject venda) throws JSONException {
        ContentValues values = new ContentValues();
        values.put(VendasProvider._ID, venda.getInt("id"));
        values.put(VendasProvider.VENDEDOR, venda.getString("vendedor"));
        values.put(VendasProvider.CLIENTE, venda.getString("cliente"));
        values.put(VendasProvider.LAST_UPDATED_TIMESTAMP, venda.getLong("last_updated"));

        if (!venda.isNull("dataEntrega"))
            values.put(VendasProvider.DATA_ENTREGA, venda.getLong("dataEntrega"));
        else
            values.put(VendasProvider.DATA_ENTREGA, -1); // vendas a serem enviadas pelos correios

        values.put(VendasProvider.FORMA_PAGAMENTO, venda.getString("formaPagamento"));
        values.put(VendasProvider.TURNO_ENTREGA, venda.getString("turnoEntrega"));
        values.put(VendasProvider.STATUS, venda.getString("status"));
        values.put(VendasProvider.ABATIMENTO, venda.getString("abatimentoEmCentavos"));
        values.put(VendasProvider.CARRINHO, venda.getString("itens"));
        values.put(VendasProvider.ANEXOS_JSON_ARRAY, venda.getString("anexos_json_array"));
        values.put(VendasProvider.SERVICO_CORREIOS, venda.getString("servicoCorreio"));
        values.put(VendasProvider.FRETE, venda.getString("freteEmCentavos"));
        values.put(VendasProvider.CODIGO_RASTREIO, venda.getString("codigoRastreio"));
        values.put(VendasProvider.FLAG_VAI_BUSCAR, venda.getBoolean("flagClienteVaiBuscar") ? 1 : 0);
        values.put(VendasProvider.FLAG_JA_BUSCOU, venda.getBoolean("flagClienteJaBuscou") ? 1 : 0);
        return values;
    }
}
