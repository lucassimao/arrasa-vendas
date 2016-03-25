package br.com.arrasavendas.entregas;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;

import br.com.arrasavendas.R;
import br.com.arrasavendas.model.Cliente;

/**
 * Created by lsimaocosta on 10/12/15.
 */
public class SelectClienteDialog extends DialogFragment {

    public static final String CLIENTES = "SelectClienteDialog.clientes";
    private Listener listener;


    public static SelectClienteDialog newInstance(LinkedList<Cliente> list){
        SelectClienteDialog dlg = new SelectClienteDialog();
        Bundle bundle = new Bundle();
        bundle.putSerializable(SelectClienteDialog.CLIENTES, list);
        dlg.setArguments(bundle);
        return dlg;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
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
                        if (listener != null) {
                            int radioButtonId = radioGroup.getCheckedRadioButtonId();

                            if (radioButtonId != -1) {
                                RadioButton selectedRadioButton = (RadioButton) radioGroup.findViewById(radioButtonId);
                                Cliente clienteSelecionado = (Cliente) selectedRadioButton.getTag();
                                listener.onOK(clienteSelecionado);
                            }else
                                Toast.makeText(getActivity(),"Selecione um dos endereços",Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancelar",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                getDialog().cancel();
                            }
                        }).setTitle("Resultado da Pesquisa");

        return builder.create();
    }

    public interface Listener {
        void onOK(Cliente cliente);
    }
}
