package br.com.arrasavendas.service;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import br.com.arrasavendas.providers.VendasProvider;

/**
 * Created by lsimaocosta on 29/02/16.
 */
public class VendaService {

    private final Context ctx;

    public VendaService(Context ctx){
        this.ctx = ctx;
    }
    public void save(JSONObject venda) throws JSONException {
        ContentValues values = new ContentValues();
        values.put(VendasProvider._ID, venda.getInt("id"));
        values.put(VendasProvider.VENDEDOR, venda.getString("vendedor"));
        values.put(VendasProvider.CLIENTE, venda.getString("cliente"));
        values.put(VendasProvider.LAST_UPDATED_TIMESTAMP, venda.getLong("last_updated"));
        values.put(VendasProvider.DATA_ENTREGA, venda.getLong("dataEntrega"));
        values.put(VendasProvider.FORMA_PAGAMENTO, venda.getString("formaPagamento"));
        values.put(VendasProvider.TURNO_ENTREGA, venda.getString("turnoEntrega"));
        values.put(VendasProvider.STATUS, venda.getString("status"));
        values.put(VendasProvider.ABATIMENTO, venda.getString("abatimentoEmCentavos"));
        values.put(VendasProvider.CARRINHO, venda.getString("itens"));
        values.put(VendasProvider.ANEXOS_JSON_ARRAY, venda.getString("anexos_json_array"));
        values.put(VendasProvider.SERVICO_CORREIOS, venda.getString("servicoCorreio"));
        values.put(VendasProvider.FRETE, venda.getString("freteEmCentavos"));
        values.put(VendasProvider.CODIGO_RASTREIO, venda.getString("codigoRastreio"));
        values.put(VendasProvider.FLAG_VAI_BUSCAR, venda.getBoolean("flagClienteVaiBuscar")?1:0);
        values.put(VendasProvider.FLAG_JA_BUSCOU, venda.getBoolean("flagClienteJaBuscou")?1:0);


        this.ctx.getContentResolver().insert(VendasProvider.CONTENT_URI, values);
    }

    public void update(Long id,JSONObject venda) throws JSONException {
        delete(id);
        save(venda);
    }

    public void delete(Long id) {
        Uri uri = VendasProvider.CONTENT_URI.buildUpon().appendPath(id.toString()).build();
        ctx.getContentResolver().delete(uri, null,null);
    }
}
