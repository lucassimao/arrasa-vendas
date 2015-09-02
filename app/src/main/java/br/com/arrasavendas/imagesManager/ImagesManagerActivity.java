package br.com.arrasavendas.imagesManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import br.com.arrasavendas.R;
import br.com.arrasavendas.providers.DownloadedImagesProvider;
import br.com.arrasavendas.providers.EstoqueProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ImagesManagerActivity extends Activity {


    private static final int CHOOSE_IMAGE_REQUEST_CODE = 1;

    private AutoCompleteTextView autoCompleteTextViewProduto;
    private Spinner spinnerUnidade;

    int count;
    private int[] _ids;
    Bitmap[] thumbnails;
    String[] localPath;
    boolean[] thumbnailsSelection;
    private ImageAdapter imageAdapter;

    private ImageButton imageBtnAdd, imageBtnDelete, imageBtnDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images_manager);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        autoCompleteTextViewProduto = (AutoCompleteTextView) findViewById(R.id.auto_complete_produto);
        spinnerUnidade = (Spinner) findViewById(R.id.spinner_unidade);
        imageBtnAdd = (ImageButton) findViewById(R.id.imageBtnAdd);
        imageBtnDelete = (ImageButton) findViewById(R.id.imageBtnDelete);
        imageBtnDownload = (ImageButton) findViewById(R.id.imageBtnDownload);

        toogleImageButtons(false);

        configurarAutoCompleteTextViewProduto();
        configurarSpinnerUnidade();
    }

    public void downloadImages(View view) {
        Long produtoId = (Long) autoCompleteTextViewProduto.getTag();

        new DownloadImagesTask(this, Collections.singleton(produtoId)){
            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                exibirImagens();
            }
        }.execute();
    }

    public void deleteImages(View view) {
        final int len = thumbnailsSelection.length;
        int cnt = 0;
        final Set<Integer> selected_ids = new HashSet<Integer>();

        for (int i = 0; i < len; i++) {
            if (thumbnailsSelection[i]) {
                cnt++;
                selected_ids.add(_ids[i]);
            }
        }
        if (cnt == 0) {
            Toast.makeText(this, "Selecione as imagens que devem ser excluidas",
                    Toast.LENGTH_LONG).show();
        } else {

            AlertDialog dialog = new AlertDialog.Builder(this).create();
            dialog.setTitle("Confirmação");
            dialog.setMessage(getResources().getQuantityString(R.plurals.msg_excluir_imagens, cnt, cnt));
            dialog.setCancelable(false);
            final int finalCnt = cnt;
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Sim", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int buttonId) {
                    String where = DownloadedImagesProvider._ID + " in ";
                    where += selected_ids.toString().replace("[", "(").replace("]", ")");
                    ContentValues cv = new ContentValues();
                    cv.put(DownloadedImagesProvider.IS_IGNORED, 1);

                    getContentResolver().update(DownloadedImagesProvider.CONTENT_URI, cv, where, null);
                    String quantityString = getResources().getQuantityString(R.plurals.msg_confirmacao_exclusao, finalCnt, finalCnt);
                    Toast.makeText(ImagesManagerActivity.this, quantityString, Toast.LENGTH_SHORT).show();

                    exibirImagens();
                }
            });
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Não", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int buttonId) {
                    dialog.dismiss();
                }
            });
            dialog.setIcon(android.R.drawable.ic_dialog_alert);
            dialog.show();

        }
    }

    private void configurarSpinnerUnidade() {

        final SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null,
                new String[]{EstoqueProvider.UNIDADE, EstoqueProvider._ID}, new int[]{android.R.id.text1}, 0);

        adapter.setStringConversionColumn(0);
        spinnerUnidade.setAdapter(adapter);

        spinnerUnidade.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinnerUnidade.setTag(id);
                exibirImagens();
                toogleImageButtons(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinnerUnidade.setTag(null);
                toogleImageButtons(false);

                GridView imagegrid = (GridView) findViewById(R.id.PhoneImageGrid);
                imageAdapter = new ImageAdapter(ImagesManagerActivity.this);

                count = 0;
                thumbnails = null;
                localPath = null;
                thumbnailsSelection = null;

                imagegrid.setAdapter(imageAdapter);
            }
        });
    }

    private void configurarAutoCompleteTextViewProduto() {

        final String[] colunas = new String[]{EstoqueProvider.PRODUTO, EstoqueProvider._ID};

        CursorLoader loader = new CursorLoader(this, EstoqueProvider.CONTENT_URI_PRODUTOS, colunas, null, null, null);
        Cursor cursor = loader.loadInBackground();

        SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_dropdown_item_1line, cursor, colunas,
                new int[]{android.R.id.text1}, 0);

        simpleCursorAdapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {

            @Override
            public CharSequence convertToString(Cursor cursor) {
                int columnIndex = cursor.getColumnIndex(EstoqueProvider.PRODUTO);
                return cursor.getString(columnIndex);
            }
        });

        simpleCursorAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence arg0) {
                if (TextUtils.isEmpty(arg0))
                    return null;

                CursorLoader cl = new CursorLoader(getApplicationContext(), EstoqueProvider.CONTENT_URI_PRODUTOS, colunas,
                        "produto_nome_ascii like ?1 or produto_nome like ?1 ", new String[]{"%" + arg0.toString() + "%"}, null);
                return cl.loadInBackground();
            }
        });

        autoCompleteTextViewProduto.setAdapter(simpleCursorAdapter);
        autoCompleteTextViewProduto.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long id) {
                long produtoId = getProdutoIdFromEstoqueId(id);

                autoCompleteTextViewProduto.setTag(produtoId);
                carregarUnidades(produtoId);
            }

        });

    }

    private void carregarUnidades(Long produtoId) {
        final String[] projection = new String[]{EstoqueProvider.UNIDADE, EstoqueProvider._ID};
        String selection = EstoqueProvider.PRODUTO_ID + " =?";
        String[] selectionArgs = {produtoId.toString()};

        CursorLoader loader = new CursorLoader(this, EstoqueProvider.CONTENT_URI, projection, selection,
                selectionArgs, EstoqueProvider.UNIDADE);
        Cursor cursor = loader.loadInBackground();

        SimpleCursorAdapter sca = (SimpleCursorAdapter) spinnerUnidade.getAdapter();
        sca.swapCursor(cursor);

    }

    public void adicionarImagem(View view) {
        Intent galleryintent = null;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            galleryintent = new Intent(Intent.ACTION_OPEN_DOCUMENT, null);

        } else {
            galleryintent = new Intent(Intent.ACTION_GET_CONTENT, null);
        }

        galleryintent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galleryintent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        galleryintent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryintent.setType("image/*");

        Intent chooser = Intent.createChooser(galleryintent, "Selecione a imagem");
        startActivityForResult(chooser, CHOOSE_IMAGE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CHOOSE_IMAGE_REQUEST_CODE && resultCode == RESULT_OK && null != data) {

            long estoqueId = (long) spinnerUnidade.getTag();
            final long produtoId = getProdutoIdFromEstoqueId(estoqueId);
            Cursor cursor = (Cursor) spinnerUnidade.getSelectedItem();
            final String unidade = cursor.getString(cursor.getColumnIndex(EstoqueProvider.UNIDADE));

            new ImportarImagensTask(this, produtoId, unidade) {
                @Override
                protected void onPostExecute(Integer result) {
                    super.onPostExecute(result);

                    exibirImagens();
                    toogleImageButtons(true);
                }
            }.execute(data);
        }
    }



    public void exit(View v) {
        finish();
    }


    private void exibirImagens() {
        long estoqueId = (long) spinnerUnidade.getTag();
        Long produtoId = getProdutoIdFromEstoqueId(estoqueId);
        Cursor cursor = (Cursor) spinnerUnidade.getSelectedItem();
        String unidade = cursor.getString(cursor.getColumnIndex(EstoqueProvider.UNIDADE));

        String[] projection = {DownloadedImagesProvider.LOCAL_PATH, DownloadedImagesProvider._ID};
        String selection = DownloadedImagesProvider.PRODUTO_ID +
                "=? AND " + DownloadedImagesProvider.UNIDADE + "=? AND " + DownloadedImagesProvider.LOCAL_PATH + " IS NOT NULL";
        String[] selectionArgs = {produtoId.toString(), unidade};

        cursor = getContentResolver().query(DownloadedImagesProvider.CONTENT_URI, projection, selection, selectionArgs, null);
        cursor.moveToFirst();

        this.count = cursor.getCount();
        this.thumbnails = new Bitmap[this.count];
        this.localPath = new String[this.count];
        this._ids = new int[this.count];
        this.thumbnailsSelection = new boolean[this.count];


        for (int i = 0; i < this.count; i++) {
            cursor.moveToPosition(i);
            int columnIndex = cursor.getColumnIndex(DownloadedImagesProvider.LOCAL_PATH);
            String imagePath = cursor.getString(columnIndex);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(imagePath, options);

            options.inSampleSize = calculateInSampleSize(options, 150, 150);
            options.inJustDecodeBounds = false;

            thumbnails[i] = BitmapFactory.decodeFile(imagePath, options);
            this._ids[i] = cursor.getInt(cursor.getColumnIndex(DownloadedImagesProvider._ID));
            localPath[i] = imagePath;
        }

        GridView imagegrid = (GridView) findViewById(R.id.PhoneImageGrid);
        imageAdapter = new ImageAdapter(this);
        imagegrid.setAdapter(imageAdapter);
        cursor.close();
    }

    private long getProdutoIdFromEstoqueId(long estoqueId) {
        String[] projection = {EstoqueProvider.PRODUTO_ID};
        Uri uri = Uri.withAppendedPath(EstoqueProvider.CONTENT_URI, String.valueOf(estoqueId));

        CursorLoader cl = new CursorLoader(this, uri, projection, null, null, null);
        Cursor c = cl.loadInBackground();
        c.moveToFirst();

        return c.getLong(c.getColumnIndex(EstoqueProvider.PRODUTO_ID));
    }

    private void toogleImageButtons(boolean enabled) {
        setImageButtonEnabled(enabled, imageBtnAdd, R.drawable.ic_add_black_36dp);
        setImageButtonEnabled(enabled, imageBtnDelete, R.drawable.ic_delete_black_36dp);
        setImageButtonEnabled(enabled, imageBtnDownload, R.drawable.ic_file_download_black_36dp);
    }

    private void setImageButtonEnabled(boolean enabled, ImageButton imageButton, int iconResId) {
        imageButton.setEnabled(enabled);
        Drawable originalIcon = getResources().getDrawable(iconResId);
        Drawable icon = enabled ? originalIcon : convertDrawableToGrayScale(originalIcon);
        imageButton.setImageDrawable(icon);
    }

    private Drawable convertDrawableToGrayScale(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        Drawable res = drawable.mutate();
        res.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
        return res;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}
