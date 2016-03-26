package br.com.arrasavendas.entregas;

import java.util.Comparator;

import br.com.arrasavendas.model.TurnoEntrega;
import br.com.arrasavendas.model.Venda;

/**
 * Created by lsimaocosta on 26/03/16.
 */
public class VendaComparator implements Comparator<Venda> {
    @Override
    public int compare(Venda venda, Venda venda2) {

        if (!venda.isFlagVaiBuscar() && !venda2.isFlagVaiBuscar()) {

            if (venda.getTurnoEntrega() == venda2.getTurnoEntrega())
                return 0;
            else
                return venda.getTurnoEntrega().equals(TurnoEntrega.Tarde) ? 1 : -1;

        } else {
            return Boolean.valueOf(venda.isFlagVaiBuscar()).compareTo(venda2.isFlagVaiBuscar());
        }
    }
}
