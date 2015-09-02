package br.com.arrasavendas.entregas;

import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import br.com.arrasavendas.R;
import br.com.arrasavendas.model.Cliente;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import br.com.arrasavendas.model.StatusVenda;
import br.com.arrasavendas.model.TurnoEntrega;
import br.com.arrasavendas.model.Venda;

public class EditClienteDialog extends DialogFragment {

    public interface ClienteDialogListener {
        public void onClienteDialogPositiveClick(View view);
    }

    private ClienteDialogListener clienteDialogListener;
    private Venda venda;
    private Spinner spinnerTurnoEntrega;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final View view = inflater.inflate(br.com.arrasavendas.R.layout.venda_cliente,
                null);

        builder.setView(view)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (clienteDialogListener != null)
                            clienteDialogListener
                                    .onClienteDialogPositiveClick(view);
                    }
                })
                .setNegativeButton("Cancelar",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                EditClienteDialog.this.getDialog().cancel();
                            }
                        });

        Cliente cliente = getVenda().getCliente();
        ((EditText) view.findViewById(R.id.editTextNome)).setText(cliente.getNome());
        ((EditText) view.findViewById(R.id.editTextDDDTelefone)).setText(cliente.getDddTelefone());
        ((EditText) view.findViewById(R.id.editTextTelefone)).setText(cliente.getTelefone());

        ((EditText) view.findViewById(R.id.editTextDDDCelular)).setText(cliente.getDddCelular());
        ((EditText) view.findViewById(R.id.editTextCelular)).setText(cliente.getCelular());

        ((EditText) view.findViewById(R.id.editTextEndereco)).setText(cliente.getEndereco());
        ((EditText) view.findViewById(R.id.editTextBairro)).setText(cliente.getBairro());

        spinnerTurnoEntrega = (Spinner) view.findViewById(R.id.spinnerTurnoEntrega);
        configurarSpinnerTurnoEntrega();

        if (getVenda().getStatus().equals(StatusVenda.PagamentoRecebido))
            ((CheckBox) view.findViewById(R.id.cbJaPagou)).setChecked(true);

        return builder.create();
    }

    private void configurarSpinnerTurnoEntrega() {
        String[] turnos = {TurnoEntrega.Manha.name(), TurnoEntrega.Tarde.name()};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_dropdown_item_1line, turnos);
        spinnerTurnoEntrega.setAdapter(adapter);

        int position = adapter.getPosition(getVenda().getTurnoEntrega().name());
        spinnerTurnoEntrega.setSelection(position);

    }

    private Venda getVenda() {
        return (Venda) getArguments().get("venda");
    }

    public void setClienteDialogListener(ClienteDialogListener clienteDialogListener) {
        this.clienteDialogListener = clienteDialogListener;
    }

}
