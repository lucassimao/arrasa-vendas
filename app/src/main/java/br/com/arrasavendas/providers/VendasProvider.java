package br.com.arrasavendas.providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import br.com.arrasavendas.DatabaseHelper;

public class VendasProvider extends ContentProvider {

    private static final String PROVIDER_NAME = VendasProvider.class.getName();

    public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/vendas");
    public final static String _ID = "_id";
    public final static String DATA_ENTREGA = "data_entrega";
    public final static String VENDEDOR = "vendedor_id";
    public final static String FORMA_PAGAMENTO = "forma_pagamento";
    public final static String STATUS = "status";
    public final static String CLIENTE = "cliente";
    public final static String TURNO_ENTREGA = "turno";
    public static final String CARRINHO = "carrinho";
    public static final String ANEXOS_JSON_ARRAY = "anexos_json_array";

    private static final UriMatcher uriMatcher;
    private static final int VENDAS = 1;
    private static final int VENDA_ID = 2;


    private SQLiteDatabase arrasaVendasDb;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "vendas", VENDAS);
        uriMatcher.addURI(PROVIDER_NAME, "vendas/#", VENDA_ID);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        int count = 0;

        switch (uriMatcher.match(uri)) {

            case VENDAS:
                arrasaVendasDb.delete(DatabaseHelper.TABLE_VENDAS, null, null);
                break;
            case VENDA_ID:
                String id = uri.getPathSegments().get(1);
                String whereClause = String.format("%s = %s", _ID, id);
                if (!TextUtils.isEmpty(selection))
                    whereClause += " AND (" + selection + ")";

                count = arrasaVendasDb.delete(DatabaseHelper.TABLE_VENDAS, whereClause, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("URI Desconhecida: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {

        switch (uriMatcher.match(uri)) {
            case VENDAS:
                return "vnd.android.cursor.dir/br.com.arrasavendas.vendas";
            case VENDA_ID:
                return "vnd.android.cursor.item/br.com.arrasavendas.vendas";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowID = arrasaVendasDb.insert(DatabaseHelper.TABLE_VENDAS, "", values);

        if (rowID > 0) {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);

            return _uri;
        } else {
            Log.e(PROVIDER_NAME, "Failed to insert row into " + uri);
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        Context ctx = getContext();
        DatabaseHelper dbHelper = new DatabaseHelper(ctx);
        this.arrasaVendasDb = dbHelper.getWritableDatabase();

        return (this.arrasaVendasDb != null);
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
        sqlBuilder.setTables(DatabaseHelper.TABLE_VENDAS);

        if (uriMatcher.match(uri) == VENDA_ID) {
            sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(1));
        }

        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = DATA_ENTREGA;
        }

        Cursor c = sqlBuilder.query(arrasaVendasDb, projection, selection, selectionArgs, null, null, sortOrder);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int count = 0;
        switch (uriMatcher.match(uri)) {
            case VENDAS:
                count = this.arrasaVendasDb.update(DatabaseHelper.TABLE_VENDAS, values, selection, selectionArgs);
                break;
            case VENDA_ID:
                String id = uri.getPathSegments().get(1);
                String whereClause = String.format("%s = %s", _ID, id);

                if (!TextUtils.isEmpty(selection))
                    whereClause += " AND (" + selection + ")";

                count = arrasaVendasDb.update(DatabaseHelper.TABLE_VENDAS, values, whereClause, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("URI Desconhecida: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;

    }

}
