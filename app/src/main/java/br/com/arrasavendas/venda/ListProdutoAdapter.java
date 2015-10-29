package br.com.arrasavendas.venda;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import br.com.arrasavendas.R;
import br.com.arrasavendas.model.ItemVenda;
import br.com.arrasavendas.providers.EstoqueProvider;

public class ListProdutoAdapter extends BaseAdapter {

    private List<ItemVenda> itens;
    private Context ctx;
    private ArrayList<ContentProviderOperation> rollback = new ArrayList<>();
    String selection = EstoqueProvider.UNIDADE + "=? AND " + EstoqueProvider.PRODUTO_ID + "=?";


    public ListProdutoAdapter(Context context) {
        this.ctx = context;
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
        LayoutInflater inflater = (LayoutInflater) this.ctx
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

        ImageButton btnExcluir = (ImageButton) rowView.findViewById(R.id.btnExcluirProduto);
        btnExcluir.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                int position = (Integer) rowView.getTag();
                ItemVenda itemRemovido = itens.remove(position);
                notifyDataSetChanged();

                int quantidadeEmEstoque = getQuantidadeEmEstoque(itemRemovido.getProdutoID(), itemRemovido.getUnidade());

                String[] selectionArgs = { itemRemovido.getUnidade(), itemRemovido.getProdutoID().toString()};
                ContentValues cv = new ContentValues();
                cv.put(EstoqueProvider.QUANTIDADE, quantidadeEmEstoque + itemRemovido.getQuantidade());
                ctx.getContentResolver().update(EstoqueProvider.CONTENT_URI, cv, selection, selectionArgs);

            }
        });

        return rowView;
    }

    public void add(Long idProduto, String nomeProduto, String unidade, Integer quantidade, BigDecimal precoAVista,  BigDecimal precoAPrazo ) {

        ItemVenda itemVenda = null;

        for(ItemVenda item: itens){
            if (item.getProdutoID().equals(idProduto) && item.getUnidade().equals(unidade)){
                itemVenda = item;
                break;
            }
        }

        if (itemVenda == null) {
            itemVenda = new ItemVenda(null, idProduto, nomeProduto, unidade, quantidade, precoAVista, precoAPrazo);
            this.itens.add(itemVenda);
            criarRollback(itemVenda);
        }else{
            int novaQtde = itemVenda.getQuantidade() + quantidade;
            itemVenda.setQuantidade(novaQtde);
        }

        int quantidadeEmEstoque = getQuantidadeEmEstoque(idProduto, unidade);

        String[] selectionArgs = {unidade, idProduto.toString()};
        ContentValues cv = new ContentValues();
        cv.put(EstoqueProvider.QUANTIDADE, quantidadeEmEstoque - quantidade);
        ctx.getContentResolver().update(EstoqueProvider.CONTENT_URI, cv, selection, selectionArgs);

    }

    public List<ItemVenda> getItens() {
        return itens;
    }

    public void rollback() {
        try {
            ContentProviderResult[] contentProviderResults = ctx.getContentResolver().applyBatch(EstoqueProvider.AUTHORITHY, rollback);
            for (ContentProviderResult result : contentProviderResults) {
                if (result.count != 1)
                    throw new Exception("Erro ao fazer rollback para " + result.uri.toString());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final void criarRollback(ItemVenda itemVenda) {

        int quantidadeEmEstoque = getQuantidadeEmEstoque(itemVenda.getProdutoID(), itemVenda.getUnidade());
        String[] selectionArgs = {itemVenda.getUnidade(), itemVenda.getProdutoID().toString()};

        rollback.add(ContentProviderOperation.
                newUpdate(EstoqueProvider.CONTENT_URI).
                withSelection(selection,
                        selectionArgs).withValue(EstoqueProvider.QUANTIDADE, quantidadeEmEstoque)
                .build());
    }

    public final int getQuantidadeEmEstoque(Long produtoId, String unidade) {

        String[] projection = {EstoqueProvider.QUANTIDADE};
        String selection = EstoqueProvider.PRODUTO_ID + " =? AND " + EstoqueProvider.UNIDADE + "=?";
        String[] selectionArgs = {produtoId.toString(), unidade};
        Cursor cursor = ctx.getContentResolver().query(EstoqueProvider.CONTENT_URI, projection, selection, selectionArgs, null);
        int columnIndex = cursor.getColumnIndex(EstoqueProvider.QUANTIDADE);

        cursor.moveToFirst();
        int qtde = cursor.getInt(columnIndex);
        cursor.close();
        return qtde;
    }

}
