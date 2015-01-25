package br.com.arrasavendas.venda;

import android.app.DialogFragment;
import android.content.CursorLoader;
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
import br.com.arrasavendas.providers.EstoqueProvider;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UnidadeQuantidadeDialogFragment extends DialogFragment {
	

	interface OnAdicionarListener{

		public void onClickBtnAdicionar(Integer idProduto, String unidade, Integer quantidade);
	}
	
	private OnAdicionarListener onAdicionarListener;
	private Map<String, Integer> unidadesQtde = new HashMap<String, Integer>();
	private Spinner spinnerUnidade;
	private Spinner spinnerQuantidade;
	
	public static UnidadeQuantidadeDialogFragment newInstance(String produto){
		UnidadeQuantidadeDialogFragment dlg = new UnidadeQuantidadeDialogFragment();
		
		Bundle b = new Bundle();
		b.putString("produto", produto);
		dlg.setArguments(b);
		
		return dlg;
	}
	
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		
		
		final View dialog =  inflater.inflate(R.layout.unidade_quantidade_dialog, container);
		String produto = getArguments().getString("produto");
		
		getDialog().setTitle("Unidade / Quantidade");

		spinnerUnidade = (Spinner) dialog.findViewById(R.id.spinnerUnidade);
		spinnerQuantidade = (Spinner) dialog.findViewById(R.id.spinnerQuantidade);

        String[] projection = {EstoqueProvider.UNIDADE, EstoqueProvider.QUANTIDADE,EstoqueProvider.PRODUTO_ID};
        String selection = EstoqueProvider.PRODUTO + " = ? AND " + EstoqueProvider.QUANTIDADE + " >0";
        String[] selectionArgs = {produto};

        CursorLoader loader = new CursorLoader(dialog.getContext(), EstoqueProvider.CONTENT_URI, projection,
                selection, selectionArgs, EstoqueProvider.UNIDADE);
		
		Cursor cursor = loader.loadInBackground();
		
		List<String> unidades = new LinkedList<String>();
		
		if (cursor.moveToFirst()){
			do{
				int columnIndexUnidade = cursor.getColumnIndex(EstoqueProvider.UNIDADE);
				int columnIndexQtde = cursor.getColumnIndex(EstoqueProvider.QUANTIDADE);
				
				String unidade = cursor.getString(columnIndexUnidade);
				
				unidades.add(unidade);
				unidadesQtde.put(unidade, cursor.getInt(columnIndexQtde));	

			
			}while(cursor.moveToNext());
		}
		
		ArrayAdapter<String> unidadeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, unidades);
		spinnerUnidade.setAdapter(unidadeAdapter);
		
		spinnerUnidade.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,	int arg2, long arg3) {
				String unidadeSelecionada = spinnerUnidade.getSelectedItem().toString();
				int quantidade = unidadesQtde.get(unidadeSelecionada);
				
				Integer[] aux = new Integer[quantidade];
				for(int i=0;i< quantidade;++i){
					aux[i] = i+1;
				}
				ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(getActivity(), android.R.layout.simple_list_item_1, aux);
				spinnerQuantidade.setAdapter(adapter);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				spinnerQuantidade.setAdapter(null);
			}
		});
		
		
		
		
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
				
				if (onAdicionarListener!=null){
					String unidade = spinnerUnidade.getSelectedItem().toString();
					Integer quantidade = Integer.valueOf(spinnerQuantidade.getSelectedItem().toString());

                    Integer idProduto = 0;
                    onAdicionarListener.onClickBtnAdicionar(idProduto, unidade, quantidade);
				}
				
				getDialog().dismiss();
			}
		});
		
		return dialog;
	};
	
	
	
	
	public void setOnAdicionarListener(OnAdicionarListener onAdicionarListener) {
		this.onAdicionarListener = onAdicionarListener;
	}




	
}
