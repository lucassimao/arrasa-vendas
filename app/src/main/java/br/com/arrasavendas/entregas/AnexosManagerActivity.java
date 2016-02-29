package br.com.arrasavendas.entregas;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

import br.com.arrasavendas.R;
import br.com.arrasavendas.Utilities;
import br.com.arrasavendas.util.Response;

import static br.com.arrasavendas.Utilities.ImageFolder;

public class AnexosManagerActivity extends ListActivity implements AnexosListAdapter.OnClickBtnExcluirAnexo {


    public static final String VENDA_ID = "AnexosManagerActivity.VENDA_ID";
    private static final int CHOOSE_IMAGE_REQUEST_CODE = 1;
    private AnexosListAdapter anexosListAdapter;
    private Long vendaId;
    private AnexosDAO dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        this.vendaId = (Long) intent.getExtras().get(VENDA_ID);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        this.dao = new AnexosDAO(this, vendaId);
        this.anexosListAdapter = new AnexosListAdapter(dao.list(), this, this);
        setListAdapter(this.anexosListAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dao.close();
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
        final File file = new File(fullPath);
        final AnexosManagerActivity ctx = this;

        if (!file.exists()) {
            final Dialog progressDlg = ProgressDialog.show(this, "Baixando anexo", "Aguarde ...");

            new DownloadAnexoAsyncTask(new DownloadAnexoAsyncTask.OnComplete() {
                @Override
                public void run(DownloadAnexoAsyncTask.HttpResponse response) {
                    progressDlg.dismiss();

                    if (response.getStatus() == HttpURLConnection.HTTP_OK) {
                        try {
                            FileOutputStream output = new FileOutputStream(file);
                            output.write(response.getBytes());
                            output.close();
                            exibirAnexo(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else
                        Toast.makeText(ctx, response.getMessage(), Toast.LENGTH_SHORT).show();

                }
            }).execute(fileName);

        } else
            exibirAnexo(file);


    }

    private void exibirAnexo(File file) {
        String authority = "br.com.arrasavendas.fileprovider";
        Uri uriForFile = FileProvider.getUriForFile(this, authority, file);

        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uriForFile, mimeType);

        startActivity(intent);
    }

    public void excluirAnexo(final int position) {

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle("Confirmação");
        dialog.setMessage(getResources().getQuantityString(R.plurals.msg_excluir_imagens, 1));
        dialog.setCancelable(false);
        dialog.setIcon(android.R.drawable.ic_dialog_alert);

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Sim", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int buttonId) {

                try {
                    final String[] filenames = dao.list();
                    JSONArray novoArray = new JSONArray();

                    // criando o novo aray de anexos: feito assim pq o construtor da classe JSONArray
                    // que recebe um array so esta disponivel a partir da versao KITKAT
                    // e nao existe um metodo remove que recebe a posição do elemento a ser removid
                    for (int i = 0; i < filenames.length; ++i) {
                        if (i == position) continue;
                        novoArray.put(filenames[i]);
                    }

                    // notificando o servidor remoto da exclusão do anexo
                    JSONObject venda = new JSONObject();
                    venda.put("id", vendaId);
                    venda.put("anexos", novoArray);

                    new UpdateVendaAsyncTask(vendaId, venda, new UpdateVendaAsyncTask.OnComplete() {
                        @Override
                        public void run(Response response) {
                            if (response.getStatus() == HttpURLConnection.HTTP_OK) {
                                dao.delete(position);
                                anexosListAdapter.setAnexos(dao.list());
                                Toast.makeText(AnexosManagerActivity.this, "Imagem excluida", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(AnexosManagerActivity.this, "Erro " +
                                        response.getStatus() + " " + response.getMessage(), Toast.LENGTH_LONG).show();

                            }
                        }
                    }).execute();

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(AnexosManagerActivity.this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();

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
                public void run(UploadAnexoAsyncTask.ResponseUpload response) {
                    progressDlg.dismiss();

                    String msg = null;

                    if (response.getStatus() == HttpURLConnection.HTTP_OK) {

                        msg = response.getMessage();

                        try {

                            // salvando copia da imagem na pasta local do app
                            String fileName = response.getFileName();
                            Utilities.salvarImagem(activity, ImageFolder.ANEXOS, fileName, uri);
                            dao.addAnexo(fileName);
                            anexosListAdapter.setAnexos(dao.list());

                        } catch (IOException e) {
                            e.printStackTrace();
                            msg = e.getMessage();
                        }
                    } else {
                        msg = "Erro: " + response.getMessage();
                    }

                    Toast.makeText(AnexosManagerActivity.this, msg, Toast.LENGTH_SHORT).show();


                }
            }, this).execute(uri);
        }
    }
}
