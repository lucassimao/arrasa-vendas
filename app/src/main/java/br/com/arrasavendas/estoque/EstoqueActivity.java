package br.com.arrasavendas.estoque;

import java.io.File;
import java.util.*;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ListView;

import br.com.arrasavendas.DownloadJSONFeedTask;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.Utilities;
import br.com.arrasavendas.imagesManager.DownloadImagesTask;
import br.com.arrasavendas.model.Produto;
import br.com.arrasavendas.providers.DownloadedImagesProvider;
import br.com.arrasavendas.providers.EstoqueProvider;
import br.com.arrasavendas.R;

public class EstoqueActivity extends Activity {

    private EstoqueExpandableListAdapter estoqueListAdapter;
    private ExpandableListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.estoque_expandable);

        list = (ExpandableListView) findViewById(R.id.listItemsEstoque);
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        getActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onStart() {
        super.onStart();
        List<Produto> produtos = getData();
        estoqueListAdapter = new EstoqueExpandableListAdapter(this, produtos);
        list.setAdapter(estoqueListAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.estoque_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share_with_whatsapp:
                shareWithWhatsApp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void shareWithWhatsApp() {
        final Map<Long, String[]> itensSelecionados = estoqueListAdapter.getUnidadesSelecionadas();
        final ArrayList<Uri> imageUris = new ArrayList<Uri>();

        new DownloadImagesTask(this, itensSelecionados.keySet()) {
            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);

                String[] projection = {DownloadedImagesProvider.LOCAL_PATH};

                for (long idProduto : itensSelecionados.keySet()) {
                    int qtdeUnidades = itensSelecionados.get(idProduto).length;

                    String selection = DownloadedImagesProvider.PRODUTO_ID + "=? AND " + DownloadedImagesProvider.UNIDADE +
                            " IN(" + Utilities.makePlaceholders(qtdeUnidades) + ")";

                    String[] selectionArgs = new String[qtdeUnidades + 1];
                    selectionArgs[0] = String.valueOf(idProduto);
                    for (int j = 1; j < selectionArgs.length; ++j)
                        selectionArgs[j] = itensSelecionados.get(idProduto)[j - 1];

                    Cursor c = getContentResolver().query(DownloadedImagesProvider.CONTENT_URI,
                            projection, selection, selectionArgs, DownloadedImagesProvider.UNIDADE);
                    c.moveToFirst();

                    do {
                        String localpath = c.getString(c.getColumnIndex(DownloadedImagesProvider.LOCAL_PATH));
                        String authority = "br.com.arrasavendas.fileprovider";
                        File file = new File(localpath);
                        Uri uri = FileProvider.getUriForFile(EstoqueActivity.this, authority, file);
                        imageUris.add(uri);

                    } while (c.moveToNext());

                    c.close();

                }


                int grupoSize = 10;
                int start = 0, end = 0;

                do {
                    end += grupoSize;

                    if (end > imageUris.size())
                        end = imageUris.size();


                    List<Uri> uriList = imageUris.subList(start, end);

                    Intent sendIntent = new Intent();
                    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    sendIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                    //sendIntent.putStringArrayListExtra(Intent.EXTRA_TEXT, textUris);
                    sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<Uri>(uriList));
                    sendIntent.setType("image/*");
                    sendIntent.setPackage("com.whatsapp");
                    startActivity(sendIntent);

                    start = end;
                } while (start < imageUris.size());


            }
        }.execute();


    }

    private List<Produto> getData() {

        // consulta apenas os produtos com quantidade >0
        CursorLoader loader = new CursorLoader(this, EstoqueProvider.CONTENT_URI_PRODUTOS, new String[]{EstoqueProvider.PRODUTO_ID, EstoqueProvider.PRODUTO}, null, null, null);

        Cursor cursor = loader.loadInBackground();

        List<Produto> estoque = new LinkedList<Produto>();

        if (cursor.moveToFirst()) {

            do {

                String nomeProduto = cursor.getString(cursor.getColumnIndex(EstoqueProvider.PRODUTO));
                Long produtoId = cursor.getLong(cursor.getColumnIndex(EstoqueProvider.PRODUTO_ID));

                Produto itemEstoque = new Produto(produtoId, nomeProduto);

                // consultando as unidades do produto
                CursorLoader unidadesLoader = new CursorLoader(getApplicationContext(),
                        EstoqueProvider.CONTENT_URI, new String[]{EstoqueProvider.UNIDADE, EstoqueProvider.QUANTIDADE, EstoqueProvider._ID},
                        EstoqueProvider.PRODUTO_ID + " = ?", new String[]{produtoId.toString()}, EstoqueProvider.UNIDADE);

                Cursor cursor2 = unidadesLoader.loadInBackground();

                while (cursor2.moveToNext()) {
                    String unidade = cursor2.getString(cursor2.getColumnIndex(EstoqueProvider.UNIDADE));
                    int qtde = cursor2.getInt(cursor2.getColumnIndex(EstoqueProvider.QUANTIDADE));
                    long estoqueId = cursor2.getLong(cursor2.getColumnIndex(EstoqueProvider._ID));

                    itemEstoque.addUnidade(estoqueId, unidade, qtde);

                }
                cursor2.close();
                estoque.add(itemEstoque);

            } while (cursor.moveToNext());
        }
        cursor.close();

        return estoque;
    }

    public void onClickBtnSincronizar(View v) {

        final ProgressDialog progressDlg = ProgressDialog.show(this,
                "Atualizando informações", "Aguarde ...");
        new DownloadJSONFeedTask(RemotePath.EstoquePath, this, new Runnable() {

            @Override
            public void run() {
                progressDlg.dismiss();

                List<Produto> produtos = getData();
                estoqueListAdapter = new EstoqueExpandableListAdapter(EstoqueActivity.this, produtos);
                list.setAdapter(estoqueListAdapter);

            }
        }).execute();
    }

    public void onClickSair(View v) {
        finish();
    }

}
