package br.com.arrasavendas.entregas.edit;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import br.com.arrasavendas.Application;
import br.com.arrasavendas.R;
import br.com.arrasavendas.entregas.EditVendaDialog;
import br.com.arrasavendas.model.Cidade;
import br.com.arrasavendas.model.Cliente;
import br.com.arrasavendas.model.TurnoEntrega;
import br.com.arrasavendas.model.Uf;
import br.com.arrasavendas.model.Venda;
import br.com.arrasavendas.providers.CidadesProvider;

/**
 * Created by lsimaocosta on 11/02/16.
 */
public class EditDeliveryFragment extends Fragment implements EditVendaListener, LoaderManager.LoaderCallbacks<Cursor> {

    private Venda venda;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_edit_venda_delivery, container, false);
        Bundle args = getArguments();
        this.venda = (Venda) args.getSerializable(EditVendaDialog.VENDA);

        Bundle bundle = new Bundle();
        bundle.putString("uf", venda.getCliente().getUf().name());
        getActivity().getLoaderManager().initLoader(Application.CIDADES_LOADER, bundle, this);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(getClass().getName(), "onViewCreated");
        setupView();
    }

    private void setupView() {
        final View view = getView();
        if (view == null)
            return;

        Cliente cliente = venda.getCliente();

        CheckBox checkBoxVaiBuscar = (CheckBox) view.findViewById(R.id.checkbox_vai_buscar);
        final CheckBox checkboxJaPegou = (CheckBox) view.findViewById(R.id.checkbox_ja_pegou);

        checkBoxVaiBuscar.setChecked(venda.isFlagVaiBuscar());
        checkboxJaPegou.setVisibility(venda.isFlagVaiBuscar() ? View.VISIBLE : View.INVISIBLE);
        checkboxJaPegou.setChecked(venda.isFlagJaBuscou());

        checkBoxVaiBuscar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkboxJaPegou.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
            }
        });

        ((EditText) view.findViewById(R.id.editTextEndereco)).setText(cliente.getEndereco());
        ((EditText) view.findViewById(R.id.editTextBairro)).setText(cliente.getBairro());
        configurarSpinnerTurnoEntrega();
        configurarSpinnerUF();
        setupSpinnerCity();
    }

    private void setupSpinnerCity() {
        View view = getView();
        final Spinner spinnerCity = (Spinner) view.findViewById(R.id.spinner_cidade);

        CursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_dropdown_item_1line,
                null, new String[]{CidadesProvider.NOME},
                new int[]{android.R.id.text1}, 0
        );

        spinnerCity.setAdapter(adapter);
    }

    private void configurarSpinnerUF() {
        View view = getView();
        final Spinner spinnerUf = (Spinner) view.findViewById(R.id.spinner_uf);

        spinnerUf.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String uf = (String) spinnerUf.getAdapter().getItem(position);
                Bundle bundle = new Bundle();
                bundle.putString("uf", uf);
                getActivity().getLoaderManager().restartLoader(Application.CIDADES_LOADER, bundle, EditDeliveryFragment.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        Uf[] values = Uf.values();
        String[] ufs = new String[values.length];

        for (int i = 0; i < ufs.length; ++i)
            ufs[i] = values[i].name();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, ufs);

        spinnerUf.setAdapter(adapter);
        Uf uf = getVenda().getCliente().getUf();
        int position = adapter.getPosition(uf.name());
        spinnerUf.setSelection(position);
    }

    private void configurarSpinnerTurnoEntrega() {
        View view = getView();

        Spinner spTunoEntrega = (Spinner) view.findViewById(R.id.spinnerTurnoEntrega);
        String[] turnos = {TurnoEntrega.Manha.name(), TurnoEntrega.Tarde.name()};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, turnos);

        spTunoEntrega.setAdapter(adapter);
        int position = adapter.getPosition(getVenda().getTurnoEntrega().name());
        spTunoEntrega.setSelection(position);
    }

/*    @Override
    public void onPause() {
        super.onPause();
        Log.d(getClass().getName(), "onPause");
        writeChanges();
    }*/


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

        Log.d(getClass().getName(),"writing changes ....");
        Cliente cliente = venda.getCliente();

        EditText editTextEndereco = (EditText) view.findViewById(R.id.editTextEndereco);
        cliente.setEndereco(editTextEndereco.getText().toString());

        EditText editTextBairro = (EditText) view.findViewById(R.id.editTextBairro);
        cliente.setBairro(editTextBairro.getText().toString());

        Spinner spinnerTurnoEntrega = (Spinner) view.findViewById(R.id.spinnerTurnoEntrega);
        Object selectedItem = spinnerTurnoEntrega.getSelectedItem();
        TurnoEntrega turnoEntrega = TurnoEntrega.valueOf(selectedItem.toString());
        venda.setTurnoEntrega(turnoEntrega);

        venda.setFlagVaiBuscar(((CheckBox) view.findViewById(R.id.checkbox_vai_buscar)).isChecked());
        venda.setFlagJaBuscou(((CheckBox) view.findViewById(R.id.checkbox_ja_pegou)).isChecked());

        Spinner spinnerCity = (Spinner) view.findViewById(R.id.spinner_cidade);
        long idCity = spinnerCity.getSelectedItemId();
        cliente.setCidade(Cidade.fromId(idCity, getActivity()));

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case Application.CIDADES_LOADER:
                Uf uf = Uf.valueOf(args.getString("uf"));
                String selection = CidadesProvider.UF + "=?";
                return new CursorLoader(getActivity(), CidadesProvider.CONTENT_URI, null,
                        selection, new String[]{uf.name()}, CidadesProvider.NOME);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        View view = getView();
        if (view == null)
            return;

        Spinner spinnerCity = (Spinner) view.findViewById(R.id.spinner_cidade);
        CursorAdapter adapter = (CursorAdapter) spinnerCity.getAdapter();
        adapter.swapCursor(data);

        int idCity = venda.getCliente().getCidade().getId();
        // setando o item atual do spinner com o valor da cidade do cliente
        for (int i = 0; i < adapter.getCount(); ++i) {
            if (adapter.getItemId(i) == idCity) {
                spinnerCity.setSelection(i);
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}

