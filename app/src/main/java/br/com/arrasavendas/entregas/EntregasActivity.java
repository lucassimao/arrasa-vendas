package br.com.arrasavendas.entregas;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ExpandableListView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import br.com.arrasavendas.DownloadJSONFeedTask;
import br.com.arrasavendas.R;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.model.Cliente;
import br.com.arrasavendas.model.StatusVenda;
import br.com.arrasavendas.model.TurnoEntrega;
import br.com.arrasavendas.model.Venda;
import br.com.arrasavendas.providers.VendasProvider;

// http://www.technotalkative.com/contextual-action-bar-cab-android/

public class EntregasActivity extends Activity{

    private static final int ENTREGAS_LOADER = 1;
    private static final int EDIT_ITENS_VENDA_RESULT = 1;
    Venda vendaSelecionada = null;
    View vendaSelecionadaView = null;
    private ActionMode actionMode = null;
    private ActionMode.Callback callback = new EntregasActionBarCallback(this);
    private EntregasExpandableListAdapter vendasListAdapter;
    private ExpandableListView list;
    private EntregasCursorCallback entregasCursorCallback = new EntregasCursorCallback();


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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entregas);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        getLoaderManager().initLoader(ENTREGAS_LOADER, null, entregasCursorCallback);

        vendasListAdapter = new EntregasExpandableListAdapter(this, null);
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
        dataEntrega.setTime(vendaSelecionada.getDataEntrega());

        DatePickerDialog dlg = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker arg0, int ano, int mes, int diaDoMes) {

                TimeZone timeZone = TimeZone.getTimeZone("Etc/UTC");
                final Calendar novaDataDeEntrega = GregorianCalendar.getInstance(timeZone);
                novaDataDeEntrega.set(Calendar.YEAR, ano);
                novaDataDeEntrega.set(Calendar.MONTH, mes);
                novaDataDeEntrega.set(Calendar.DAY_OF_MONTH, diaDoMes);

                final ProgressDialog dlg = ProgressDialog.show(EntregasActivity.this, "Atualizando data de entrega", "Aguarde ...");

                new UpdateDataEntregaVendaAsyncTask(vendaSelecionada.getId(), novaDataDeEntrega.getTime(), new br.com.arrasavendas.entregas.UpdateDataEntregaVendaAsyncTask.OnComplete() {

                    @Override
                    public void run(HttpResponse response) {
                        dlg.dismiss();
                        int statusCode = response.getStatusLine().getStatusCode();

                        if (statusCode == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
                            String string = "Erro ao atualizar venda, verifique se todos os campos foram preenchidos!";
                            Toast.makeText(EntregasActivity.this, string, Toast.LENGTH_LONG).show();

                        } else if (statusCode == HttpStatus.SC_OK) {
                            vendaSelecionada.setDataEntrega(novaDataDeEntrega.getTime());
                            vendasListAdapter.atualizarDatasDeEntregas();
                            vendasListAdapter.notifyDataSetChanged();
                            actionMode.finish();
                            Toast.makeText(EntregasActivity.this, "Data de entrega atualizada com sucesso!", Toast.LENGTH_LONG).show();

                        } else {
                            Toast.makeText(EntregasActivity.this, "Erro " + statusCode, Toast.LENGTH_SHORT).show();
                        }

                    }
                }, EntregasActivity.this).execute();

            }


        }, dataEntrega.get(Calendar.YEAR), dataEntrega.get(Calendar.MONTH), dataEntrega.get(Calendar.DAY_OF_MONTH));

        dlg.setTitle("Atualize a data da entrega");
        dlg.show();
    }

    void editarItensVenda() {
        Intent intent = new Intent(this, EditItensVendaActivity.class);
        intent.putExtra(EditItensVendaActivity.VENDA, this.vendaSelecionada);
        startActivityForResult(intent, EDIT_ITENS_VENDA_RESULT);
    }

    void showEditClienteDialog() {
        EditClienteDialog dlg = new EditClienteDialog();
        Bundle bundle = new Bundle();
        bundle.putSerializable(EditClienteDialog.VENDA, this.vendaSelecionada);
        dlg.setArguments(bundle);


        dlg.setClienteDialogListener(new EditClienteDialog.ClienteDialogListener() {

            @Override
            public void onPositiveClick(final Cliente updatedCliente, final TurnoEntrega turnoEntrega, final StatusVenda statusVenda) {

                final ProgressDialog dlg = ProgressDialog.show(EntregasActivity.this, "Atualizando activity_venda", "Aguarde ...");

                new UpdateClienteVendaAsyncTask(vendaSelecionada.getId(), updatedCliente, turnoEntrega, statusVenda, new UpdateClienteVendaAsyncTask.OnComplete() {

                    @Override
                    public void run(HttpResponse response) {
                        dlg.dismiss();

                        int statusCode = response.getStatusLine().getStatusCode();

                        if (statusCode == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
                            String string = "Erro ao atualizar activity_venda, verifique se todos os campos foram preenchidos!";
                            Toast.makeText(EntregasActivity.this, string, Toast.LENGTH_LONG).show();

                        } else if (statusCode == HttpStatus.SC_OK) {
                            vendaSelecionada.setCliente(updatedCliente);
                            vendaSelecionada.setTurnoEntrega(turnoEntrega);
                            vendaSelecionada.setStatus(statusVenda);
                            vendasListAdapter.refreshView();

                            Toast.makeText(EntregasActivity.this, "Venda atualizada com sucesso!", Toast.LENGTH_LONG).show();
                            actionMode.finish();

                        } else {
                            Toast.makeText(EntregasActivity.this, "Erro " + statusCode, Toast.LENGTH_SHORT).show();
                        }


                    }
                }, EntregasActivity.this).execute();


            }


        });


        dlg.show(getFragmentManager(), "tag");
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
                            public void run(HttpResponse response) {
                                dlg.dismiss();

                                int statusCode = response.getStatusLine().getStatusCode();

                                switch (statusCode) {
                                    case HttpStatus.SC_NO_CONTENT:
                                        vendasListAdapter.removerVenda(venda);
                                        Toast.makeText(EntregasActivity.this, "Venda excluida com sucesso!", Toast.LENGTH_LONG).show();
                                        break;
                                    case HttpStatus.SC_UNPROCESSABLE_ENTITY:
                                        Toast.makeText(EntregasActivity.this, "Erro ao excluir activity_venda!", Toast.LENGTH_LONG).show();
                                        break;
                                    default:
                                        Toast.makeText(EntregasActivity.this, "Erro " + statusCode, Toast.LENGTH_SHORT).show();
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
        new DownloadJSONFeedTask(this, new Runnable() {

            @Override
            public void run() {
                progressDlg.dismiss();
                getLoaderManager().restartLoader(ENTREGAS_LOADER, null, entregasCursorCallback);

            }
        }).execute(RemotePath.VendaPath);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sync:
                sincronizar();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.entregas_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    void adicionarAnexo() {
        Intent intent = new Intent(this, AnexosManagerActivity.class);
        intent.putExtra(AnexosManagerActivity.VENDA_ID, vendaSelecionada.getId());
        startActivity(intent);
    }

    private class EntregasCursorCallback implements LoaderManager.LoaderCallbacks<Cursor>{
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (id == ENTREGAS_LOADER) {
                return new CursorLoader(getApplicationContext(), VendasProvider.CONTENT_URI, null, null, null, VendasProvider.DATA_ENTREGA);
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
