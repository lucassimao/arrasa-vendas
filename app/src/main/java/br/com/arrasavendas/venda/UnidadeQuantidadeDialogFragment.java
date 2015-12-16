package br.com.arrasavendas.venda;

import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import br.com.arrasavendas.R;
import br.com.arrasavendas.model.ItemVenda;
import br.com.arrasavendas.providers.EstoqueProvider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UnidadeQuantidadeDialogFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {


    private static final String NOME_PRODUTO = "UnidadeQuantidadeDialogFragment.produto";
    private String produto;
    private final int UNIDADES_LOADER = 1;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == UNIDADES_LOADER) {
            String[] projection = {EstoqueProvider.UNIDADE, EstoqueProvider.QUANTIDADE};
            String selection = EstoqueProvider.PRODUTO + " = ? AND " + EstoqueProvider.QUANTIDADE + " >0";
            String[] selectionArgs = {produto};

            return new CursorLoader(getActivity(), EstoqueProvider.CONTENT_URI, projection,
                    selection, selectionArgs, EstoqueProvider.UNIDADE);
        }else
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (cursor.moveToFirst()) {
            int columnIndexUnidade = cursor.getColumnIndex(EstoqueProvider.UNIDADE);
            int columnIndexQtde = cursor.getColumnIndex(EstoqueProvider.QUANTIDADE);

            do {
                String unidade = cursor.getString(columnIndexUnidade);
                int quantidade = cursor.getInt(columnIndexQtde);

                unidadesQtde.put(unidade, quantidade);
            } while (cursor.moveToNext());
        }

        ArrayAdapter<String> unidadeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,new LinkedList<>(unidadesQtde.keySet()));
        spinnerUnidade.setAdapter(unidadeAdapter);


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public interface OnAdicionarListener {

        void onClickBtnAdicionar(long idProduto, String unidade, Integer quantidade, BigDecimal precoAVista, BigDecimal precoAPrazo);
    }

    private OnAdicionarListener onAdicionarListener;
    private Map<String, Integer> unidadesQtde = new HashMap<String, Integer>();
    private Spinner spinnerUnidade;
    private Spinner spinnerQuantidade;

    public static UnidadeQuantidadeDialogFragment newInstance(String produto) {
        UnidadeQuantidadeDialogFragment dlg = new UnidadeQuantidadeDialogFragment();

        Bundle b = new Bundle();
        b.putString(UnidadeQuantidadeDialogFragment.NOME_PRODUTO, produto);
        dlg.setArguments(b);

        return dlg;
    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        final View dialog = inflater.inflate(R.layout.unidade_quantidade_dialog, container);
        this.produto = getArguments().getString(UnidadeQuantidadeDialogFragment.NOME_PRODUTO);

        getDialog().setTitle("Unidade / Quantidade");

        spinnerUnidade = (Spinner) dialog.findViewById(R.id.spinnerUnidade);
        spinnerQuantidade = (Spinner) dialog.findViewById(R.id.spinnerQuantidade);

        getLoaderManager().initLoader(UNIDADES_LOADER, null, this);


        Button btnCancelar = (Button) dialog.findViewById(R.id.btnCancelar);
        btnCancelar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                getDialog().dismiss();
            }
        });


        Button btnAdicionar = (Button) dialog.findViewById(R.id.btnAdicionar);
        btnAdicionar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (onAdicionarListener != null) {
                    String unidade = spinnerUnidade.getSelectedItem().toString();
                    Integer quantidade = Integer.valueOf(spinnerQuantidade.getSelectedItem().toString());

                    String[] projection = {EstoqueProvider.PRECO_A_VISTA, EstoqueProvider.PRECO_A_PRAZO, EstoqueProvider.PRODUTO_ID};
                    String selection = EstoqueProvider.PRODUTO + "=? AND " + EstoqueProvider.UNIDADE+"=?";
                    String[] selectionArgs = {produto,unidade};
                    Cursor c = getActivity().getContentResolver().query(EstoqueProvider.CONTENT_URI_PRODUTOS,
                            projection, selection, selectionArgs, null);

                    c.moveToFirst();
                    Long idProduto = c.getLong(c.getColumnIndex(EstoqueProvider.PRODUTO_ID));
                    // valores armazenados em centavos e sendo transformados em reais
                    BigDecimal precoAPrazo = BigDecimal.valueOf(c.getLong(c.getColumnIndex(EstoqueProvider.PRECO_A_PRAZO))/100d);
                    BigDecimal precoAVista = BigDecimal.valueOf(c.getLong(c.getColumnIndex(EstoqueProvider.PRECO_A_VISTA))/100d);
                    c.close();
                    onAdicionarListener.onClickBtnAdicionar(idProduto, unidade, quantidade, precoAVista,precoAPrazo);

                }

                getDialog().dismiss();
            }
        });

        spinnerUnidade.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                String unidadeSelecionada = spinnerUnidade.getSelectedItem().toString();
                int quantidade = unidadesQtde.get(unidadeSelecionada);

                Integer[] aux = new Integer[quantidade];
                for (int i = 0; i < quantidade; ++i) {
                    aux[i] = i + 1;
                }
                ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(getActivity(), android.R.layout.simple_list_item_1, aux);
                spinnerQuantidade.setAdapter(adapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                spinnerQuantidade.setAdapter(null);
            }
        });

        return dialog;
    }


    public void setOnAdicionarListener(OnAdicionarListener onAdicionarListener) {
        this.onAdicionarListener = onAdicionarListener;
    }


}
