package br.com.arrasavendas.entregas;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.Serializable;

import br.com.arrasavendas.DatabaseHelper;
import br.com.arrasavendas.providers.VendasProvider;

/**
 * Created by lsimaocosta on 25/02/16.
 */
public class AnexosDAO implements Serializable {

    private final Context ctx;
    private final Long vendaId;
    private final Uri uri;
    private transient SQLiteDatabase db;

    public AnexosDAO(Context ctx, Long vendaId) {
        this.db = new DatabaseHelper(ctx).getWritableDatabase();
        this.ctx = ctx;
        this.vendaId = vendaId;
        this.uri = VendasProvider.CONTENT_URI.buildUpon().appendPath(vendaId.toString()).build();
    }

    private String getAnexosJsonArray() {
        String[] projection = {VendasProvider.ANEXOS_JSON_ARRAY};

        ContentResolver contentResolver = this.ctx.getContentResolver();
        Cursor cursor = contentResolver.query(uri,projection, null, null, null);

        cursor.moveToFirst();
        String anexosJsonArray = cursor.getString(0);
        cursor.close();
        return anexosJsonArray;
    }

    public boolean addAnexo(String filename){
        String anexosJsonArray = getAnexosJsonArray();

        if (!TextUtils.isEmpty(anexosJsonArray)) {
            try {
                JSONArray array = new JSONArray(anexosJsonArray);
                array.put(filename);

                ContentValues cv = new ContentValues();
                cv.put(VendasProvider.ANEXOS_JSON_ARRAY, array.toString());
                ctx.getContentResolver().update(uri, cv, null, null);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        return false;
    }

    public String[] list() {
        String[] str = null;
        String anexosJsonArray = getAnexosJsonArray();

        if (!TextUtils.isEmpty(anexosJsonArray)) {

            try {
                JSONArray array = new JSONArray(anexosJsonArray);
                str = new String[array.length()];
                for (int i = 0; i < array.length(); ++i) {
                    str[i] = array.getString(i);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        return str;
    }

    public void delete(int position) {
        String anexosJsonArray = getAnexosJsonArray();

        if (!TextUtils.isEmpty(anexosJsonArray)) {
            try {
                // API Level 16 não dispõe do método remove, so foi acrescentado na API 19
                JSONArray array = new JSONArray(anexosJsonArray);

                JSONArray novoArray = new JSONArray();
                for (int i = 0; i < array.length(); ++i) {
                    if (i == position) continue;
                    novoArray.put(array.get(i));
                }

                ContentValues cv = new ContentValues();
                cv.put(VendasProvider.ANEXOS_JSON_ARRAY, novoArray.toString());
                ctx.getContentResolver().update(uri, cv, null,null);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        if (this.db != null)
            this.db.close();
    }
}
