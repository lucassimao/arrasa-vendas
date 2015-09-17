package br.com.arrasavendas.estoque;

import android.app.*;
import android.content.ContentValues;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.*;

import br.com.arrasavendas.R;
import br.com.arrasavendas.model.Produto;
import br.com.arrasavendas.providers.EstoqueProvider;

public class EstoqueExpandableListAdapter extends BaseExpandableListAdapter {

    private final FragmentManager fragmentManager;
    private final Activity ctx;
    private List<Produto> produtos;
    private Map<Integer, Boolean[]> unidadesSelecionadas;
    private LayoutInflater inflater;

    public EstoqueExpandableListAdapter(Activity ctx, List<Produto> produtos) {
        this.ctx = ctx;
        this.fragmentManager = ctx.getFragmentManager();
        this.inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.produtos = produtos;
        this.unidadesSelecionadas = new HashMap<>();

        for (int i = 0; i < produtos.size(); ++i) {
            Produto itemEstoque = produtos.get(i);
            int qtdeDeUnidades = itemEstoque.getUnidades().size();

            Boolean[] flags = new Boolean[qtdeDeUnidades];
            Arrays.fill(flags, false);

            this.unidadesSelecionadas.put(i, flags);
        }

    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.produtos.get(groupPosition).getUnidades().get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, final ViewGroup parent) {

        final String unidade = (String) getChild(groupPosition, childPosition);
        final Produto produto = this.produtos.get(groupPosition);
        final Integer quantidade = produto.getQuantidades().get(unidade);


        if (convertView == null) {
            convertView = inflater.inflate(R.layout.estoque_list_row_details, null);
        }

        Button btnEditQuantidadeEmEstoque = (Button) convertView.findViewById(R.id.btn_edit_quantidade_em_estoque);
        btnEditQuantidadeEmEstoque.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                atualizarQuantidadeEmEstoque(produto,unidade);
            }
        });

        TextView txtUnidade = (TextView) convertView.findViewById(R.id.txtUnidade);
        txtUnidade.setText(unidade);

        TextView txtQtde = (TextView) convertView.findViewById(R.id.txtQuantidade);
        txtQtde.setText(quantidade.toString());

        CheckBox checkBoxShareUnidade = (CheckBox) convertView.findViewById(R.id.checkBoxShareUnidade);
        checkBoxShareUnidade.setChecked(unidadesSelecionadas.get(groupPosition)[childPosition]);

        checkBoxShareUnidade.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                unidadesSelecionadas.get(groupPosition)[childPosition] = isChecked;
                notifyDataSetChanged();
            }
        });

        return convertView;
    }

    private void atualizarQuantidadeEmEstoque(final Produto produto, final String unidade) {
        final int quantidadeAtual = produto.getQuantidades().get(unidade);
        final long estoqueId = produto.getEstoqueId(unidade);

        AtualizarQuantidadeEstoqueDialogFragment dialogFragment = AtualizarQuantidadeEstoqueDialogFragment.newInstance(estoqueId, unidade, quantidadeAtual);


        dialogFragment.setUpdateListener(new AtualizarQuantidadeEstoqueDialogFragment.UpdateListener() {
            @Override
            public void onSuccess(int novaQuantidade) {
                Toast.makeText(ctx, "Estoque atualizado", Toast.LENGTH_SHORT).show();

                ContentValues cv = new ContentValues();
                cv.put(EstoqueProvider.QUANTIDADE, novaQuantidade);
                ctx.getContentResolver().update(EstoqueProvider.CONTENT_URI, cv, EstoqueProvider._ID + "=?", new String[]{"" + estoqueId});

                produto.updateQuantidade(unidade,novaQuantidade);
                notifyDataSetChanged();
            }

            @Override
            public void onFail(String error) {
                Toast.makeText(ctx,error , Toast.LENGTH_SHORT).show();
            }
        });
        dialogFragment.show(this.fragmentManager,"updateQtde");
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.produtos.get(groupPosition).getUnidades().size();
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
    public View getGroupView(final int groupPosition, boolean isExpanded,
                             View convertView, final ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.estoque_list_row_group, null);
        }
        Produto produto = (Produto) getGroup(groupPosition);
        TextView textViewProduto = (TextView) convertView.findViewById(R.id.textViewNomeProduto);
        //textViewProduto.setTag();
        textViewProduto.setText(produto.getNome());

        /*
         * Por padrao o checkbox vai marcado. Em baixo ele verifica o status das unidades. Se ao menos
         * uma nao estiver selecionada ele tambem fica desmarcado
         */
        final CheckBox checkboxShareProduto = (CheckBox) convertView.findViewById(R.id.checkBoxShareProduto);
        checkboxShareProduto.setChecked(true);
        final Boolean[] flags = unidadesSelecionadas.get(groupPosition);

        for (boolean b : flags) {
            if (!b) {
                checkboxShareProduto.setChecked(false);
                break;
            }
        }

        checkboxShareProduto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean checkedState = checkboxShareProduto.isChecked();

                // marcando as unidades dos produtos com o mesmo status do produto
                for (int i = 0; i < flags.length; ++i)
                    flags[i] = checkedState;

                notifyDataSetChanged();

/*                ExpandableListView parentView = (ExpandableListView) parent;

                if (checkedState) {
                    parentView.expandGroup(groupPosition, true);
                } else {
                    parentView.collapseGroup(groupPosition);
                }*/
            }
        });

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

    public Map<Long, String[]> getUnidadesSelecionadas() {

        Map<Long, String[]> selecionados = new HashMap<>();

        for (int i = 0; i < produtos.size(); ++i) {
            Produto item = produtos.get(i);
            int qtdeUnidades = item.getUnidades().size();
            Set<String> set = new HashSet<>();

            for (int j = 0; j < qtdeUnidades; ++j) {
                String unidade = item.getUnidades().get(j);
                // so envia quem tiver marcado e tiver ao menos 1 item em estoque
                if (this.unidadesSelecionadas.get(i)[j] && produtos.get(i).getQuantidades().get(unidade) > 0) {
                    set.add(unidade);
                }
            }

            if (set.size() > 0) {
                String[] unidades = new String[set.size()];
                set.toArray(unidades);
                selecionados.put(item.getId(), unidades);
            }
        }

        return selecionados;
    }
}
