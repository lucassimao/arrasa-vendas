package br.com.arrasavendas.entregas;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ExpandableListView;
import android.widget.SearchView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import br.com.arrasavendas.UpdateDBAsyncTask;
import br.com.arrasavendas.R;
import br.com.arrasavendas.model.Cidade;
import br.com.arrasavendas.model.FormaPagamento;
import br.com.arrasavendas.model.StatusVenda;
import br.com.arrasavendas.model.Venda;
import br.com.arrasavendas.model.Vendedor;
import br.com.arrasavendas.providers.VendasProvider;
import br.com.arrasavendas.service.VendaService;
import br.com.arrasavendas.util.Response;
import static br.com.arrasavendas.Application.ENTREGAS_LOADER;

// http://www.technotalkative.com/contextual-action-bar-cab-android/

public class EntregasActivity extends FragmentActivity {

    private static final int EDIT_ITENS_VENDA_RESULT = 1;
    private static final int MENU_SEARCH = 0;
    private static final String TIPO_FILTRO = "TIPO_FILTRO";
    Venda vendaSelecionada = null;
    View vendaSelecionadaView = null;
    private ActionMode actionMode = null;
    private ActionMode.Callback callback = new EntregasActionBarCallback(this);
    private EntregasExpandableListAdapter vendasListAdapter;
    private ExpandableListView list;
    private EntregasCursorCallback entregasCursorCallback = new EntregasCursorCallback();
    private Menu menu;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_OK && requestCode == EDIT_ITENS_VENDA_RESULT) {
            if (actionMode != null) {
                actionMode.finish();
                actionMode = null;
            }
            getLoaderManager().restartLoader(ENTREGAS_LOADER, null, entregasCursorCallback);
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Bundle bundle = new Bundle();
            bundle.putString("query",query);
            bundle.putSerializable(TIPO_FILTRO,TipoFiltro.NOME_CLIENTE);
            getLoaderManager().restartLoader(ENTREGAS_LOADER, bundle, entregasCursorCallback);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entregas);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        getLoaderManager().initLoader(ENTREGAS_LOADER, null, entregasCursorCallback);

        vendasListAdapter = new EntregasExpandableListAdapter(this);
        list = (ExpandableListView) findViewById(R.id.listItemsEntregas);
        list.setAdapter(vendasListAdapter);

        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                if (actionMode != null) {
                    actionMode.finish();
                    actionMode = null;
                }

                Venda venda = (Venda) list.getItemAtPosition(position);
                EntregasActivity.this.vendaSelecionada = venda;
                EntregasActivity.this.vendaSelecionadaView = view;

                // deixando o item da lista com uma sombra para destacar
                float[] hsv = new float[3];
                int color = ((ColorDrawable) view.getBackground()).getColor();
                Color.colorToHSV(color, hsv);
                hsv[2] *= 0.8f; // value component
                view.setBackgroundColor(Color.HSVToColor(hsv));

                actionMode = startActionMode(EntregasActivity.this.callback);
                return true;
            }
        });

        list.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
                if (actionMode != null) {
                    actionMode.finish();
                    actionMode = null;
                }
            }
        });

    }

    void updateDataEntrega() {
        final Calendar dataEntrega = Calendar.getInstance();
        if (vendaSelecionada.getDataEntrega() != null)
            dataEntrega.setTime(vendaSelecionada.getDataEntrega());
        else
            dataEntrega.setTime(new Date());

        DatePickerDialog dlg = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker arg0, int ano, int mes, int diaDoMes) {

                TimeZone timeZone = TimeZone.getTimeZone("Etc/UTC");
                final Calendar novaDataDeEntrega = GregorianCalendar.getInstance(timeZone);
                novaDataDeEntrega.set(Calendar.YEAR, ano);
                novaDataDeEntrega.set(Calendar.MONTH, mes);
                novaDataDeEntrega.set(Calendar.DAY_OF_MONTH, diaDoMes);

                final ProgressDialog dlg = ProgressDialog.show(EntregasActivity.this, "Atualizando data de entrega", "Aguarde ...");

                SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));

                try {
                    JSONObject obj = new JSONObject();
                    obj.put("dataEntrega", sdf.format(novaDataDeEntrega.getTime()));

                    new UpdateVendaAsyncTask(vendaSelecionada.getId(), obj, new UpdateVendaAsyncTask.OnComplete() {

                        @Override
                        public void run(Response response) {
                            dlg.dismiss();
                            int statusCode = response.getStatus();

                            if (statusCode == HttpURLConnection.HTTP_OK) {
                                try {
                                    VendaService service = new VendaService(getApplicationContext());
                                    JSONObject venda = new JSONObject(response.getMessage());

                                    service.update(vendaSelecionada.getId(), venda);
                                    vendaSelecionada.setDataEntrega(novaDataDeEntrega.getTime());
                                    vendasListAdapter.refreshView();
                                    actionMode.finish();

                                    Toast.makeText(EntregasActivity.this, "Data de entrega atualizada!", Toast.LENGTH_LONG).show();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    String string = "Erro :" + response.getMessage();
                                    Toast.makeText(EntregasActivity.this, string, Toast.LENGTH_LONG).show();
                                }
                            } else {
                                String string = "Erro " + statusCode + ": " + response.getMessage();
                                Toast.makeText(EntregasActivity.this, string, Toast.LENGTH_LONG).show();
                            }

                        }
                    }).execute();

                } catch (JSONException e) {
                    e.printStackTrace();
                    String string = "Erro : " + e.getMessage();
                    Toast.makeText(EntregasActivity.this, string, Toast.LENGTH_LONG).show();

                }

            }

        }, dataEntrega.get(Calendar.YEAR),
                dataEntrega.get(Calendar.MONTH),
                dataEntrega.get(Calendar.DAY_OF_MONTH));

        dlg.setTitle("Atualize a data da entrega");
        dlg.show();
    }

    void editarItensVenda() {
        Intent intent = new Intent(this, EditItensVendaActivity.class);
        intent.putExtra(EditItensVendaActivity.VENDA, this.vendaSelecionada);
        startActivityForResult(intent, EDIT_ITENS_VENDA_RESULT);
    }

    void showEditVendaDialog() {
        Venda clone = null;

        // clonando a venda p/ que os Fragmentos de edição
        // da venda possam atualizar livrimente os dados da venda
        // Caso a atualizaçao seja cancelada, o clone eh simplismente
        // descartado
        try {
            clone = (Venda) this.vendaSelecionada.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        EditVendaDialog dlg = EditVendaDialog.newInstance(clone);


        dlg.setListener(new EditVendaDialog.Listener() {

            @Override
            public void onOK(final Venda updatedVenda) {

                final ProgressDialog dlg = ProgressDialog.show(EntregasActivity.this, "Atualizando Venda", "Aguarde ...");
                JSONObject obj = new JSONObject();

                try {
                    if (updatedVenda.getCliente().getCidade().getId() != Cidade.TERESINA_ID)
                        obj.put("dataEntrega", "");

                    obj.put("cliente", updatedVenda.getCliente().toJson());
                    obj.put("abatimentoEmCentavos", updatedVenda.getAbatimentoEmCentavos());
                    obj.put("formaPagamento", updatedVenda.getFormaDePagamento().name());
                    obj.put("turnoEntrega", updatedVenda.getTurnoEntrega().name());
                    obj.put("status", updatedVenda.getStatus().name());
                    obj.put("flagClienteVaiBuscar", updatedVenda.isFlagVaiBuscar());
                    obj.put("flagClienteJaBuscou", updatedVenda.isFlagJaBuscou());

                    if (updatedVenda.getServicoCorreios() == null)
                        obj.put("servicoCorreio", "");
                    else
                        obj.put("servicoCorreio", updatedVenda.getServicoCorreios().name());

                    obj.put("freteEmCentavos", updatedVenda.getFreteEmCentavos());
                    obj.put("codigoRastreio", updatedVenda.getCodigoRastreio());


                    Vendedor vendedor = updatedVenda.getVendedor();
                    if (vendedor != null) {
                        JSONObject vendedorJSONObj = new JSONObject();
                        vendedorJSONObj.put("id", vendedor.getId());
                        obj.put("vendedor", vendedorJSONObj);
                    } else
                        obj.put("vendedor", "null"); // site

//                    Log.d(getClass().getName(), RemotePath.getEntityPath(RemotePath.VendaPath,vendaSelecionada.getId()));
//                    Log.d(getClass().getName(), obj.toString(1));


                    new UpdateVendaAsyncTask(EntregasActivity.this.vendaSelecionada.getId(), obj, new UpdateVendaAsyncTask.OnComplete() {

                        @Override
                        public void run(Response response) {
                            dlg.dismiss();

                            int statusCode = response.getStatus();

                            if (statusCode == HttpURLConnection.HTTP_OK) {

                                EntregasActivity.this.vendaSelecionada.setCliente(updatedVenda.getCliente());
                                EntregasActivity.this.vendaSelecionada.setTurnoEntrega(updatedVenda.getTurnoEntrega());
                                EntregasActivity.this.vendaSelecionada.setFormaDePagamento(updatedVenda.getFormaDePagamento());
                                EntregasActivity.this.vendaSelecionada.setStatus(updatedVenda.getStatus());
                                EntregasActivity.this.vendaSelecionada.setAbatimentoEmCentavos(updatedVenda.getAbatimentoEmCentavos());
                                EntregasActivity.this.vendaSelecionada.setVendedor(updatedVenda.getVendedor());
                                EntregasActivity.this.vendaSelecionada.setServicoCorreios(updatedVenda.getServicoCorreios());
                                EntregasActivity.this.vendaSelecionada.setFreteEmCentavos(updatedVenda.getFreteEmCentavos());
                                EntregasActivity.this.vendaSelecionada.setCodigoRastreio(updatedVenda.getCodigoRastreio());
                                EntregasActivity.this.vendaSelecionada.setFlagVaiBuscar(updatedVenda.isFlagVaiBuscar());
                                EntregasActivity.this.vendaSelecionada.setFlagJaBuscou(updatedVenda.isFlagJaBuscou());

                                vendasListAdapter.refreshView();

                                VendaService service = new VendaService(getApplication());

                                try {
                                    JSONObject object = new JSONObject(response.getMessage());
                                    service.update(EntregasActivity.this.vendaSelecionada.getId(), object);
                                } catch (JSONException e) {
                                    // se der pau, no prox acesso o sistema baixa novamente
                                    e.printStackTrace();
                                }

                                Toast.makeText(EntregasActivity.this, "Venda atualizada com sucesso!", Toast.LENGTH_LONG).show();
                                actionMode.finish();
                            } else {
                                String string = "Erro " + statusCode + ": " + response.getMessage();
                                Toast.makeText(EntregasActivity.this, string, Toast.LENGTH_LONG).show();
                            }


                        }
                    }).execute();

                } catch (JSONException e) {
                    e.printStackTrace();
                    String string = "Erro: " + e.getMessage();
                    Toast.makeText(EntregasActivity.this, string, Toast.LENGTH_LONG).show();

                }

            }


        });

        dlg.show(getSupportFragmentManager(), "editVenda");
    }

    void excluirVenda() {
        final Venda venda = this.vendaSelecionada;

        new AlertDialog.Builder(this)
                .setTitle("Rapaz ...")
                .setMessage("Tem certeza?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        final ProgressDialog dlg = ProgressDialog.show(EntregasActivity.this, "Excluindo", "Aguarde ...");

                        new ExcluirVendaAsyncTask(venda.getId(), new ExcluirVendaAsyncTask.OnComplete() {

                            @Override
                            public void run(Response response) {
                                dlg.dismiss();

                                int statusCode = response.getStatus();

                                if (statusCode == HttpURLConnection.HTTP_NO_CONTENT) {

                                    VendaService service = new VendaService(EntregasActivity.this);
                                    service.delete(venda.getId());
                                    vendasListAdapter.removerVenda(venda);

                                    Toast.makeText(EntregasActivity.this, "Venda excluida!",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(EntregasActivity.this,
                                            "Erro " + statusCode + ": " + response.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }

                                actionMode.finish();

                            }
                        }, EntregasActivity.this).execute();


                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }


    public void sincronizar() {

        final ProgressDialog progressDlg = ProgressDialog.show(this, "Atualizando informações", "Aguarde ...");
        vendasListAdapter.setCursor(null);
        new UpdateDBAsyncTask(this, new UpdateDBAsyncTask.OnCompleteListener() {

            @Override
            public void run(Response response) {
                progressDlg.dismiss();

                switch (response.getStatus()) {
                    case HttpURLConnection.HTTP_OK:
                    case HttpURLConnection.HTTP_NO_CONTENT:
                        getLoaderManager().restartLoader(ENTREGAS_LOADER, null,
                                entregasCursorCallback);
                        break;
                    default:
                        Toast.makeText(getApplicationContext(),
                                "Erro " + response.getStatus() + ": " + response.getMessage(),
                                Toast.LENGTH_LONG).show();
                }

            }
        }).execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // ocultando o campo de busca
        menu.findItem(R.id.search).collapseActionView();
        Bundle bundle = new Bundle();

        switch (item.getItemId()) {
            case R.id.sync:
                sincronizar();
                return true;
            case R.id.show_all:
                vendasListAdapter.setCursor(null);
                bundle.putSerializable(TIPO_FILTRO,TipoFiltro.TODOS);
                break;
            case R.id.show_entregas_por_correios:
                bundle.putSerializable(TIPO_FILTRO,TipoFiltro.ENTREGAS_POR_CORREIOS);
                break;
            case R.id.show_cliente_vai_buscar:
                bundle.putSerializable(TIPO_FILTRO,TipoFiltro.CLIENTE_VAI_BUSCAR);
                break;
            case R.id.show_iniciados_e_nao_finalizados:
                bundle.putSerializable(TIPO_FILTRO,TipoFiltro.NAO_FINALIZADOS);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        getLoaderManager().restartLoader(ENTREGAS_LOADER, bundle,entregasCursorCallback);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.entregas_activity_menu, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();

        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        MenuItem mSearchMenu = menu.findItem(R.id.search);

        mSearchMenu.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true; // Return true to expand action view
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                getLoaderManager().restartLoader(ENTREGAS_LOADER, null, entregasCursorCallback);
                return true; // Return true to collapse action view
            }
        });

        this.menu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    void adicionarAnexo() {
        Intent intent = new Intent(this, AnexosManagerActivity.class);
        intent.putExtra(AnexosManagerActivity.VENDA_ID, vendaSelecionada.getId());
        startActivity(intent);
    }

    private class EntregasCursorCallback implements LoaderManager.LoaderCallbacks<Cursor> {
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (id == ENTREGAS_LOADER) {
                String selection = null;
                String[] selectionArgs = null;
                if (args!=null){
                    TipoFiltro filtro = (TipoFiltro) args.getSerializable(TIPO_FILTRO);
                    switch (filtro){
                        case NOME_CLIENTE:
                            selection = VendasProvider.CLIENTE +" LIKE ?";
                            String query = args.getString("query");
                            selectionArgs = new String[]{"%"+ query +"%,\"dddCelular\"%"};
                            break;
                        case ENTREGAS_POR_CORREIOS:
                            selection = VendasProvider.DATA_ENTREGA +" = ?";
                            selectionArgs = new String[]{"-1"};
                            break;
                        case CLIENTE_VAI_BUSCAR:
                            selection = VendasProvider.FLAG_VAI_BUSCAR +" = ?";
                            selectionArgs = new String[]{"1"};
                            break;
                        case NAO_FINALIZADOS:
                            selection = VendasProvider.FORMA_PAGAMENTO +" = ? AND " + VendasProvider.STATUS + "=?";
                            selectionArgs = new String[]{FormaPagamento.PagSeguro.name(),
                                    StatusVenda.AguardandoPagamento.name()};
                            break;
                    }
                }
                return new CursorLoader(getApplicationContext(),
                        VendasProvider.CONTENT_URI, null, selection, selectionArgs, VendasProvider.DATA_ENTREGA);
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            vendasListAdapter.setCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            vendasListAdapter.setCursor(null);
        }

    }

}
