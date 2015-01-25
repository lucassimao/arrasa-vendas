package br.com.arrasavendas.estoque;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;
import br.com.arrasavendas.R;

public class EstoqueExpandableListAdapter extends BaseExpandableListAdapter {

	private List<ItemEstoque> produtos;
	private LayoutInflater inflater;

	public EstoqueExpandableListAdapter(Context ctx, List<ItemEstoque> produtos) {
		this.inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.produtos = produtos;

	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return this.produtos.get(groupPosition).unidades.get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {

		final String unidade = (String) getChild(groupPosition, childPosition);

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.estoque_list_row_details,null);
		}

		TextView txtUnidade = (TextView) convertView.findViewById(R.id.txtUnidade);
		txtUnidade.setText(unidade);

		TextView txtQtde = (TextView) convertView.findViewById(R.id.txtQuantidade);
		ItemEstoque produto = this.produtos.get(groupPosition);
		txtQtde.setText(produto.quantidades.get(unidade).toString());

		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return this.produtos.get(groupPosition).unidades.size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return this.produtos.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return this.produtos.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.estoque_list_row_group, null);
		}
		ItemEstoque produto = (ItemEstoque) getGroup(groupPosition);
		((CheckedTextView) convertView).setText(produto.nome);
		((CheckedTextView) convertView).setChecked(isExpanded);
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return false;
	}

}
