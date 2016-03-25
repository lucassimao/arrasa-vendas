package br.com.arrasavendas.entregas;

import android.content.res.Resources;
import android.graphics.Color;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import br.com.arrasavendas.R;
import br.com.arrasavendas.model.Venda;

/**
 * Created by lsimaocosta on 18/12/15.
 */
class EntregasActionBarCallback implements ActionMode.Callback {

    private final EntregasActivity entregasActivity;

    public EntregasActionBarCallback(EntregasActivity entregasActivity) {
        this.entregasActivity = entregasActivity;
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        actionMode.getMenuInflater().inflate(R.menu.entregas_contextual_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
//            actionMode.setTitle("sction mode title");
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.edit_address:
                entregasActivity.showEditVendaDialog();
                break;
            case R.id.update_data_entrega:
                entregasActivity.updateDataEntrega();
                break;
            case R.id.delete:
                entregasActivity.excluirVenda();
                break;
            case R.id.shopping_cart:
                entregasActivity.editarItensVenda();
                break;
            case R.id.add_anexo:
                entregasActivity.adicionarAnexo();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {

        View view = entregasActivity.vendaSelecionadaView;
        Venda venda = entregasActivity.vendaSelecionada;

        restoreViewBackground(view, venda);

        entregasActivity.vendaSelecionada = null;
        entregasActivity.vendaSelecionadaView = null;
    }

    private void restoreViewBackground(View view, Venda venda) {
        if (venda.getVendedor() == null)
            view.setBackgroundColor(Color.WHITE);
        else {
            Resources resources = entregasActivity.getResources();

            switch (venda.getVendedor()) {
                case Lucas:
                    view.setBackgroundColor(resources.getColor(R.color.blueLucas));
                    break;
                case Adna:
                    view.setBackgroundColor(resources.getColor(R.color.pinkAdna));
                    break;
                case MariaClara:
                    view.setBackgroundColor(resources.getColor(R.color.amareloMClara));
                    break;
                default:

            }
        }

    }
}
