package br.com.arrasavendas.entregas.edit;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.R;
import br.com.arrasavendas.entregas.EditVendaDialog;
import br.com.arrasavendas.entregas.SelectClienteDialog;
import br.com.arrasavendas.model.Cidade;
import br.com.arrasavendas.model.Cliente;
import br.com.arrasavendas.model.Venda;
import br.com.arrasavendas.model.Vendedor;
import br.com.arrasavendas.providers.ClientesProvider;

/**
 * Created by lsimaocosta on 11/02/16.
 */
public class EditClientFragment extends Fragment implements EditVendaListener {


    private static final String TAG = EditClientFragment.class.getSimpleName();
    private Venda venda;
    private final String VENDEDOR_SITE = "Site";


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(
                R.layout.fragment_edit_venda_cliente, container, false);

        Bundle args = getArguments();
        this.venda = (Venda) args.getSerializable(EditVendaDialog.VENDA);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        configurarSpinnerVendedor(view);

        EditText editTextTelefone = (EditText) view.findViewById(R.id.editTextTelefone);
        editTextTelefone.setOnTouchListener(onTouchListener);

        EditText editTextCelular = (EditText) view.findViewById(R.id.editTextCelular);
        editTextCelular.setOnTouchListener(onTouchListener);

        setupView();
    }

    @Override
    public void setupView() {
        View view = getView();

        setCliente(venda.getCliente());

        // setando vendedor
        Vendedor vendedor = this.venda.getVendedor();
        Spinner spVendedor = (Spinner) view.findViewById(R.id.spVendedor);
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spVendedor.getAdapter();

        int position = (vendedor != null) ? adapter.getPosition(vendedor.name().toString()) :
                                            adapter.getPosition(VENDEDOR_SITE);

        spVendedor.setSelection(position);
    }

    private void setCliente(Cliente cliente) {
        venda.setCliente(cliente);
        View view = getView();

        ((EditText) view.findViewById(R.id.editTextNome)).setText(cliente.getNome());
        ((EditText) view.findViewById(R.id.editTextDDDTelefone)).setText(cliente.getDddTelefone());
        ((EditText) view.findViewById(R.id.editTextTelefone)).setText(cliente.getTelefone());
        ((EditText) view.findViewById(R.id.editTextCelular)).setText(cliente.getCelular());
        ((EditText) view.findViewById(R.id.editTextDDDCelular)).setText(cliente.getDddCelular());
    }



    private void configurarSpinnerVendedor(View view) {
        Spinner spVendedor = (Spinner) view.findViewById(R.id.spVendedor);
        TextView txtVendedor = (TextView) view.findViewById(R.id.txtVendedor);

        // importante deixar configurar mesmo que ele nao fique visivel,
        // pois no writeChanges o vendedor selecionado sera utilizado
        if (Application.getInstance().isAdmin()) {
            txtVendedor.setVisibility(View.VISIBLE);
            spVendedor.setVisibility(View.VISIBLE);
        }

        String[] vendedores = {Vendedor.Adna.name(), Vendedor.Lucas.name(),
                Vendedor.MariaClara.name(), VENDEDOR_SITE};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, vendedores);
        spVendedor.setAdapter(adapter);
    }

    public Venda getVenda() {
        return venda;
    }

    @Override
    public void writeChanges() {

        View view = getView();

        if (view == null) {
            Log.d(getClass().getName(), "getView() == null . skiping");
            return;
        }
        Log.d(getClass().getName(), "writing changes ....");
        Cliente cliente = venda.getCliente();

        EditText editTextNome = (EditText) view.findViewById(R.id.editTextNome);
        cliente.setNome(editTextNome.getText().toString());

        EditText editTextDDDTelefone = (EditText) view.findViewById(R.id.editTextDDDTelefone);
        cliente.setDddTelefone(editTextDDDTelefone.getText().toString());

        EditText editTextTelefone = (EditText) view.findViewById(R.id.editTextTelefone);
        cliente.setTelefone(editTextTelefone.getText().toString());

        EditText editTextDDDCelular = (EditText) view.findViewById(R.id.editTextDDDCelular);
        cliente.setDddCelular(editTextDDDCelular.getText().toString());

        EditText editTextCelular = (EditText) view.findViewById(R.id.editTextCelular);
        cliente.setCelular(editTextCelular.getText().toString());

        if (Application.getInstance().isAdmin()) {
            Vendedor vendedor = null;
            Spinner spVendedor = (Spinner) view.findViewById(R.id.spVendedor);
            Object selectedItem = spVendedor.getSelectedItem();

            //TODO corrigir isso aqui ... horrivel ... shame on me!
            try {

                vendedor = Vendedor.valueOf(selectedItem.toString());
            } catch (IllegalArgumentException e) {
                // se arremessar exceção significa que o item selecionado foi "Site"
                vendedor = null;
            }
            this.venda.setVendedor(vendedor);
        }

    }

    private LinkedList<Cliente> procurarCliente(String text, TipoBusca tipoBusca) {

        if (text == null || text.trim().length() == 0)
            return null;

        LinkedList<Cliente> list = null;
        ContentResolver contentResolver = getActivity().getContentResolver();
        String selection = null;

        if (tipoBusca == TipoBusca.Telefone) {
            selection = ClientesProvider.TELEFONE + " LIKE ?";
        } else {
            selection = ClientesProvider.CELULAR + " LIKE ?";
        }

        String[] selectionArgs = {"%" + text + "%"};
        Cursor cursor = contentResolver.query(ClientesProvider.CONTENT_URI, null, selection, selectionArgs, ClientesProvider.NOME);
        cursor.moveToFirst();

        if (cursor.getCount() > 0) {
            list = new LinkedList<>();

            do {

                Cliente cliente = new Cliente();
                cliente.setBairro(cursor.getString(cursor.getColumnIndex(ClientesProvider.BAIRRO)));
                cliente.setCelular(cursor.getString(cursor.getColumnIndex(ClientesProvider.CELULAR)));
                cliente.setDddCelular(cursor.getString(cursor.getColumnIndex(ClientesProvider.DDD_CELULAR)));
                cliente.setTelefone(cursor.getString(cursor.getColumnIndex(ClientesProvider.TELEFONE)));
                cliente.setDddTelefone(cursor.getString(cursor.getColumnIndex(ClientesProvider.DDD_TELEFONE)));
                cliente.setEndereco(cursor.getString(cursor.getColumnIndex(ClientesProvider.ENDERECO)));
                cliente.setNome(cursor.getString(cursor.getColumnIndex(ClientesProvider.NOME)));
                cliente.setId(cursor.getLong(cursor.getColumnIndex(ClientesProvider.CLIENTE_ID)));

                long idCity = cursor.getLong(cursor.getColumnIndex(ClientesProvider.ID_CIDADE));
                Cidade cidade = Cidade.fromId(idCity, getActivity());
                cliente.setCidade(cidade);

                list.add(cliente);

            } while (cursor.moveToNext());
        }

        cursor.close();

        return list;
    }


    private SelectClienteDialog.Listener listener = new SelectClienteDialog.Listener() {
        @Override
        public void onOK(Cliente cliente) {
            setCliente(cliente);
        }
    };

    View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        final int DRAWABLE_LEFT = 0;
        final int DRAWABLE_TOP = 1;
        final int DRAWABLE_RIGHT = 2;
        final int DRAWABLE_BOTTOM = 3;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            EditText editText = (EditText) v;

            if (event.getAction() == MotionEvent.ACTION_UP) {
                Rect bounds = editText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds();
                int leftEdgeOfRightDrawable = editText.getRight() - bounds.width();

                if (event.getRawX() >= leftEdgeOfRightDrawable) {
                    String text = editText.getText().toString();
                    TipoBusca tipoBusca = (editText.getId() == R.id.editTextTelefone) ? TipoBusca.Telefone : TipoBusca.Celular;
                    LinkedList<Cliente> list = procurarCliente(text, tipoBusca);

                    if (list != null) {
                        SelectClienteDialog dlg = SelectClienteDialog.newInstance(list);
                        dlg.setListener(listener);
                        dlg.show(getFragmentManager(), "selectClienteDialog");
                    } else
                        Toast.makeText(getActivity(), "Nenhum cliente encontrado", Toast.LENGTH_SHORT).show();


                    return true;
                }
            }
            return false;
        }
    };


}

