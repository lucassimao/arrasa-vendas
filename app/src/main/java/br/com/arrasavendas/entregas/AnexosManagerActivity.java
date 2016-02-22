package br.com.arrasavendas.entregas;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import br.com.arrasavendas.R;
import br.com.arrasavendas.Utilities;
import br.com.arrasavendas.providers.VendasProvider;

import static br.com.arrasavendas.Utilities.ImageFolder;

public class AnexosManagerActivity extends ListActivity {


    public static final String VENDA_ID = "AnexosManagerActivity.VENDA_ID";
    private static final int CHOOSE_IMAGE_REQUEST_CODE = 1;
    private AnexosListAdapter anexosListAdapter;
    private Long vendaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        this.vendaId = (Long) intent.getExtras().get(VENDA_ID);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        this.anexosListAdapter = new AnexosListAdapter();
        setListAdapter(this.anexosListAdapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.anexos_manager_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_anexo:
                selecionarAnexo();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }


    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        String fileName = (String) this.anexosListAdapter.getItem(position);
        String fullPath = ImageFolder.ANEXOS.getPath(AnexosManagerActivity.this) + fileName;
        File file = new File(fullPath);

        String authority = "br.com.arrasavendas.fileprovider";
        Uri uriForFile = FileProvider.getUriForFile(this, authority, file);

