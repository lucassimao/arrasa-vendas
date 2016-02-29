package br.com.arrasavendas.service;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Normalizer;

import br.com.arrasavendas.providers.DownloadedImagesProvider;
import br.com.arrasavendas.providers.EstoqueProvider;

/**
 * Created by lsimaocosta on 29/02/16.
 */
public class EstoqueService {

    private final Context ctx;

    public EstoqueService(Context ctx) {
        this.ctx = ctx;
    }

    public void save(JSONObject estoque) throws JSONException {
        String produtoNome = estoque.getString("produto_nome");
        // excluindo acentos
        String produtoNomeASCII = Normalizer.normalize(produtoNome, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");

        ContentValues values = new ContentValues();
        values.put(EstoqueProvider._ID, estoque.getLong("estoque_id"));
        values.put(EstoqueProvider.PRODUTO, produtoNome);
        values.put(EstoqueProvider.PRODUTO_ASCII, produtoNomeASCII);
        values.put(EstoqueProvider.PRODUTO_ID, estoque.getInt("produto_id"));
        values.put(EstoqueProvider.LAST_UPDATED_TIMESTAMP, estoque.getLong("last_updated"));
        values.put(EstoqueProvider.PRECO_A_VISTA, estoque.getLong("produto_precoAVistaEmCentavos"));
        values.put(EstoqueProvider.PRECO_A_PRAZO, estoque.getLong("produto_precoAPrazoEmCentavos"));
        values.put(EstoqueProvider.UNIDADE, estoque.getString("unidade"));
        values.put(EstoqueProvider.QUANTIDADE, estoque.getInt("quantidade"));

        JSONArray produtoFotos = estoque.getJSONArray("produto_fotos");

        for (int j = 0; j < produtoFotos.length(); j++) {
            ContentValues cv = new ContentValues();
            cv.put(DownloadedImagesProvider.IMAGE_NAME, produtoFotos.getString(j));
            cv.put(DownloadedImagesProvider.PRODUTO_ID, estoque.getInt("produto_id"));
            cv.put(DownloadedImagesProvider.ESTOQUE_ID, estoque.getInt("estoque_id"));
            cv.put(DownloadedImagesProvider.PRODUTO_NOME, produtoNome);
            cv.put(DownloadedImagesProvider.PRODUTO_ASCII, produtoNomeASCII);
            cv.put(DownloadedImagesProvider.UNIDADE, estoque.getString("unidade"));
            cv.put(DownloadedImagesProvider.IS_IGNORED, 0);

            this.ctx.getContentResolver().insert(DownloadedImagesProvider.CONTENT_URI, cv);
        }

        this.ctx.getContentResolver().insert(EstoqueProvider.CONTENT_URI, values);
    }

    public void update(Long id, JSONObject estoque) throws JSONException {
        Uri uri = EstoqueProvider.CONTENT_URI.buildUpon().appendPath(id.toString()).build();

        ctx.getContentResolver().delete(uri, null, null);
        ctx.getContentResolver().delete(DownloadedImagesProvider.CONTENT_URI,
                DownloadedImagesProvider.ESTOQUE_ID + "=?", new String[]{id.toString()});

        save(estoque);

    }
}
