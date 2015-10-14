package br.com.arrasavendas.entregas;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import br.com.arrasavendas.DownloadJSONFeedTask;
import br.com.arrasavendas.R;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.model.Cliente;
import br.com.arrasavendas.model.FormaPagamento;
import br.com.arrasavendas.model.ItemVenda;
import br.com.arrasavendas.model.StatusVenda;
import br.com.arrasavendas.model.TurnoEntrega;
import br.com.arrasavendas.model.Venda;
import br.com.arrasavendas.providers.VendasProvider;

// http://www.technotalkative.com/contextual-action-bar-cab-android/

public class EntregasActivity extends Activity {

    private VendasExpandableListAdapter vendasListAdapter;
    private ExpandableListView list;
    Venda vendaSelecionada = null;
    View vendaSelecionadaView = null;
    ActionMode actionMode = null;
    ActionBarCallBack callback = new ActionBarCallBack();

    class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            actionMode.getMenuInflater().inflate(R.menu.entregas_contextual_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
//            actionMode.setTitle("CheckBox is Checked");
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.edit_address:
                    showEditClienteDialog();
                    break;
                case R.id.update_data_entrega:
                    updateDataEntrega();
                    break;
                case R.id.delete:
                    excluirVenda(vendaSelecionada);
                    break;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {

            View view = EntregasActivity.this.vendaSelecionadaView;
            Venda venda = EntregasActivity.this.vendaSelecionada;

            restoreViewBackground(view, venda);

            EntregasActivity.this.vendaSelecionada = null;
            EntregasActivity.this.vendaSelecionadaView = null;
        }
    }

    private void restoreViewBackground(View view, Venda venda) {
        if (venda.getVendedor() == null)
            view.setBackgroundColor(Color.WHITE);
        else
            switch (venda.getVendedor()) {
                case Lucas:
                    view.setBackgroundColor(getResources().getColor(R.color.blueLucas));
                    break;
                case Adna:
                    view.setBackgroundColor(getResources().getColor(R.color.pinkAdna));
                    break;
                case MariaClara:
                    view.setBackgroundColor(getResources().getColor(R.color.amareloMClara));
                    break;
                default:

            }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entregas);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        list = (ExpandableListView) findViewById(R.id.listItemsEntregas);

        List<Venda> vendas = getData();
        vendasListAdapter = new VendasExpandableListAdapter(this, vendas);
        list.setAdapter(vendasListAdapter);

        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                if (actionMode != null){
                    actionMode.finish();
                    actionMode = null;
                }

                Venda venda = (Venda) list.getItemAtPosition(position);
                EntregasActivity.this.vendaSelecionada = venda;
                EntregasActivity.this.vendaSelecionadaView = view;

                float[] hsv = new float[3];
                int color = ((ColorDrawable)view.getBackground()).getColor();
                Color.colorToHSV(color, hsv);
                hsv[2] *= 0.8f; // value component
                view.setBackgroundColor(Color.HSVToColor(hsv));

