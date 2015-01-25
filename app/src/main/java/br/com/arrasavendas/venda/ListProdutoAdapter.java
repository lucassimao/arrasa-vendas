package br.com.arrasavendas.venda;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import br.com.arrasavendas.estoque.EstoqueExpandableListAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import br.com.arrasavendas.R;
import br.com.arrasavendas.providers.EstoqueProvider;

public class ListProdutoAdapter extends BaseAdapter {

    private List<ItemVenda> itens;
    private Context context;

    public ListProdutoAdapter(Context context) {
        this.context = context;
        itens = new LinkedList<ItemVenda>();
    }

    @Override
    public int getCount() {
        return itens.size();
    }

    @Override
    public Object getItem(int position) {
        return this.itens.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public JSONObject getItemsAsJson() {

        JSONObject obj = new JSONObject();

        try {

            JSONArray array = new JSONArray();
            for (ItemVenda item : itens) {

                JSONObject jsonObj = item.asJsonObject();
                array.put(jsonObj);

            }

            obj.put("itens", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) this.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View rowView = inflater.inflate(R.layout.venda_list_row, parent,
                false);
        String produto = itens.get(position).getNomeProduto();
        String unidade = itens.get(position).getUnidade().toString();
        String quantidade = String.valueOf(itens.get(position).getQuantidade());

        TextView txtItem = (TextView) rowView.findViewById(R.id.txtItem);
        txtItem.setText(produto);

        TextView txtUnidade = (TextView) rowView.findViewById(R.id.txtUnidade);
        txtUnidade.setText(unidade);

        TextView txtQuantidade = (TextView) rowView.findViewById(R.id.txtQtde);
        txtQuantidade.setText(quantidade);

        rowView.setTag(position);

        Button btnExcluir = (Button) rowView.findViewById(R.id.btnExcluirProduto);
        btnExcluir.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                int position = (Integer) rowView.getTag();
                itens.remove(position);
                notifyDataSetChanged();
            }
        });

        return rowView;
    }

    public void add(Integer idProduto, String nomeProduto, String unidade, Integer quantidade) {
        String[] projection = {EstoqueProvider.PRECO_A_VISTA, EstoqueProvider.PRECO_A_PRAZO, EstoqueProvider.PRODUTO_ID};
        String selection = EstoqueProvider.PRODUTO +"=? and " + EstoqueProvider.UNIDADE + " =?";
        String[] selectionArgs = {nomeProduto, unidade};

        CursorLoader loader = new CursorLoader(context, EstoqueProvider.CONTENT_URI, projection, selection, selectionArgs, null);

        Cursor c = loader.loadInBackground();
        c.moveToNext();

        BigDecimal precoAVista = new BigDecimal(c.getString(c.getColumnIndex(EstoqueProvider.PRECO_A_VISTA)));
        precoAVista = precoAVista.divide(BigDecimal.valueOf(100));

        BigDecimal precoAPrazo = new BigDecimal(c.getString(c.getColumnIndex(EstoqueProvider.PRECO_A_PRAZO)));
        precoAPrazo = precoAPrazo.divide(BigDecimal.valueOf(100));

        ItemVenda itemVenda = new ItemVenda(c.getLong(c.getColumnIndex(EstoqueProvider.PRODUTO_ID)), nomeProduto, unidade, quantidade, precoAVista, precoAPrazo);
        this.itens.add(itemVenda);

        c.close();

    }

}
