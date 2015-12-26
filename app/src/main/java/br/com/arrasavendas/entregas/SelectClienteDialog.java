package br.com.arrasavendas.entregas;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.LinkedList;

import br.com.arrasavendas.R;
import br.com.arrasavendas.model.Cliente;

/**
 * Created by lsimaocosta on 10/12/15.
 */
public class SelectClienteDialog extends DialogFragment {

    public static final String CLIENTES = "SelectClienteDialog.clientes";
    private SelectClienteDialogListener selectClienteDialogListener;

    public void setSelectClienteDialogListener(SelectClienteDialogListener selectClienteDialogListener) {
        this.selectClienteDialogListener = selectClienteDialogListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_cliente_search_result, null);

        Bundle bundle = getArguments();
        LinkedList<Cliente> list = (LinkedList<Cliente>) bundle.getSerializable(SelectClienteDialog.CLIENTES);
        final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radio_group);

        for (Cliente cliente : list) {
            RadioButton rb = new RadioButton(getActivity());
            rb.setTypeface(null, Typeface.BOLD);
            rb.setText(cliente.getNome());
            rb.setTag(cliente);
            radioGroup.addView(rb);

            TextView textView = new TextView(getActivity());
            textView.setText(cliente.getEndereco() + " " + cliente.getBairro());
            radioGroup.addView(textView);
        }

        builder.setView(view)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (selectClienteDialogListener != null) {
                            int radioButtonId = radioGroup.getCheckedRadioButtonId();
                            RadioButton selectedRadioButton = (RadioButton) radioGroup.findViewById(radioButtonId);
                            Cliente clienteSelecionado = (Cliente) selectedRadioButton.getTag();
                            selectClienteDialogListener.onOK(clienteSelecionado);
                        }
                    }
                })
                .setNegativeButton("Cancelar",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                SelectClienteDialog.this.getDialog().cancel();
                            }
                        }).setTitle("Resultado da Pesquisa");

        return builder.create();
    }

    public interface SelectClienteDialogListener {
        void onOK(Cliente cliente);
    }
}
