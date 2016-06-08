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

import java.util.LinkedList;
import java.util.List;

import br.com.arrasavendas.Utilities;
import br.com.arrasavendas.model.ItemVenda;
import br.com.arrasavendas.providers.DownloadedImagesProvider;
import br.com.arrasavendas.providers.EstoqueProvider;

/**
 * Created by lsimaocosta on 29/02/16.
 */
public class EstoqueService {

    private static final String TAG = EstoqueService.class.getSimpleName();
    private final Context ctx;

    public EstoqueService(Context ctx) {
        this.ctx = ctx;
    }

    public void update(long id, JSONObject estoque) throws JSONException {
        Uri uri = EstoqueProvider.CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();

        ctx.getContentResolver().delete(uri, null, null);
        ctx.getContentResolver().delete(DownloadedImagesProvider.CONTENT_URI,
                DownloadedImagesProvider.ESTOQUE_ID + "=?", new String[]{String.valueOf(id)});

        ContentValues values = convertEstoqueJSONObject2ContentValue(estoque);
        this.ctx.getContentResolver().insert(EstoqueProvider.CONTENT_URI, values);

        List<ContentValues> fotos = convertJSONProdutoFotos2ContentValue(estoque);
        ctx.getContentResolver().bulkInsert(DownloadedImagesProvider.CONTENT_URI, fotos.toArray(new ContentValues[0]));

    }

    public final void save(JSONArray itens) {
        try {

            if (isTableEstoqueEmpty()) { // tabela vazia: bulk insert !
                ContentValues[] contentValues = new ContentValues[itens.length()];
                List<ContentValues> fotos = new LinkedList<>();

                for (int i = 0; i < itens.length(); ++i) {
                    JSONObject estoque = itens.getJSONObject(i);
                    ContentValues cv = convertEstoqueJSONObject2ContentValue(estoque);
                    contentValues[i] = cv;

                    fotos.addAll(convertJSONProdutoFotos2ContentValue(estoque));
                }

                ctx.getContentResolver().bulkInsert(EstoqueProvider.CONTENT_URI, contentValues);
                ctx.getContentResolver().bulkInsert(DownloadedImagesProvider.CONTENT_URI, fotos.toArray(new ContentValues[0]));

            } else
                for (int i = 0; i < itens.length(); ++i) {
                    JSONObject jsonObject = itens.getJSONObject(i);
                    long estoqueId = jsonObject.getLong("estoque_id");

                    Uri uri = EstoqueProvider.CONTENT_URI.buildUpon().
                            appendPath(String.valueOf(estoqueId)).build();

                    Cursor c = ctx.getContentResolver().query(uri,
                            new String[]{EstoqueProvider._ID}, null, null, null);

                    // se o estoque existe, atualiza; caso contrario, salva
                    if (c.getCount() == 1)
                        update(estoqueId, jsonObject);
                    else
                        save(jsonObject);

                    c.close();
                }
        } catch (JSONException e) {
            Log.d(TAG, "Erro ao converter json object de estoque em content value: ");
            e.printStackTrace();
        }
    }

    private boolean isTableEstoqueEmpty() {
        Cursor cursor = ctx.getContentResolver().query(EstoqueProvider.CONTENT_URI, new String[]{EstoqueProvider._ID}, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count == 0;
    }

    private void save(JSONObject jsonObject) throws JSONException {
        ContentValues values = convertEstoqueJSONObject2ContentValue(jsonObject);
        this.ctx.getContentResolver().insert(EstoqueProvider.CONTENT_URI, values);

        List<ContentValues> fotos = convertJSONProdutoFotos2ContentValue(jsonObject);
        ctx.getContentResolver().bulkInsert(DownloadedImagesProvider.CONTENT_URI, fotos.toArray(new ContentValues[0]));

    }

    private final List<ContentValues> convertJSONProdutoFotos2ContentValue(JSONObject estoque) throws JSONException {
        List<ContentValues> fotos = new LinkedList<>();

        String produtoNome = estoque.getString("produto_nome");
        String produtoNomeASCII = Utilities.excluirCaracteresEspeciais(produtoNome);
        String unidade = estoque.getString("unidade");
        int produtoId = estoque.getInt("produto_id");
        int estoqueId = estoque.getInt("estoque_id");
        JSONArray produtoFotos = estoque.getJSONArray("produto_fotos");

        for (int j = 0; j < produtoFotos.length(); j++) {
            ContentValues cv = new ContentValues();

            cv.put(DownloadedImagesProvider.IMAGE_NAME, produtoFotos.getString(j));
            cv.put(DownloadedImagesProvider.PRODUTO_ID, produtoId);
            cv.put(DownloadedImagesProvider.ESTOQUE_ID, estoqueId);
            cv.put(DownloadedImagesProvider.PRODUTO_NOME, produtoNome);
            cv.put(DownloadedImagesProvider.PRODUTO_ASCII, produtoNomeASCII);
            cv.put(DownloadedImagesProvider.UNIDADE, unidade);
            cv.put(DownloadedImagesProvider.IS_IGNORED, 0);

            fotos.add(cv);
        }
        return fotos;
    }

    @NonNull
    private final ContentValues convertEstoqueJSONObject2ContentValue(JSONObject estoque) throws JSONException {
        String produtoNome = estoque.getString("produto_nome");
        String produtoNomeASCII = Utilities.excluirCaracteresEspeciais(produtoNome);

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

        return values;
    }

    public void devolverItens(List<ItemVenda> itens) {
        for(ItemVenda item: itens){
            long id = item.getId();
            Uri uri = EstoqueProvider.CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();

            Cursor c = ctx.getContentResolver().query(uri,new String[]{EstoqueProvider.QUANTIDADE},null,null,null);
            c.moveToFirst();
            long quantidade = c.getLong(c.getColumnIndex(EstoqueProvider.QUANTIDADE));
            c.close();

            ContentValues cv = new ContentValues();
            cv.put(EstoqueProvider.QUANTIDADE,quantidade + item.getQuantidade());

            ctx.getContentResolver().update(uri,cv,null,null);
        }
    }
}
