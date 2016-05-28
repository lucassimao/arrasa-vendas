package br.com.arrasavendas.estoque;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.*;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import br.com.arrasavendas.DownloadJSONAsyncTask;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.Utilities;
import br.com.arrasavendas.entregas.TipoFiltro;
import br.com.arrasavendas.imagesManager.DownloadImagesTask;
import br.com.arrasavendas.model.Produto;
import br.com.arrasavendas.providers.DownloadedImagesProvider;
import br.com.arrasavendas.providers.EstoqueProvider;
import br.com.arrasavendas.R;
import br.com.arrasavendas.util.Response;

import static br.com.arrasavendas.Application.ENTREGAS_LOADER;
import static br.com.arrasavendas.Application.ESTOQUE_LOADER;

public class EstoqueActivity extends Activity {

    private static final String TAG = EstoqueActivity.class.getSimpleName();
    private EstoqueExpandableListAdapter estoqueListAdapter;
    private ExpandableListView list;
    private EstoqueCursorCallback estoqueCursorCallback = new EstoqueCursorCallback();
    private Menu menu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estoque);

        list = (ExpandableListView) findViewById(R.id.listItemsEstoque);
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        estoqueListAdapter = new EstoqueExpandableListAdapter(this);
        list.setAdapter(estoqueListAdapter);

        getLoaderManager().initLoader(ESTOQUE_LOADER, null, estoqueCursorCallback);

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Bundle bundle = new Bundle();
            bundle.putString("query",query);
            getLoaderManager().restartLoader(ESTOQUE_LOADER, bundle, estoqueCursorCallback);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.estoque_activity_menu, menu);

        SearchManager searchManager =  (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        MenuItem mSearchMenu = menu.findItem(R.id.search);

        mSearchMenu.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true; // Return true to expand action view
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                getLoaderManager().restartLoader(ESTOQUE_LOADER, null, estoqueCursorCallback);
                return true; // Return true to collapse action view
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share_with_whatsapp:
                shareWithWhatsApp();
                return true;
            case R.id.sync_estoque:
                // ocultando o campo de busca
                menu.findItem(R.id.search).collapseActionView();
                sincronizarEstoque();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sincronizarEstoque() {

        final ProgressDialog progressDlg = ProgressDialog.show(this,
                "Atualizando informações", "Aguarde ...");
        new DownloadJSONAsyncTask(this, new DownloadJSONAsyncTask.OnCompleteListener() {

            @Override
            public void run(Response response) {

                progressDlg.dismiss();

                switch(response.getStatus()){
                    case HttpURLConnection.HTTP_OK:
                    case HttpURLConnection.HTTP_NOT_MODIFIED:
                        getLoaderManager().restartLoader(ESTOQUE_LOADER, null, estoqueCursorCallback);
                        break;
                    default:
                        Toast.makeText(getApplicationContext(),
                                "Erro " + response.getStatus()+ ": "+ response.getMessage(),
                                Toast.LENGTH_LONG).show();
                }


            }
        }).execute(RemotePath.EstoquePath);
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

    private class EstoqueCursorCallback implements LoaderManager.LoaderCallbacks<Cursor> {

        public Loader<Cursor> onCreateLoader(int id, Bundle args) {

            if (id == ESTOQUE_LOADER) {
                // consulta apenas os produtos com quantidade >0
                String[] projection = {EstoqueProvider.PRODUTO_ID, EstoqueProvider.PRODUTO};
                String selection = null;
                String[] selectionArgs = null;

                if (args!=null && args.containsKey("query")) {
                    selection = EstoqueProvider.PRODUTO_ASCII + " LIKE ?";
                    selectionArgs = new String[]{"%"+ args.getString("query")+"%"};
                }
                return new CursorLoader(getApplicationContext(), EstoqueProvider.CONTENT_URI_PRODUTOS,
                        projection, selection, selectionArgs, EstoqueProvider.PRODUTO);
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) { estoqueListAdapter.setCursor(cursor); }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            estoqueListAdapter.setCursor(null);
        }

    }

}
