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
 * Created by lsimaocosta on 13/03/16.
 */
public class CidadesProvider  extends ContentProvider {

    private static final String PROVIDER_NAME = CidadesProvider.class.getName();


    public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/cidades");

    public final static String UF = "uf";
    public final static String NOME = "nome";
    public static final String _ID = "_id";


    private static final UriMatcher uriMatcher;
    private static final int ALL = 1;
    private static final int LOOKUP_BY_ID = 2;


    private SQLiteDatabase arrasaVendasDb;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "cidades", ALL);
        uriMatcher.addURI(PROVIDER_NAME, "cidades/#", LOOKUP_BY_ID);
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
        sqlBuilder.setTables(DatabaseHelper.TABLE_CIDADE);

        if (uriMatcher.match(uri) == LOOKUP_BY_ID) {
            sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(1));
        }

        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = UF;
        }

        Cursor c = sqlBuilder.query(arrasaVendasDb,
                projection,selection,selectionArgs,null,null,sortOrder);

        return c;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int qty = values.length;
        long rowId = 0;
        arrasaVendasDb.beginTransaction();

        for(ContentValues cv : values) {
            rowId = arrasaVendasDb.insert(DatabaseHelper.TABLE_CIDADE, "", cv);
            if (rowId<=0) --qty;
        }
        arrasaVendasDb.setTransactionSuccessful();
        arrasaVendasDb.endTransaction();
        return qty;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowID = arrasaVendasDb.insert(DatabaseHelper.TABLE_CIDADE, "", values);

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
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return arrasaVendasDb.delete(DatabaseHelper.TABLE_CIDADE,selection,selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return arrasaVendasDb.update(DatabaseHelper.TABLE_CIDADE,values,selection,selectionArgs);

    }
}
