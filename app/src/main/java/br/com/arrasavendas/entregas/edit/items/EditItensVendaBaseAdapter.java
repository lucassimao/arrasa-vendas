package br.com.arrasavendas.entregas.edit.items;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import br.com.arrasavendas.R;
import br.com.arrasavendas.Utilities;
import br.com.arrasavendas.model.ItemVenda;
import br.com.arrasavendas.model.Venda;
import br.com.arrasavendas.providers.EstoqueProvider;

/**
 * Created by lsimaocosta on 21/10/15.
 */
public class EditItensVendaBaseAdapter extends BaseAdapter {


    private final List<ItemVenda> itens;
    private final LayoutInflater inflater;
    private final Context ctx;
    private final Venda venda;
    ArrayList<ContentProviderOperation> rollback = new ArrayList<>();
    String selection = EstoqueProvider.UNIDADE + "=? AND " + EstoqueProvider.PRODUTO_ID + "=?";


    public EditItensVendaBaseAdapter(Context ctx, Venda venda) {
        this.venda = venda;
        this.itens = venda.getItens();
        this.ctx = ctx;
        this.inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        criarRollbacks();
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


    private void criarRollbacks() {
        for (ItemVenda itemVenda : itens) {
            criarRollback(itemVenda);
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

    @Override
    public int getCount() {
        return this.itens.size();
    }

    @Override
    public Object getItem(int position) {
        return this.itens.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_row_edit_itens_venda, null);
        }

        final ItemVenda itemVenda = this.itens.get(position);

        ImageButton btnExcluirProduto = (ImageButton) convertView.findViewById(R.id.btn_excluir_produto);
        btnExcluirProduto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String[] selectionArgs = {itemVenda.getUnidade(), itemVenda.getProdutoID().toString()};
                int quantidadeEmEstoque = getQuantidadeEmEstoque(itemVenda.getProdutoID(), itemVenda.getUnidade());

                ContentValues cv = new ContentValues();
                cv.put(EstoqueProvider.QUANTIDADE, quantidadeEmEstoque + itemVenda.getQuantidade());
                ctx.getContentResolver().update(EstoqueProvider.CONTENT_URI, cv, selection, selectionArgs);

                EditItensVendaBaseAdapter.this.itens.remove(position);
                notifyDataSetChanged();
            }
        });

        TextView txtItem = (TextView) convertView.findViewById(R.id.txtItem);
        txtItem.setText(itemVenda.getNomeProduto());

        TextView txtUnidade = (TextView) convertView.findViewById(R.id.txtUnidade);
        txtUnidade.setText(itemVenda.getUnidade());

        int quantidadeEmEstoque = getQuantidadeEmEstoque(itemVenda.getProdutoID(), itemVenda.getUnidade());
        final int max = itemVenda.getQuantidade() + quantidadeEmEstoque;

        final Spinner spinneQuantidade = (Spinner) convertView.findViewById(R.id.spinner_quantidade);
        spinneQuantidade.setAdapter(new ArrayAdapter<Integer>(ctx, android.R.layout.simple_spinner_item, Utilities.interval(1, max)));
        spinneQuantidade.setSelection(itemVenda.getQuantidade() - 1);

        spinneQuantidade.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                Integer newQtde = (Integer) parent.getSelectedItem();
                int newQuantidadeEmEstoque = max - newQtde;

                String[] selectionArgs = {itemVenda.getUnidade(), itemVenda.getProdutoID().toString()};
                ContentValues cv = new ContentValues();
                cv.put(EstoqueProvider.QUANTIDADE, newQuantidadeEmEstoque);
                ctx.getContentResolver().update(EstoqueProvider.CONTENT_URI, cv, selection, selectionArgs);

                itemVenda.setQuantidade(newQtde);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        return convertView;
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


    public void addToItens(ItemVenda itemVenda) {

        if (!venda.contemItem(itemVenda.getProdutoID(), itemVenda.getUnidade())){
            criarRollback(itemVenda);
        }
        venda.addToItens(itemVenda);

        int quantidadeEmEstoque = getQuantidadeEmEstoque(itemVenda.getProdutoID(), itemVenda.getUnidade());
        String[] selectionArgs = {itemVenda.getUnidade(), itemVenda.getProdutoID().toString()};

        ContentValues cv = new ContentValues();
        cv.put(EstoqueProvider.QUANTIDADE, quantidadeEmEstoque - itemVenda.getQuantidade());
        ctx.getContentResolver().update(EstoqueProvider.CONTENT_URI, cv, selection, selectionArgs);

        notifyDataSetChanged();

    }
}
