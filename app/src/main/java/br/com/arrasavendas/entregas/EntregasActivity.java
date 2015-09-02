package br.com.arrasavendas.entregas;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import br.com.arrasavendas.DownloadJSONFeedTask;
import br.com.arrasavendas.R;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.providers.VendasProvider;
import br.com.arrasavendas.model.Cliente;
import br.com.arrasavendas.model.FormaPagamento;
import br.com.arrasavendas.model.ItemVenda;
import br.com.arrasavendas.model.StatusVenda;
import br.com.arrasavendas.model.TurnoEntrega;
import br.com.arrasavendas.model.Venda;

public class EntregasActivity extends Activity {

    private VendasExpandableListAdapter vendasListAdapter;
    private ExpandableListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entregas);

        list = (ExpandableListView) findViewById(R.id.listItemsEntregas);

        List<Venda> vendas = getData();
        vendasListAdapter = new VendasExpandableListAdapter(getFragmentManager(), this, vendas);
        list.setAdapter(vendasListAdapter);

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

    public void onClickBtnSincronizar(View v) {

        final ProgressDialog progressDlg = ProgressDialog.show(this, "Atualizando informações", "Aguarde ...");
        new DownloadJSONFeedTask(RemotePath.VendaPath, this, new Runnable() {

            @Override
            public void run() {
                progressDlg.dismiss();

                List<Venda> vendas = getData();
                vendasListAdapter = new VendasExpandableListAdapter(getFragmentManager(), EntregasActivity.this, vendas);
                list.setAdapter(vendasListAdapter);

            }
        }).execute();
    }

    public void onClickSair(View v) {
        finish();
    }

}