                actionMode =  startActionMode(callback);
                return true;
            }
        });

    }

    private void updateDataEntrega() {
        final Calendar dataEntrega = Calendar.getInstance();
        dataEntrega.setTime(vendaSelecionada.getDataEntrega());


        DatePickerDialog dlg = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker arg0, int ano, int mes, int diaDoMes) {

                final Calendar novaDataDeEntrega = GregorianCalendar.getInstance(TimeZone.getTimeZone("Etc/UTC"));
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

    private void showEditClienteDialog() {
        EditClienteDialog dlg = new EditClienteDialog();
        Bundle bundle = new Bundle();
        bundle.putSerializable("venda", this.vendaSelecionada);
        dlg.setArguments(bundle);


        dlg.setClienteDialogListener(new EditClienteDialog.ClienteDialogListener() {

            @Override
            public void onPositiveClick(final Cliente updatedCliente, final TurnoEntrega turnoEntrega, final StatusVenda statusVenda) {

                final ProgressDialog dlg = ProgressDialog.show(EntregasActivity.this, "Atualizando venda", "Aguarde ...");

                new UpdateClienteVendaAsyncTask(vendaSelecionada.getId(), updatedCliente, turnoEntrega, statusVenda, new br.com.arrasavendas.entregas.UpdateClienteVendaAsyncTask.OnComplete() {

                    @Override
                    public void run(HttpResponse response) {
                        dlg.dismiss();

                        int statusCode = response.getStatusLine().getStatusCode();

                        if (statusCode == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
                            String string = "Erro ao atualizar venda, verifique se todos os campos foram preenchidos!";
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

    private void excluirVenda(final Venda venda) {
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
                                        Toast.makeText(EntregasActivity.this, "Erro ao excluir venda!", Toast.LENGTH_LONG).show();
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




    private List<Venda> getData() {
        CursorLoader cLoader = new CursorLoader(getApplicationContext(), VendasProvider.CONTENT_URI, null, null, null, VendasProvider.DATA_ENTREGA);

        Cursor cursor = cLoader.loadInBackground();

        List<Venda> vendas = new LinkedList<Venda>();

        while (cursor.moveToNext()) {

            Venda v = new Venda();

            Calendar dataEntrega = Calendar.getInstance();
            dataEntrega.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(VendasProvider.DATA_ENTREGA)));
            v.setDataEntrega(dataEntrega.getTime());

            v.setId(cursor.getLong(cursor.getColumnIndex(VendasProvider._ID)));
            v.setVendedor(cursor.getString(cursor.getColumnIndex(VendasProvider.VENDEDOR)));

            String formaPagamento = cursor.getString(cursor.getColumnIndex(VendasProvider.FORMA_PAGAMENTO));
            v.setFormaDePagamento(FormaPagamento.valueOf(formaPagamento));

            String status = cursor.getString(cursor.getColumnIndex(VendasProvider.STATUS));
            v.setStatus(StatusVenda.valueOf(status));

            String turnoEntrega = cursor.getString(cursor.getColumnIndex(VendasProvider.TURNO_ENTREGA));
            v.setTurnoEntrega(TurnoEntrega.valueOf(turnoEntrega));

            try {

                JSONObject clienteJSONObj = new JSONObject(cursor.getString(cursor.getColumnIndex(VendasProvider.CLIENTE)));

                Cliente cliente = new Cliente();
                cliente.setId(clienteJSONObj.getLong("id"));
                cliente.setNome(clienteJSONObj.getString("nome"));
                cliente.setCelular(clienteJSONObj.getString("celular"));
                cliente.setDddCelular(clienteJSONObj.getString("dddCelular"));
                cliente.setDddTelefone(clienteJSONObj.getString("dddTelefone"));
                cliente.setTelefone(clienteJSONObj.getString("telefone"));

                JSONObject enderecoJSONObj = clienteJSONObj.getJSONObject("endereco");
                cliente.setEndereco(enderecoJSONObj.getString("complemento"));
                cliente.setBairro(enderecoJSONObj.getString("bairro"));

                v.setCliente(cliente);

                JSONArray itens = new JSONArray(cursor.getString(cursor.getColumnIndex(VendasProvider.CARRINHO)));

                for (int i = 0; i < itens.length(); ++i) {
                    JSONObject jsonObj = itens.getJSONObject(i);

                    String produto = jsonObj.getString("produto_nome");
                    Long produtoID = jsonObj.getLong("produto_id");
                    String unidade = jsonObj.getString("unidade");
                    Integer quantidade = jsonObj.getInt("quantidade");
                    BigDecimal precoAVistaEmReais = BigDecimal.valueOf(jsonObj.getDouble("precoAVistaEmCentavos")).divide(BigDecimal.valueOf(100));
                    BigDecimal precoAPrazoEmReais = BigDecimal.valueOf(jsonObj.getDouble("precoAPrazoEmCentavos")).divide(BigDecimal.valueOf(100));

                    ItemVenda item = new ItemVenda(produtoID, produto, unidade, quantidade, precoAVistaEmReais, precoAPrazoEmReais);
                    v.addToItens(item);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }


            vendas.add(v);

        }

        cursor.close();

        return vendas;
    }

    public void sincronizar() {

        final ProgressDialog progressDlg = ProgressDialog.show(this, "Atualizando informações", "Aguarde ...");
        new DownloadJSONFeedTask(RemotePath.VendaPath, this, new Runnable() {

            @Override
            public void run() {
                progressDlg.dismiss();

                List<Venda> vendas = getData();
                vendasListAdapter = new VendasExpandableListAdapter(EntregasActivity.this, vendas);
                list.setAdapter(vendasListAdapter);

            }
        }).execute();
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
}
