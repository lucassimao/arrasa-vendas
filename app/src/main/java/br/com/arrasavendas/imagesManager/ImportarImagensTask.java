package br.com.arrasavendas.imagesManager;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.OpenableColumns;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import br.com.arrasavendas.R;
import br.com.arrasavendas.Utilities;
import br.com.arrasavendas.providers.DownloadedImagesProvider;

public class ImportarImagensTask extends AsyncTask<Intent, Void, Integer> {

    private final Long produtoId;
    private final String unidade;
    private Context ctx;

    public ImportarImagensTask(Context ctx, Long produtoId, String unidade) {
        super();
        this.ctx = ctx;
        this.produtoId = produtoId;
        this.unidade = unidade;
    }

    @Override
    protected Integer doInBackground(Intent... params) {

        Set<Uri> uris = new HashSet<Uri>();
        Intent intent = params[0];
        ContentResolver contentResolver = this.ctx.getContentResolver();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            final int takeFlags = intent.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.getData() != null) {
                uris.add(intent.getData());
            } else {
                ClipData clipdata = intent.getClipData();

                for (int i = 0; i < clipdata.getItemCount(); i++) {
                    Uri uri = clipdata.getItemAt(i).getUri();
                    this.ctx.grantUriPermission(this.ctx.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    contentResolver.takePersistableUriPermission(uri, takeFlags);
                    uris.add(uri);
                }
            }
        } else {
            uris.add(intent.getData());
        }

        for (Uri uri : uris) {

            String imageFileName = getImageFileName(uri);
            String localPath = null;

            try {
                localPath = Utilities.salvarImagem(this.ctx, Utilities.ImageFolder.PRODUTOS, imageFileName, uri);
                saveNewImage(imageFileName, localPath);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return uris.size();
    }

    @Override
    protected void onPostExecute(Integer result) {
        String msg = this.ctx.getResources().getQuantityString(R.plurals.msg_confirmacao_importacao_imagem, result, result);
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }


    private String getImageFileName(Uri uri) {

        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.
        String[] projection = {OpenableColumns.DISPLAY_NAME};
        String displayName = null;
        Cursor cursor = this.ctx.getContentResolver().query(uri, projection, null, null, null, null);

        try {
            // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (cursor != null && cursor.moveToFirst()) {

                // Note it's called "Display Name".  This is
                // provider-specific, and might not necessarily be the file name.
                displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

            }
        } finally {
            cursor.close();
        }
        return displayName;
    }


    private Uri saveNewImage(String imageName, String localPath) {
        ContentValues cv = new ContentValues();
        cv.put(DownloadedImagesProvider.IMAGE_NAME, imageName);
        cv.put(DownloadedImagesProvider.PRODUTO_ID, produtoId);
        cv.put(DownloadedImagesProvider.LOCAL_PATH, localPath);
        cv.put(DownloadedImagesProvider.UNIDADE, unidade);
        cv.put(DownloadedImagesProvider.IS_IGNORED, 0);

        return this.ctx.getContentResolver().insert(DownloadedImagesProvider.CONTENT_URI, cv);
    }

}
