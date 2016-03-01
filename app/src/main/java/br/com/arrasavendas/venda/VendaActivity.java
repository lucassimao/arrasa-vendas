package br.com.arrasavendas.venda;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.widget.*;
import br.com.arrasavendas.Application;
import br.com.arrasavendas.model.*;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup.OnCheckedChangeListener;
import br.com.arrasavendas.R;
import br.com.arrasavendas.providers.EstoqueProvider;
import br.com.arrasavendas.service.VendaService;
import br.com.arrasavendas.util.Response;
import br.com.arrasavendas.venda.SalvarVendaAsyncTask.OnComplete;

public class VendaActivity extends Activity {

    private final DataSetObserver listProdutosObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            atualizarPreco();
        }
    };

    private static final int ITEM_GRAVAR = 0;
    private static final int ITEM_LIMPAR = 1;

    private ListView list;
    private AutoCompleteTextView txtViewProduto;
    private EditText editTextCliente;

    private Date dateEntrega;
    private ListProdutoAdapter listProdutosAdapter;
    private TextView txtValorTotal;
    private RadioGroup radioGroupFormaPagamento;
    private CheckBox checkboxJaPagou;
    private Spinner spinnerTurnoEntrega;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venda);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        listProdutosAdapter = new ListProdutoAdapter(this);
        listProdutosAdapter.registerDataSetObserver(listProdutosObserver);

        list = (ListView) findViewById(R.id.listItensVenda);
        list.setAdapter(listProdutosAdapter);

        editTextCliente = (EditText) findViewById(R.id.editTextCliente);
        checkboxJaPagou = (CheckBox) findViewById(R.id.checkBoxJaPagou);
        txtValorTotal = (TextView) findViewById(R.id.txtValorTotal);
        spinnerTurnoEntrega = (Spinner) findViewById(R.id.spinnerTurnoEntrega);

        radioGroupFormaPagamento = (RadioGroup) findViewById(R.id.radioGroupFormaPagamento);
        radioGroupFormaPagamento.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup arg0, int arg1) {
                atualizarPreco();
            }
        });

        Application app = ((Application) getApplicationContext());

        String currentUser = app.getCurrentUser();
        if (currentUser != null) {
            switch(currentUser){
                case "lsimaocosta@gmail.com":
                    ( (RadioButton) findViewById(R.id.radioVendedorLucas)).setChecked(true);
                    break;
                case "mariaclaravn26@gmail.com":
                    ( (RadioButton) findViewById(R.id.radioVendedorMariaClara)).setChecked(true);
                    break;
                case "fisio.adnadantas@gmail.com":
                    ( (RadioButton) findViewById(R.id.radioVendedorAdna)).setChecked(true);
                    break;
            }
        }


        configurarAutoCompleteTextViewProduto();
    }


    private void configurarAutoCompleteTextViewProduto() {
        txtViewProduto = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewProduto);

        final String[] colunas = new String[]{EstoqueProvider.PRODUTO, EstoqueProvider._ID};

        SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_dropdown_item_1line, null, colunas,
                new int[]{android.R.id.text1},
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        txtViewProduto.setAdapter(simpleCursorAdapter);
        txtViewProduto
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        txtViewProduto.setTag(id);
                    }

                });

        simpleCursorAdapter
                .setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {

                    @Override
                    public CharSequence convertToString(Cursor cursor) {
                        return cursor.getString(cursor
                                .getColumnIndex(EstoqueProvider.PRODUTO));
                    }
                });

        simpleCursorAdapter.setFilterQueryProvider(new FilterQueryProvider() {
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

    public void onClickAddProduto(View v) {

        if (txtViewProduto.getTag() == null) {

            Toast.makeText(this, "Escolha o produto primeiro!",
                    Toast.LENGTH_SHORT).show();
            txtViewProduto.requestFocus();

        } else {

            final String produto = txtViewProduto.getText().toString();
            UnidadeQuantidadeDialogFragment fragment = UnidadeQuantidadeDialogFragment.newInstance(produto);

            fragment.setOnAdicionarListener(new UnidadeQuantidadeDialogFragment.OnAdicionarListener() {

                @Override
                public void onClickBtnAdicionar(long idProduto, String unidade, Integer quantidade, BigDecimal precoAVista, BigDecimal precoAPrazo) {

                    listProdutosAdapter.add(idProduto, produto, unidade, quantidade,precoAVista,precoAPrazo);
                    listProdutosAdapter.notifyDataSetChanged();

                    txtViewProduto.setText("");
                    txtViewProduto.setTag(null);
                    atualizarPreco();
                }
            });

            fragment.show(getFragmentManager(), "dialog");
        }

    }


    protected void atualizarPreco() {
        BigDecimal valorTotal = BigDecimal.ZERO;

        for (int i = 0; i < listProdutosAdapter.getCount(); ++i) {
            ItemVenda item = (ItemVenda) listProdutosAdapter.getItem(i);

            switch (radioGroupFormaPagamento.getCheckedRadioButtonId()) {
                case R.id.radioFormaPagamentoAVista:
                    valorTotal = valorTotal.add(item.getPrecoAVista().multiply(BigDecimal.valueOf(item.getQuantidade())));
                    break;

                case R.id.radioFormaPagamentoParcelado:
                    valorTotal = valorTotal.add(item.getPrecoAPrazo().multiply(BigDecimal.valueOf(item.getQuantidade())));
                    break;
            }

        }

        txtValorTotal
                .setText(String.format("R$ %.2f", valorTotal.doubleValue()));

    }

    public void onClickSelecionarDataEntrega(View view) {

        final Calendar hoje = Calendar.getInstance();

        DatePickerDialog dlg = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker arg0, int ano, int mes, int diaDoMes) {

                        TextView txtViewDataEntrega = (TextView) findViewById(R.id.txtViewDataEntrega);
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.YEAR, ano);
                        calendar.set(Calendar.MONTH, mes);
                        calendar.set(Calendar.DAY_OF_MONTH, diaDoMes);

                        dateEntrega = calendar.getTime();

                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - EEEE", Locale.getDefault());
                        txtViewDataEntrega.setText(sdf.format(calendar.getTime()));

                    }
                }, hoje.get(Calendar.YEAR), hoje.get(Calendar.MONTH), hoje
                .get(Calendar.DAY_OF_MONTH));

        dlg.setTitle("Escolha a data da entrega");

        dlg.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem menuItem1 = menu.add(0, ITEM_GRAVAR, 0, "Gravar");
        menuItem1.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS
                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        MenuItem menuItem2 = menu.add(0, ITEM_LIMPAR, 1, "Limpar");
        menuItem2.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS
                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case ITEM_GRAVAR:
                salvarVenda();
                break;
            case ITEM_LIMPAR:
                limparFormulario();
                break;
            case android.R.id.home: // caso seja o up bottom, da um delete e continua chamando na superclasse
                listProdutosAdapter.rollback();
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void limparFormulario() {
        RadioGroup radiogroupVendedor = (RadioGroup) findViewById(R.id.radioGroupVendedor);
        radiogroupVendedor.clearCheck();

        this.dateEntrega = null;
        TextView txtViewDataEntrega = (TextView) findViewById(R.id.txtViewDataEntrega);
        txtViewDataEntrega.setText("Selecionar data ...");

        RadioGroup radioGroupFormaPagamento = (RadioGroup) findViewById(R.id.radioGroupFormaPagamento);
        radioGroupFormaPagamento.clearCheck();

        editTextCliente.setText("");
        checkboxJaPagou.setChecked(false);

        listProdutosAdapter = new ListProdutoAdapter(this);
        list.setAdapter(listProdutosAdapter);
        listProdutosAdapter.registerDataSetObserver(listProdutosObserver);

    }

    private void salvarVenda() {

        Vendedor vendedor;
        FormaPagamento formaPagamento;
        StatusVenda status;
        Cliente cliente = new Cliente();
        SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy", Locale.getDefault());

        RadioGroup radiogroupVendedor = (RadioGroup) findViewById(R.id.radioGroupVendedor);

        switch (radiogroupVendedor.getCheckedRadioButtonId()) {
            case R.id.radioVendedorAdna:
                vendedor = Vendedor.Adna;
                break;
            case R.id.radioVendedorLucas:
                vendedor = Vendedor.Lucas;
                break;
            case R.id.radioVendedorMariaClara:
                vendedor = Vendedor.MariaClara;
                break;
            default:
                Toast.makeText(this, "O Vendedor deve ser selecionado!", Toast.LENGTH_SHORT).show();
                return;
        }

        status = checkboxJaPagou.isChecked() ? StatusVenda.PagamentoRecebido : StatusVenda.AguardandoPagamento;

        if (this.dateEntrega == null) {
            String string = "A data de entrega deve ser informada!";
            Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
            return;
        }

        switch (radioGroupFormaPagamento.getCheckedRadioButtonId()) {
            case R.id.radioFormaPagamentoAVista:
                formaPagamento = FormaPagamento.AVista;
                break;
            case R.id.radioFormaPagamentoParcelado:
                formaPagamento = FormaPagamento.PagSeguro;
                break;
            default:
                String string = "A forma de pagamento deve ser informada!";
                Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
                return;
        }

        if (listProdutosAdapter.isEmpty()) {
            Toast.makeText(this, "Adicione ao menos um item!", Toast.LENGTH_SHORT).show();
            return;
        }

        String nomeCliente = editTextCliente.getText().toString();
        if (TextUtils.isEmpty(nomeCliente)) {
            Toast.makeText(this, "Informe o nome do cliente!", Toast.LENGTH_SHORT).show();
            return;
        } else {
            cliente.setNome(nomeCliente);
        }

        JSONObject obj = new JSONObject();

        try {

            JSONObject vendedorJSONObj = new JSONObject();
            vendedorJSONObj.put("id", vendedor.getId());
            obj.put("vendedor", vendedorJSONObj);

            obj.put("dataEntrega", sdf.format(this.dateEntrega));
            obj.put("formaPagamento", formaPagamento.name());
            obj.put("status", status.name());
            obj.put("turnoEntrega", TurnoEntrega.Manha); // por padrão
            obj.put("cliente", cliente.toJson());
            obj.put("carrinho", listProdutosAdapter.getItemsAsJson());


            final ProgressDialog dlg = ProgressDialog.show(this, "Salvando activity_venda", "Aguarde ...");

            new SalvarVendaAsyncTask(new OnComplete() {


                @Override
                public void run(Response response) {
                    dlg.dismiss();

                    int statusCode = response.getStatus();

                    if (statusCode == HttpURLConnection.HTTP_CREATED){
                        atualizarEstoque();

                        VendaService service = new VendaService(VendaActivity.this);
                        try {
                            service.save(new JSONObject(response.getMessage()));
                        } catch (JSONException e) {
                            // fail silently
                            e.printStackTrace();
                        }

                        Toast.makeText(getBaseContext(), "Venda salva!", Toast.LENGTH_LONG).show();
                        finish();
                    }else{
                        Toast.makeText(getBaseContext(), "Erro " + statusCode + ": " + response.getMessage(), Toast.LENGTH_LONG).show();
                    }

                }
            }).execute(obj);

        } catch (JSONException e) {
            e.printStackTrace();

        }

    }

    protected void atualizarEstoque() {
        String[] projection = {EstoqueProvider.QUANTIDADE};

        for (int i = 0; i < listProdutosAdapter.getCount(); ++i) {
            ItemVenda item = (ItemVenda) listProdutosAdapter.getItem(i);

            String selection = EstoqueProvider.PRODUTO_ID + "=? and " + EstoqueProvider.UNIDADE + "=?";
            String[] selectionArgs = {item.getProdutoID().toString(), item.getUnidade()};

            CursorLoader loader = new CursorLoader(getApplicationContext(), EstoqueProvider.CONTENT_URI, projection, selection, selectionArgs, null);

            Cursor c = loader.loadInBackground();
            c.moveToNext();

            ContentValues cv = new ContentValues();
            cv.put(EstoqueProvider.QUANTIDADE, c.getInt(c.getColumnIndex(EstoqueProvider.QUANTIDADE)) - item.getQuantidade());

            c.close();

            getContentResolver().update(EstoqueProvider.CONTENT_URI, cv, selection, selectionArgs);


        }

    }

    public void onClickSair(View v) {
        finish();
    }

}
