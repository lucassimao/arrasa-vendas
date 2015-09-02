package br.com.arrasavendas.providers;

import android.content.*;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import br.com.arrasavendas.DatabaseHelper;

/**
 * Created by lsimaocosta on 19/07/15.
 */
public class DownloadedImagesProvider extends ContentProvider {

    private static final String PROVIDER_NAME = DownloadedImagesProvider.class.getName();
    public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/downloadedImages");

    public final static String _ID = "_id";
    public final static String PRODUTO_ID = "produto_id";
    public final static String PRODUTO_NOME = "produto";
    public static final String PRODUTO_ASCII = "produto_ascii" ;
    public final static String UNIDADE = "unidade";
    public final static String IMAGE_NAME = "image_name";
    public final static String LOCAL_PATH = "local_path";
    public final static String IS_IGNORED = "is_ignored";


    private static final UriMatcher uriMatcher;
    private static final int DOWNLOADED_IMAGES_ALL = 1; // retorna tudo
    private static final int DOWNLOADED_IMAGES_ID = 2; // consulta a tabela pelo ID

    private SQLiteDatabase arrasaVendasDb;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "downloadedImages", DOWNLOADED_IMAGES_ALL);
        uriMatcher.addURI(PROVIDER_NAME, "downloadedImages/#", DOWNLOADED_IMAGES_ID);
    }

    @Override
    public boolean onCreate() {
        Context ctx = getContext();
        DatabaseHelper dbHelper = new DatabaseHelper(ctx);
        this.arrasaVendasDb = dbHelper.getWritableDatabase();

        return (this.arrasaVendasDb != null);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
        sqlBuilder.setTables(DatabaseHelper.TABLE_DOWNLOADED_IMAGES);
        String groupBy = null, having = null;

        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = PRODUTO_ID;
        }

        sqlBuilder.appendWhere(IS_IGNORED + "=0");

        switch (uriMatcher.match(uri)) {
            case DOWNLOADED_IMAGES_ALL:
                break;
            case DOWNLOADED_IMAGES_ID:
                sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(1));
                break;
        }

        Cursor c = sqlBuilder.query(arrasaVendasDb, projection, selection, selectionArgs, groupBy, having, sortOrder);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case DOWNLOADED_IMAGES_ALL:
                return "vnd.android.cursor.dir/br.com.arrasavendas.image";
            case DOWNLOADED_IMAGES_ID:
                return "vnd.android.cursor.item/br.com.arrasavendas.image";
            default:
                throw new IllegalArgumentException("URI Desconhecida: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        long rowId = arrasaVendasDb.insertWithOnConflict(DatabaseHelper.TABLE_DOWNLOADED_IMAGES, null, values, SQLiteDatabase.CONFLICT_IGNORE);

        if (rowId > 0) {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)) {
            case DOWNLOADED_IMAGES_ID:
                String id = uri.getPathSegments().get(1);
                String whereClause = _ID + "=" + id;
                if (!TextUtils.isEmpty(selection))
                    whereClause += " AND (" + selection + ")";
                count = arrasaVendasDb.delete(DatabaseHelper.TABLE_DOWNLOADED_IMAGES, whereClause, selectionArgs);
                break;
            case DOWNLOADED_IMAGES_ALL:
                count = arrasaVendasDb.delete(DatabaseHelper.TABLE_DOWNLOADED_IMAGES, selection, selectionArgs);
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
            case DOWNLOADED_IMAGES_ALL:
                count = this.arrasaVendasDb.update(DatabaseHelper.TABLE_DOWNLOADED_IMAGES, values, selection, selectionArgs);
                break;
            case DOWNLOADED_IMAGES_ID:
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
