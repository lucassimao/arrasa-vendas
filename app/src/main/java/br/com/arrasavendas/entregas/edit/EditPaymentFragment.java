package br.com.arrasavendas.entregas.edit;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.math.BigDecimal;

import br.com.arrasavendas.R;
import br.com.arrasavendas.entregas.EditVendaDialog;
import br.com.arrasavendas.model.FormaPagamento;
import br.com.arrasavendas.model.StatusVenda;
import br.com.arrasavendas.model.Venda;

/**
 * Created by lsimaocosta on 11/02/16.
 * <p/>
 * Não é necesário implementar setUserVisibleHint
 * para atualizar a view quando a mesma exebida pois as informações desse fragment
 * nao sao alteradas por outra aba
 */
public class EditPaymentFragment extends Fragment implements EditVendaListener {

    private Venda venda;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(
                R.layout.fragment_edit_venda_payment, container, false);

        Bundle args = getArguments();
        this.venda = (Venda) args.getSerializable(EditVendaDialog.VENDA);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (venda.getStatus().equals(StatusVenda.PagamentoRecebido))
            ((CheckBox) view.findViewById(R.id.cbJaPagou)).setChecked(true);

        switch(venda.getFormaDePagamento()){
            case AVista:
                ((RadioButton) view.findViewById(R.id.radioFormaPagamentoAVista)).setChecked(true);
                break;
            case PagSeguro:
                ((RadioButton) view.findViewById(R.id.radioFormaPagamentoParcelado)).setChecked(true);
                break;
        }

        ((EditText)view.findViewById(R.id.edit_text_abatimento)).setText(String.format("%.2f",venda.getAbatimentoEmReais()));
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(getClass().getName(),"onPause");
        writeChanges();
    }


    @Override
    public void writeChanges() {

        View view = getView();

        if (view == null) {
            Log.d(getClass().getName(),"getView() == null . skiping");
            return;
        }

        CheckBox checkBoxJaPagou = (CheckBox) view.findViewById(R.id.cbJaPagou);
        StatusVenda statusVenda = (checkBoxJaPagou.isChecked()) ? StatusVenda.PagamentoRecebido :
                StatusVenda.AguardandoPagamento;
        venda.setStatus(statusVenda);

        RadioGroup rg = (RadioGroup) view.findViewById(R.id.radioGroupFormaPagamento);
        switch(rg.getCheckedRadioButtonId()){
            case R.id.radioFormaPagamentoAVista:
                venda.setFormaDePagamento(FormaPagamento.AVista);
                break;
            case R.id.radioFormaPagamentoParcelado:
                venda.setFormaDePagamento(FormaPagamento.PagSeguro);
                break;
        }

        EditText editText = (EditText) view.findViewById(R.id.edit_text_abatimento);
        String string = editText.getText().toString().replace(",",".");
        BigDecimal _100 = BigDecimal.valueOf(100);
        int abatimentoEmCentavos = new BigDecimal(string).multiply(_100).intValue();
        venda.setAbatimentoEmCentavos(abatimentoEmCentavos);


    }
}

