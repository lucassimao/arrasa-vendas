package br.com.arrasavendas.imagesManager;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.*;
import android.os.AsyncTask;

import br.com.arrasavendas.Utilities;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.providers.DownloadedImagesProvider;

public class DownloadImagesTask extends AsyncTask<Void, Integer, Void> {

    private final Set<Long> setOfProdutoId;
    private final int qtdeImagens;
    private Context ctx;
    private ProgressDialog progressDialog;

    public DownloadImagesTask(Context ctx, Set<Long> setOfProdutoId) {
        super();
        this.ctx = ctx;
        this.setOfProdutoId = setOfProdutoId;
        this.qtdeImagens = getQuantidadeDeImagensParaBaixar(this.setOfProdutoId);

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (qtdeImagens > 0) {
            progressDialog = new ProgressDialog(this.ctx);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false);
            progressDialog.setProgress(0);
            progressDialog.setMax(qtdeImagens);
            progressDialog.setMessage("Baixando imagens ...");
            progressDialog.show();
        }
    }

    @Override
    protected Void doInBackground(Void... params) {

        if (qtdeImagens > 0) {

            String[] projection = {DownloadedImagesProvider._ID, DownloadedImagesProvider.IMAGE_NAME,
                    DownloadedImagesProvider.UNIDADE, DownloadedImagesProvider.PRODUTO_NOME};
            String selection = DownloadedImagesProvider.LOCAL_PATH + " is NULL AND " + DownloadedImagesProvider.PRODUTO_ID + "=?";


            for (long produtoId : this.setOfProdutoId) {

                String[] selectionArgs = {String.valueOf(produtoId)};

                Cursor cursor = this.ctx.getContentResolver().query(DownloadedImagesProvider.CONTENT_URI, projection, selection, selectionArgs, DownloadedImagesProvider._ID);
                int count = 0;

                while (cursor.moveToNext()) {
                    long downloadedImageId = cursor.getLong(cursor.getColumnIndex(DownloadedImagesProvider._ID));
                    String imageName = cursor.getString(cursor.getColumnIndex(DownloadedImagesProvider.IMAGE_NAME));
                    String produtoNome = cursor.getString(cursor.getColumnIndex(DownloadedImagesProvider.PRODUTO_NOME));
                    String unidade = cursor.getString(cursor.getColumnIndex(DownloadedImagesProvider.UNIDADE));

                    try {
                        byte[] imageBytes = downloadImage(imageName);
                        String localPath = salvarImagem(imageName.toLowerCase(), imageBytes); // evitando bug dos caractreres especiais

                        if (getQuantidadeDeUnidadesDoProduto(produtoId) > 1)
                            desenharLegenda(localPath, produtoNome, unidade);

                        updateLocalPath(downloadedImageId, localPath);

                        publishProgress(++count);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        progressDialog.incrementProgressBy(1);

    }

    @Override
    protected void onPostExecute(Void result) {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    private int getQuantidadeDeImagensParaBaixar(Set<Long> setOfProdutoId) {
        String[] projection = {DownloadedImagesProvider._ID};
        String selection = DownloadedImagesProvider.LOCAL_PATH + " is NULL AND " +
                DownloadedImagesProvider.PRODUTO_ID + " IN(" + Utilities.makePlaceholders(setOfProdutoId.size()) + ")";

        String[] selectionArgs = new String[setOfProdutoId.size()];
        int i = 0;
        for (long produtoId : setOfProdutoId)
            selectionArgs[i++] = String.valueOf(produtoId);

        Cursor c = this.ctx.getContentResolver().query(DownloadedImagesProvider.CONTENT_URI, projection, selection, selectionArgs, null);
        int count = c.getCount();
        c.close();
        return count;
    }

    private int getQuantidadeDeUnidadesDoProduto(long idProduto) {
        String[] projection = {"distinct " + DownloadedImagesProvider.UNIDADE};
        String selection = DownloadedImagesProvider.PRODUTO_ID + " =?";
        String[] selectionArgs = {String.valueOf(idProduto)};
        Cursor c = this.ctx.getContentResolver().query(DownloadedImagesProvider.CONTENT_URI, projection, selection, selectionArgs, null);
        int count = c.getCount();
        c.close();
        return count;
    }

    private void desenharLegenda(String localPath, String produto, String unidade) {

        Typeface tf = Typeface.createFromAsset(this.ctx.getAssets(), "fonts/Pacifico.ttf");
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inMutable = true;
        float scale = this.ctx.getResources().getDisplayMetrics().density;

        Bitmap b = BitmapFactory.decodeFile(localPath, op);
        Canvas canvas = new Canvas(b);
        Paint paint = new Paint();
        paint.setTypeface(tf);
        paint.setColor(Color.parseColor("#FF00FF")); // Text Color
        paint.setTextSize((int) (20 * scale));
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)); // Text Overlapping Pattern
        canvas.drawBitmap(b, 0, 0, paint);
        canvas.drawText(unidade, 10, b.getHeight() - (int) (10 * scale), paint);

        FileOutputStream fos = null;
        File tempFile = null;
        try {
            tempFile = new File(localPath);
            fos = new FileOutputStream(tempFile);
            b.compress(Bitmap.CompressFormat.JPEG, 100, fos);

        } catch (IOException e) {
            e.printStackTrace();
        }

        b.recycle();

    }

    private byte[] downloadImage(String imageName) throws IOException {

        HttpClient client = new DefaultHttpClient();

        HttpGet httpGet = new HttpGet(RemotePath.getProdutosImageURL(imageName));
        HttpResponse response = client.execute(httpGet);
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();

        if (statusCode == HttpStatus.SC_OK) {
            HttpEntity entity = response.getEntity();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = entity.getContent();
            byte[] buffer = new byte[4 * 1024];
            int read = 0;

            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
        return null;

    }

    private String salvarImagem(String imageName, byte[] imageBytes) {
        File file = new File(ctx.getFilesDir() + "/produtos/", imageName);
        try {
            FileOutputStream output = new FileOutputStream(file);
            output.write(imageBytes);
            output.close();
            return file.getAbsolutePath();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void updateLocalPath(Long id, String localPath) {
        String where = DownloadedImagesProvider._ID + "=?";
        String[] whereArgs = {id.toString()};

        ContentValues cv = new ContentValues();
        cv.put(DownloadedImagesProvider.LOCAL_PATH, localPath);

        this.ctx.getContentResolver().update(DownloadedImagesProvider.CONTENT_URI, cv, where, whereArgs);
    }

}
