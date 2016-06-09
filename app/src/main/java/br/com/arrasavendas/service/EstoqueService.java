package br.com.arrasavendas.service;

import android.content.ClipData;
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

    private void save(JSONObject estoque) throws JSONException {
        ContentValues values = convertEstoqueJSONObject2ContentValue(estoque);
        this.ctx.getContentResolver().insert(EstoqueProvider.CONTENT_URI, values);

        List<ContentValues> fotos = convertJSONProdutoFotos2ContentValue(estoque);
        ctx.getContentResolver().bulkInsert(DownloadedImagesProvider.CONTENT_URI, fotos.toArray(new ContentValues[0]));

    }

    public final void save(JSONArray itens) {
        try {

            if (isTableEstoqueEmpty()) {
                Log.d(TAG,"Tabela de estoques vazia ... fazendo bulk insert !");

                ContentValues[] contentValues = new ContentValues[itens.length()];
                List<ContentValues> fotos = new LinkedList<>();

                for (int i = 0; i < itens.length(); ++i) {
                    JSONObject estoque = itens.getJSONObject(i);
                    contentValues[i] = convertEstoqueJSONObject2ContentValue(estoque);
                    fotos.addAll(convertJSONProdutoFotos2ContentValue(estoque));
                }

                ctx.getContentResolver().bulkInsert(EstoqueProvider.CONTENT_URI, contentValues);
                ctx.getContentResolver().bulkInsert(DownloadedImagesProvider.CONTENT_URI, fotos.toArray(new ContentValues[0]));

            } else {
                String[] projection = {EstoqueProvider._ID};

                for (int i = 0; i < itens.length(); ++i) {
                    JSONObject jsonObject = itens.getJSONObject(i);
                    long estoqueId = jsonObject.getLong("estoque_id");

                    Uri.Builder builder = EstoqueProvider.CONTENT_URI.buildUpon();
                    Uri uri = builder.appendPath(String.valueOf(estoqueId)).build();

                    Cursor c = ctx.getContentResolver().query(uri, projection, null, null, null);

                    // se o estoque existe, atualiza; caso contrario, salva
                    int count = c.getCount();
                    c.close();

                    if (count == 1)
                        update(estoqueId, jsonObject);
                    else
                        save(jsonObject);

                }
            }
        } catch (JSONException e) {
            Log.d(TAG, "Erro ao converter json object de estoque em content value: ");
            e.printStackTrace();
        }
    }

    private boolean isTableEstoqueEmpty() {
        String[] projection = {EstoqueProvider._ID};

        Cursor cursor = ctx.getContentResolver().query(EstoqueProvider.CONTENT_URI, projection, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count == 0;
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
            cv.put(DownloadedImagesProvider.IS_IGNORED, 0); // false

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

    // devolvendo itens p/ o estoque
    public void reportItens(List<ItemVenda> itens) {
        String selection = EstoqueProvider.PRODUTO_ID + "=? and " + EstoqueProvider.UNIDADE + "=?";
        String[] projection = {EstoqueProvider.QUANTIDADE};

        for (ItemVenda item : itens) {

            String produtoId = item.getProdutoID().toString();
            String unidade = item.getUnidade();
            String[] selectionArgs = {produtoId, unidade};

            Cursor c = ctx.getContentResolver().query(EstoqueProvider.CONTENT_URI,
                    projection, selection, selectionArgs, null);
            c.moveToFirst();

            int quantidadeAtual = c.getInt(c.getColumnIndex(EstoqueProvider.QUANTIDADE));
            c.close();

            ContentValues cv = new ContentValues();
            cv.put(EstoqueProvider.QUANTIDADE, quantidadeAtual + item.getQuantidade());

            ctx.getContentResolver().update(EstoqueProvider.CONTENT_URI, cv, selection, selectionArgs);

        }
    }

    // removendo do estoque
    public void retirarItens(List<ItemVenda> itens) {
        String[] projection = {EstoqueProvider.QUANTIDADE};
        String selection = EstoqueProvider.PRODUTO_ID + "=? and " + EstoqueProvider.UNIDADE + "=?";

        for (ItemVenda item : itens) {

            String[] selectionArgs = {item.getProdutoID().toString(), item.getUnidade()};
            Cursor c = ctx.getContentResolver().query(EstoqueProvider.CONTENT_URI,
                    projection, selection, selectionArgs, null);
            c.moveToFirst();

            int quantidadeAtual = c.getInt(c.getColumnIndex(EstoqueProvider.QUANTIDADE));
            c.close();

            ContentValues cv = new ContentValues();
            cv.put(EstoqueProvider.QUANTIDADE, quantidadeAtual - item.getQuantidade());

            ctx.getContentResolver().update(EstoqueProvider.CONTENT_URI, cv, selection, selectionArgs);
        }
    }
}