        String extension = MimeTypeMap.getFileExtensionFromUrl(fullPath);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uriForFile, mimeType);

        startActivity(intent);
    }

    private void excluirImagem(final int position) {

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle("Confirmação");
        dialog.setMessage(getResources().getQuantityString(R.plurals.msg_excluir_imagens, 1));
        dialog.setCancelable(false);
        dialog.setIcon(android.R.drawable.ic_dialog_alert);

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Sim", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int buttonId) {

                // removendo o novo anexo do banco de dados local
                String[] projection = {VendasProvider.ANEXOS_JSON_ARRAY};
                String selection = VendasProvider._ID + "=?";
                String[] selectionArgs = {AnexosManagerActivity.this.vendaId.toString()};

                Cursor cursor = getContentResolver().query(VendasProvider.CONTENT_URI,
                        projection, selection, selectionArgs, null);

                cursor.moveToFirst();
                String anexosJsonArray = cursor.getString(0);
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
                    getContentResolver().update(VendasProvider.CONTENT_URI, cv, selection, selectionArgs);

                    anexosListAdapter.notifyDataSetChanged();

                    JSONObject venda = new JSONObject();
                    venda.put("id", vendaId);
                    venda.put("anexos", novoArray);

                    // notificando o servidor remoto da exclusão do anexo
                    new UpdateVendaAsyncTask(vendaId, venda, new UpdateVendaAsyncTask.OnComplete() {
                        @Override
                        public void run(HttpResponse response) {
                            Toast.makeText(AnexosManagerActivity.this, "Imagem excluida", Toast.LENGTH_SHORT).show();
                        }
                    }).execute();

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Não", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int buttonId) {
                dialog.dismiss();
            }
        });
        dialog.show();


    }

    private void selecionarAnexo() {
        Intent galleryintent = null;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            galleryintent = new Intent(Intent.ACTION_OPEN_DOCUMENT, null);

        } else {
            galleryintent = new Intent(Intent.ACTION_GET_CONTENT, null);
        }

        galleryintent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        galleryintent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        galleryintent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "image/*"});
        galleryintent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryintent.setType("*/*");

        Intent chooser = Intent.createChooser(galleryintent, "Selecione o anexo");
        startActivityForResult(chooser, CHOOSE_IMAGE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        /**
         * executado quando o usuário seleciona algum arquivo. faz o upload do arquivo imediatamente
         */
        if (requestCode == CHOOSE_IMAGE_REQUEST_CODE && resultCode == RESULT_OK && null != data) {

            final ProgressDialog progressDlg = ProgressDialog.show(this, "Enviando anexo", "Aguarde ...");
            final Uri uri = data.getData();
            final AnexosManagerActivity activity = this;

            new UploadAnexoAsyncTask(this.vendaId, new UploadAnexoAsyncTask.OnComplete() {
                @Override
                public void run(UploadAnexoAsyncTask.Response response) {
                    progressDlg.dismiss();

                    Toast.makeText(AnexosManagerActivity.this, response.getMessage(), Toast.LENGTH_SHORT).show();

                    // salvando copia da imagem na pasta local do app
                    Utilities.salvarImagem(activity, ImageFolder.ANEXOS, response.getFileName(), uri);

                    // adicionando o novo anexo no registro do db local
                    String[] projection = {VendasProvider.ANEXOS_JSON_ARRAY};
                    String selection = VendasProvider._ID + "=?";
                    String[] selectionArgs = {AnexosManagerActivity.this.vendaId.toString()};

                    Cursor cursor = getContentResolver().query(VendasProvider.CONTENT_URI,
                            projection, selection, selectionArgs, null);

                    cursor.moveToFirst();
                    String anexosJsonArray = cursor.getString(0);
                    try {
                        JSONArray array = new JSONArray(anexosJsonArray);
                        array.put(response.getFileName());

                        ContentValues cv = new ContentValues();
                        cv.put(VendasProvider.ANEXOS_JSON_ARRAY, array.toString());

                        getContentResolver().update(VendasProvider.CONTENT_URI, cv, selection, selectionArgs);

                        anexosListAdapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }, this).execute(uri);
        }
    }

    private class AnexosListAdapter extends BaseAdapter {

        private int count;
        private Bitmap[] thumbnails;
        private String[] filenames;

        public AnexosListAdapter() {
            super();
            recarregarDados();
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            recarregarDados();
        }

        private void recarregarDados() {
            String[] projection = {VendasProvider.ANEXOS_JSON_ARRAY};
            String selection = VendasProvider._ID + "=?";
            String[] selectionArgs = {vendaId.toString()};

            Cursor cursor = getContentResolver().query(VendasProvider.CONTENT_URI, projection, selection, selectionArgs, null);
            cursor.moveToFirst();

            try {
                String jsonArray = cursor.getString(0);
                JSONArray array = new JSONArray(jsonArray);

                this.count = array.length();

                if (this.count > 0) {

                    this.thumbnails = new Bitmap[this.count];
                    this.filenames = new String[this.count];

                    for (int i = 0; i < this.count; i++) {
                        String filename = array.getString(i);
                        filenames[i] = filename;

                        String imagePath = ImageFolder.ANEXOS.getPath(AnexosManagerActivity.this) + filename;

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(imagePath, options);

                        // se for algum arquivo de image, cria um thumbnail
                        if (options.outWidth != -1 && options.outHeight != -1) {

                            options.inSampleSize = Utilities.calculateInSampleSize(options, 50, 50);
                            options.inJustDecodeBounds = false;
                            thumbnails[i] = BitmapFactory.decodeFile(imagePath, options);

                        }else{
                            // o arquivo eh do tipo pdf, n precisa de thumbnail
                            thumbnails[i] = null;
                        }
                    }
                }

                cursor.close();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getCount() {
            return this.count;
        }

        public Object getItem(int position) {
            return filenames[position];
        }

        @Override
        public long getItemId(int position) {
            return filenames[position].hashCode();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(R.layout.list_item_anexos_manager, parent, false);

            ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
            // se for uma image
            if (thumbnails[position] != null)
                imageView.setImageBitmap(thumbnails[position]);
            else
                imageView.setImageResource(R.drawable.pdf_icon);

            TextView textView = (TextView) convertView.findViewById(R.id.textView);
            textView.setText(filenames[position]);

            ImageButton imageButton = (ImageButton) convertView.findViewById(R.id.imageButton);
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    excluirImagem(position);
                }
            });
            return convertView;
        }
    }
}
