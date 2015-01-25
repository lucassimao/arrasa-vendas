package br.com.arrasavendas.estoque;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;

import br.com.arrasavendas.DownloadJSONFeedTask;
import br.com.arrasavendas.RemotePath;
import br.com.arrasavendas.providers.EstoqueProvider;
import br.com.arrasavendas.R;

public class EstoqueActivity extends Activity {

	private EstoqueExpandableListAdapter estoqueListAdapter;
	private ExpandableListView list;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.estoque_expandable);

		list = (ExpandableListView) findViewById(R.id.listItemsEstoque);

		List<ItemEstoque> produtos = getData();
		estoqueListAdapter = new EstoqueExpandableListAdapter(this, produtos);
		list.setAdapter(estoqueListAdapter);

	}

	private List<ItemEstoque> getData() {

		CursorLoader loader = new CursorLoader(getApplicationContext(),
				EstoqueProvider.CONTENT_URI_PRODUTOS, null, null, null, null);
		
		Cursor cursor = loader.loadInBackground();

		List<ItemEstoque> estoque = new LinkedList<ItemEstoque>();

		if (cursor.moveToFirst()) {

			do {

				String nomeProduto = cursor.getString(cursor.getColumnIndex(EstoqueProvider.PRODUTO));
				ItemEstoque itemEstoque = new ItemEstoque(nomeProduto);
				CursorLoader unidadesLoader = new CursorLoader(getApplicationContext(),
						EstoqueProvider.CONTENT_URI, new String[]{EstoqueProvider.UNIDADE,EstoqueProvider.QUANTIDADE}, 
						EstoqueProvider.PRODUTO + " = ?", new String[]{itemEstoque.nome}, EstoqueProvider.UNIDADE);	
				
				Cursor cursor2 = unidadesLoader.loadInBackground();
				
				while(cursor2.moveToNext()){
					String unidade = cursor2.getString(cursor2.getColumnIndex(EstoqueProvider.UNIDADE));
					int qtde = cursor2.getInt(cursor2.getColumnIndex(EstoqueProvider.QUANTIDADE));
					itemEstoque.addUnidade(unidade,qtde);
					
				}	
				cursor2.close();
				estoque.add(itemEstoque);
				
			} while (cursor.moveToNext());
		}
		cursor.close();

		return estoque;
	}

	public void onClickBtnSincronizar(View v) {

		final ProgressDialog progressDlg = ProgressDialog.show(this,
				"Atualizando informações", "Aguarde ...");
		new DownloadJSONFeedTask(RemotePath.EstoqueList, this, new Runnable() {

			@Override
			public void run() {
				progressDlg.dismiss();

				List<ItemEstoque> produtos = getData();
				estoqueListAdapter = new EstoqueExpandableListAdapter(getBaseContext(), produtos);
				list.setAdapter(estoqueListAdapter);

			}
		}).execute();
	}

	public void onClickSair(View v) {
		finish();
	}

}
