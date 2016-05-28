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
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import br.com.arrasavendas.DatabaseHelper;

/**
 * Created by lsimaocosta on 04/12/15.
 *
 * Provider utilizado para acessar informações de
 * clientes que já foram previamente cadastradas no sistema
 *
 */
public class ClientesProvider extends ContentProvider {

    private static final String PROVIDER_NAME = ClientesProvider.class.getName();

    public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/clientes");
    public final static String CLIENTE_ID = "cliente_id";
    public final static String NOME = "nome";
    public final static String CELULAR = "celular";
    public final static String DDD_CELULAR = "ddd_celular";
    public final static String TELEFONE = "telefone";
    public final static String DDD_TELEFONE = "ddd_telefone";
    public final static String ENDERECO = "endereco";
    public final static String UF = "uf";
    public final static String ID_UF= "id_uf";
    public static final String CIDADE = "cidade";
    public static final String ID_CIDADE = "id_cidade";
    public static final String BAIRRO = "bairro";
    public static final String LAST_UPDATED_TIMESTAMP = "last_updated_timestamp";

    private static final UriMatcher uriMatcher;
    private static final int ALL = 1;
    private static final int CLIENTE_LOOKUP = 2;


    private SQLiteDatabase arrasaVendasDb;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "clientes", ALL);
        uriMatcher.addURI(PROVIDER_NAME, "clientes/#", CLIENTE_LOOKUP);
    }


    @Override
    public boolean onCreate() {
        Context ctx = getContext();
        DatabaseHelper dbHelper = new DatabaseHelper(ctx);
        this.arrasaVendasDb = dbHelper.getWritableDatabase();

        return (this.arrasaVendasDb != null);
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
        sqlBuilder.setTables(DatabaseHelper.TABLE_CLIENTES);

        if (uriMatcher.match(uri) == CLIENTE_LOOKUP) {
            sqlBuilder.appendWhere(CLIENTE_ID + " = " + uri.getPathSegments().get(1));
        }

        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = NOME;
        }

        Cursor c = sqlBuilder.query(arrasaVendasDb, projection, selection, selectionArgs, null, null, sortOrder);
        return c;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case ALL:
                return "vnd.android.cursor.dir/br.com.arrasavendas.clientes";
            case CLIENTE_LOOKUP:
                return "vnd.android.cursor.item/br.com.arrasavendas.cliente";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowID = arrasaVendasDb.insert(DatabaseHelper.TABLE_CLIENTES, "", values);

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
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int qty = values.length;
        long rowId = 0;
        arrasaVendasDb.beginTransaction();

        for(ContentValues cv : values) {
            rowId = arrasaVendasDb.insert(DatabaseHelper.TABLE_CLIENTES, "", cv);
            if (rowId<=0) --qty;
        }
        arrasaVendasDb.setTransactionSuccessful();
        arrasaVendasDb.endTransaction();
        return qty;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        int count = 0;

        switch (uriMatcher.match(uri)) {

            case ALL:
                arrasaVendasDb.delete(DatabaseHelper.TABLE_CLIENTES, null, null);
                break;
            case CLIENTE_LOOKUP:
                String id = uri.getPathSegments().get(1);
                String whereClause = String.format("%s = %s", CLIENTE_ID, id);
                if (!TextUtils.isEmpty(selection))
                    whereClause += " AND (" + selection + ")";

                count = arrasaVendasDb.delete(DatabaseHelper.TABLE_CLIENTES, whereClause, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("URI Desconhecida: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;
        switch (uriMatcher.match(uri)) {
            case ALL:
                count = this.arrasaVendasDb.update(DatabaseHelper.TABLE_CLIENTES, values, selection, selectionArgs);
                break;
            case CLIENTE_LOOKUP:
                String id = uri.getPathSegments().get(1);
                String whereClause = String.format("%s = %s", CLIENTE_ID, id);

                if (!TextUtils.isEmpty(selection))
                    whereClause += " AND (" + selection + ")";

                count = arrasaVendasDb.update(DatabaseHelper.TABLE_CLIENTES, values, whereClause, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("URI Desconhecida: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
