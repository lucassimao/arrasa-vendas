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


public class EstoqueProvider extends ContentProvider {

    private static final String PROVIDER_NAME = EstoqueProvider.class.getName();

    public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/estoque");
    // representa os produtos que tiver no minimo 1 item em estoque
    public static final Uri CONTENT_URI_PRODUTOS = Uri.parse(CONTENT_URI.toString() + "/produtos");

    public static final Uri CONTENT_URI_UNIDADES = Uri.parse(CONTENT_URI.toString() + "/unidades");

    public final static String _ID = "_id";
    public final static String PRODUTO = "produto_nome";
    public final static String PRODUTO_ID = "produto_id";
    public final static String UNIDADE = "unidade";
    public final static String QUANTIDADE = "quantidade";
    public final static String PRECO_A_VISTA = "prevoAVista";
    public final static String PRECO_A_PRAZO = "prevoAPrazo";


    private static final UriMatcher uriMatcher;
    private static final int ESTOQUE_ALL = 1;
    private static final int ESTOQUE_ID = 2;
    private static final int ESTOQUE_PRODUTOS = 3;


    private SQLiteDatabase arrasaVendasDb;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "estoque", ESTOQUE_ALL);
        uriMatcher.addURI(PROVIDER_NAME, "estoque/#", ESTOQUE_ID);
        uriMatcher.addURI(PROVIDER_NAME, "estoque/produtos", ESTOQUE_PRODUTOS);
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
        if (uriMatcher.match(uri) == ESTOQUE_PRODUTOS) {
            groupBy = PRODUTO;
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
