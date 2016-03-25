package br.com.arrasavendas.estoque;

import android.app.*;
import android.content.ContentValues;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

import br.com.arrasavendas.R;
import br.com.arrasavendas.model.Produto;
import br.com.arrasavendas.providers.EstoqueProvider;
import br.com.arrasavendas.service.EstoqueService;
import br.com.arrasavendas.util.Response;

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
            convertView = inflater.inflate(R.layout.list_row_detail_estoque, null);
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
            public void onSuccess(Response response) {

                try {

                    JSONObject obj =new JSONObject(response.getMessage());
                    int novaQuantidade = obj.getInt("quantidade");
                    EstoqueService service = new EstoqueService(ctx);

                    service.update(estoqueId,obj);
                    produto.updateQuantidade(unidade,novaQuantidade);
                    notifyDataSetChanged();
                    Toast.makeText(ctx, "Estoque atualizado", Toast.LENGTH_SHORT).show();

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(ctx, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

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
            convertView = inflater.inflate(R.layout.list_row_group_estoque, null);
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
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    public Map<Long, String[]> getUnidadesSelecionadas() {

        Map<Long, String[]> selecionados = new HashMap<>();

        for (int i = 0; i < produtos.size(); ++i) {
            Produto produto = produtos.get(i);
            int qtdeUnidades = produto.getUnidades().size();
            Set<String> set = new HashSet<>();

            for (int j = 0; j < qtdeUnidades; ++j) {
                if (this.unidadesSelecionadas.get(i)[j]) {
                    String unidade = produto.getUnidades().get(j);
                    set.add(unidade);
                }
            }

            if (set.size() > 0) {
                String[] unidades = new String[set.size()];
                set.toArray(unidades);
                selecionados.put(produto.getId(), unidades);
            }
        }

        return selecionados;
    }
}
