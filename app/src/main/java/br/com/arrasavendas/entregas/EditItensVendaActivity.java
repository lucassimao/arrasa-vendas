package br.com.arrasavendas.entregas;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

import br.com.arrasavendas.DownloadJSONFeedTask;
import br.com.arrasavendas.R;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.model.ItemVenda;
import br.com.arrasavendas.model.Venda;
import br.com.arrasavendas.providers.EstoqueProvider;
import br.com.arrasavendas.providers.VendasProvider;
import br.com.arrasavendas.venda.UnidadeQuantidadeDialogFragment;

public class EditItensVendaActivity extends Activity {

    public static final String VENDA = "EditItensVendaActivity.VENDA_OBJ";
    private static final int PRODUTOS_LOADER = 1;
    private Venda venda;
    private AutoCompleteTextView autoCompleteProduto;
    private ListView listItensVenda;
    private EditItensVendaBaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_itens_venda);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        this.venda = (Venda) getIntent().getSerializableExtra(VENDA);
        if (this.venda == null)
            throw new IllegalArgumentException("Venda deve ser selecionada");

        getActionBar().setTitle("Itens de " + this.venda.getCliente().getNome());

        listItensVenda = (ListView) findViewById(R.id.list_itens_venda);
        adapter = new EditItensVendaBaseAdapter(this, this.venda);
        listItensVenda.setAdapter(adapter);

        configurarAutoCompleteTextViewProduto();

    }



    public void onClickAddProduto(View v) {

        if (autoCompleteProduto.getTag() == null) {

            String text = "Escolha o produto primeiro!";
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            autoCompleteProduto.requestFocus();

        } else {

            final String produto = autoCompleteProduto.getText().toString();
            UnidadeQuantidadeDialogFragment fragment = UnidadeQuantidadeDialogFragment.newInstance(produto);

            fragment.setOnAdicionarListener(new UnidadeQuantidadeDialogFragment.OnAdicionarListener() {

                @Override
                public void onClickBtnAdicionar(long idProduto, String unidade, Integer novaQuantidade, BigDecimal precoAVista, BigDecimal precoAPrazo) {

                    adapter.addToItens(new ItemVenda(null, idProduto, produto, unidade, novaQuantidade, precoAVista, precoAPrazo));

                    autoCompleteProduto.setText("");
                    autoCompleteProduto.setTag(null);
                }
            });

            fragment.show(getFragmentManager(), "dialog");
        }

    }

    private void configurarAutoCompleteTextViewProduto() {

        final String[] colunas = {EstoqueProvider.PRODUTO, EstoqueProvider._ID};

        autoCompleteProduto = (AutoCompleteTextView) findViewById(R.id.auto_complete_produto);

        SimpleCursorAdapter autoCompleteProdutoCursorAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_dropdown_item_1line, null, colunas,
                new int[]{android.R.id.text1},
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        autoCompleteProduto.setAdapter(autoCompleteProdutoCursorAdapter);

        autoCompleteProduto.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                autoCompleteProduto.setTag(id);
            }

        });

        autoCompleteProdutoCursorAdapter
                .setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {

                    @Override
                    public CharSequence convertToString(Cursor cursor) {
                        return cursor.getString(cursor
                                .getColumnIndex(EstoqueProvider.PRODUTO));
                    }
                });

        autoCompleteProdutoCursorAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence arg0) {
                if (TextUtils.isEmpty(arg0))
                    return null;

                CursorLoader cl = new CursorLoader(getApplicationContext(),
                        EstoqueProvider.CONTENT_URI_PRODUTOS, colunas,
                        "produto_nome_ascii like ?1 or produto_nome like ?1 ", new String[]{"%"
                        + arg0.toString() + "%"}, null);
                return cl.loadInBackground();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_itens_venda_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                salvarVenda(venda);
                return true;
            case R.id.cancel:
                adapter.rollback();
                setResult(Activity.RESULT_CANCELED);
                finish();
                return true;
            case android.R.id.home: // no click do up button, da rollback tb e deixa chamar super.onOptionsItemSelected
                adapter.rollback();
                setResult(Activity.RESULT_CANCELED);
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void salvarVenda(final Venda venda) {

        JSONObject obj = new JSONObject();

        try {

            JSONArray array = new JSONArray();
            JSONObject carrinho = new JSONObject();

            for (ItemVenda item : venda.getItens()) {

                JSONObject jsonObj = item.asJsonObject();
                array.put(jsonObj);

            }
            carrinho.put("itens", array);
            obj.put("carrinho", carrinho);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final ProgressDialog progressDlg = ProgressDialog.show(this, "Atualizando informações", "Aguarde ...");
        new UpdateVendaAsyncTask(venda.getId(), obj, new UpdateVendaAsyncTask.OnComplete() {
            @Override
            public void run(HttpResponse response) {
                String msg = null;

                progressDlg.dismiss();

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    // atualizando o estoque e a lista de vendas
                    new DownloadJSONFeedTask(EditItensVendaActivity.this, null).execute(RemotePath.EstoquePath);

                    ContentValues cv = new ContentValues();
                    JSONArray array = new JSONArray();

                    for (ItemVenda item : venda.getItens()) {
                        JSONObject obj = new JSONObject();

                        try {
                            obj.put("produto_nome", item.getNomeProduto());
                            obj.put("id", (item.getId() == null) ? -1 : item.getId());
                            obj.put("produto_id", item.getProdutoID());
                            obj.put("unidade", item.getUnidade());
                            obj.put("quantidade", item.getQuantidade());
                            obj.put("precoAVistaEmCentavos", item.getPrecoAVista().doubleValue() * 100);
                            obj.put("precoAPrazoEmCentavos", item.getPrecoAPrazo().doubleValue() * 100);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        array.put(obj);
                    }
                    cv.put(VendasProvider.CARRINHO, array.toString());
                    getContentResolver().update(VendasProvider.CONTENT_URI, cv, VendasProvider._ID + "=?", new String[]{venda.getId().toString()});

                    msg = "Venda atualizada com sucesso";
                    Toast.makeText(EditItensVendaActivity.this, msg, Toast.LENGTH_SHORT).show();

                    setResult(Activity.RESULT_OK);
                    finish();

                } else {
                    Toast.makeText(EditItensVendaActivity.this, response.getStatusLine().getReasonPhrase(), Toast.LENGTH_SHORT).show();
                }


            }
        }).execute();

    }

}
