package br.com.arrasavendas.providers;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

import br.com.arrasavendas.DatabaseHelper;


public class EstoqueProvider extends ContentProvider {

    public static final String AUTHORITHY = EstoqueProvider.class.getName();

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITHY + "/estoque");
    // representa os produtos que tiver no minimo 1 item em estoque
    public static final Uri CONTENT_URI_PRODUTOS = Uri.parse(CONTENT_URI.toString() + "/produtos");

    public final static String _ID = "_id";
    public final static String PRODUTO = "produto_nome";
    public final static String PRODUTO_ASCII = "produto_nome_ascii";
    public final static String PRODUTO_ID = "produto_id";
    public final static String UNIDADE = "unidade";
    public final static String QUANTIDADE = "quantidade";
    public final static String PRECO_A_VISTA = "prevoAVista";
    public final static String PRECO_A_PRAZO = "prevoAPrazo";
    public final static String LAST_UPDATED_TIMESTAMP = "last_updated_timestamp";


    private static final UriMatcher uriMatcher;
    private static final int ESTOQUE_ALL = 1;
    private static final int ESTOQUE_ID = 2;
    private static final int ESTOQUE_PRODUTOS = 3;

    private SQLiteDatabase arrasaVendasDb;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITHY, "estoque", ESTOQUE_ALL);
        uriMatcher.addURI(AUTHORITHY, "estoque/#", ESTOQUE_ID);
        uriMatcher.addURI(AUTHORITHY, "estoque/produtos", ESTOQUE_PRODUTOS);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        int count = 0;

        switch (uriMatcher.match(uri)) {

            case ESTOQUE_ALL:
                count = arrasaVendasDb.delete(DatabaseHelper.TABLE_ESTOQUE, selection, selectionArgs);
                break;
            case ESTOQUE_ID:
                String id = uri.getPathSegments().get(1);
                String whereClause = String.format("%s = %s", _ID, id);
                if (!TextUtils.isEmpty(selection))
                    whereClause += " AND (" + selection + ")";

                count = arrasaVendasDb.delete(DatabaseHelper.TABLE_ESTOQUE, whereClause, selectionArgs);
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
            case ESTOQUE_PRODUTOS:
                return "vnd.android.cursor.dir/br.com.arrasavendas.produto";
            case ESTOQUE_ALL:
                return "vnd.android.cursor.dir/br.com.arrasavendas.estoque";
            case ESTOQUE_ID:
                return "vnd.android.cursor.item/br.com.arrasavendas.estoque";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowID = arrasaVendasDb.insert(DatabaseHelper.TABLE_ESTOQUE, "", values);

        if (rowID > 0) {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        } else {
            Log.e("EstoqueProvider", "Failed to insert row into " + uri);
        }
        return null;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int qty = values.length;
        long rowId = 0;
        arrasaVendasDb.beginTransaction();

        for(ContentValues cv : values) {
            rowId = arrasaVendasDb.insert(DatabaseHelper.TABLE_ESTOQUE, "", cv);
            if (rowId<=0) --qty;
        }
        arrasaVendasDb.setTransactionSuccessful();
        arrasaVendasDb.endTransaction();
        return qty;
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        return super.applyBatch(operations);
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
        sqlBuilder.setTables(DatabaseHelper.TABLE_ESTOQUE);
        String groupBy = null;
        String having = null;

        if (uriMatcher.match(uri) == ESTOQUE_ID) {
            sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(1));
        }
        else
        if (uriMatcher.match(uri) == ESTOQUE_PRODUTOS) {
            groupBy = PRODUTO_ID;
            having = "AVG(quantidade) > 0";
        }

        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = PRODUTO;
        }

        sqlBuilder.setDistinct(true);
        Cursor c = sqlBuilder.query(arrasaVendasDb, projection, selection, selectionArgs, groupBy, having, sortOrder);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int count = 0;
        switch (uriMatcher.match(uri)) {
            case ESTOQUE_ALL:
                count = this.arrasaVendasDb.update(DatabaseHelper.TABLE_ESTOQUE, values, selection, selectionArgs);
                break;
            case ESTOQUE_ID:
                String id = uri.getPathSegments().get(1);
                String whereClause = String.format("%s = %s", _ID, id);

                if (!TextUtils.isEmpty(selection))
                    whereClause += " AND (" + selection + ")";

                count = arrasaVendasDb.update(DatabaseHelper.TABLE_ESTOQUE, values, whereClause, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("URI Desconhecida: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;

    }

}
