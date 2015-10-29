package br.com.arrasavendas.entregas;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.LinkedList;

import br.com.arrasavendas.R;
import br.com.arrasavendas.model.Cliente;
import br.com.arrasavendas.model.StatusVenda;
import br.com.arrasavendas.model.TurnoEntrega;
import br.com.arrasavendas.model.Venda;
import br.com.arrasavendas.providers.ClientesProvider;

public class EditClienteDialog extends DialogFragment {

    public static final String VENDA = "EditClienteDialog.venda";

    public interface ClienteDialogListener {
        void onPositiveClick(Cliente cliente, TurnoEntrega turnoEntrega, StatusVenda statusVenda);
    }

    private enum TipoBusca{
        Celular, Telefone;
    }

    private ClienteDialogListener clienteDialogListener;
    private Venda venda;
    private Spinner spinnerTurnoEntrega;

    private SelectClienteDialog.SelectClienteDialogListener listener = new SelectClienteDialog.SelectClienteDialogListener() {
        @Override
        public void onOK(Cliente cliente) {
            carregarCliente(cliente);
        }
    };

    View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        final int DRAWABLE_LEFT = 0;
        final int DRAWABLE_TOP = 1;
        final int DRAWABLE_RIGHT = 2;
        final int DRAWABLE_BOTTOM = 3;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            EditText editText = (EditText) v;

            if (event.getAction() == MotionEvent.ACTION_UP) {
                Rect bounds = editText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds();
                int leftEdgeOfRightDrawable = editText.getRight() - bounds.width();

                if (event.getRawX() >= leftEdgeOfRightDrawable) {
                    String text = editText.getText().toString();
                    TipoBusca tipoBusca = (editText.getId() == R.id.editTextTelefone)?TipoBusca.Telefone:TipoBusca.Celular;
                    LinkedList<Cliente> list = procurarCliente(text, tipoBusca);

                    if (list != null) {
                        SelectClienteDialog dlg = new SelectClienteDialog();
                        dlg.setSelectClienteDialogListener(listener);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable(SelectClienteDialog.CLIENTES, list);
                        dlg.setArguments(bundle);

                        dlg.show(getFragmentManager(), "selectClienteDialog");
                    }else
                        Toast.makeText(getActivity(), "Nenhum cliente encontrado", Toast.LENGTH_SHORT).show();



                    return true;
                }
            }
            return false;
        }
    };


    private void carregarCliente(Cliente cliente) {

        Dialog view = getDialog();

        ((EditText) view.findViewById(R.id.editTextNome)).setText(cliente.getNome());
        ((EditText) view.findViewById(R.id.editTextDDDTelefone)).setText(cliente.getDddTelefone());
        ((EditText) view.findViewById(R.id.editTextTelefone)).setText(cliente.getTelefone());
        ((EditText) view.findViewById(R.id.editTextCelular)).setText(cliente.getCelular());
        ((EditText) view.findViewById(R.id.editTextDDDCelular)).setText(cliente.getDddCelular());
        ((EditText) view.findViewById(R.id.editTextEndereco)).setText(cliente.getEndereco());
        ((EditText) view.findViewById(R.id.editTextBairro)).setText(cliente.getBairro());

    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final View view = inflater.inflate(R.layout.venda_cliente,null);

        builder.setView(view).setTitle("Editar Cliente")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (clienteDialogListener != null) {

                            final Cliente updatedCliente = getUpdatedCliente();

                            Spinner spinnerTurnoEntrega = (Spinner) view.findViewById(R.id.spinnerTurnoEntrega);
                            TurnoEntrega turnoEntrega = TurnoEntrega.valueOf(spinnerTurnoEntrega.getSelectedItem().toString());

                            CheckBox checkBoxJaPagou = (CheckBox) view.findViewById(R.id.cbJaPagou);
                            StatusVenda statusVenda = (checkBoxJaPagou.isChecked()) ? StatusVenda.PagamentoRecebido : StatusVenda.AguardandoPagamento;

                            clienteDialogListener.onPositiveClick(updatedCliente, turnoEntrega, statusVenda);
                        }
                    }
                })
                .setNegativeButton("Cancelar",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                EditClienteDialog.this.getDialog().cancel();
                            }
                        });



        EditText editTextTelefone = (EditText) view.findViewById(R.id.editTextTelefone);
        editTextTelefone.setOnTouchListener(onTouchListener);

        EditText editTextCelular = (EditText) view.findViewById(R.id.editTextCelular);
        editTextCelular.setOnTouchListener(onTouchListener);

        spinnerTurnoEntrega = (Spinner) view.findViewById(R.id.spinnerTurnoEntrega);
        configurarSpinnerTurnoEntrega();


        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        carregarCliente(getVenda().getCliente());
        if (getVenda().getStatus().equals(StatusVenda.PagamentoRecebido))
            ((CheckBox) getDialog().findViewById(R.id.cbJaPagou)).setChecked(true);
    }

    private LinkedList<Cliente> procurarCliente(String text, TipoBusca tipoBusca) {

        if ( text == null || text.trim().length() == 0)
            return null;

        LinkedList<Cliente> list = null;
        ContentResolver contentResolver = getActivity().getContentResolver();
        String selection = null;

        if (tipoBusca == TipoBusca.Telefone ) {
            selection = ClientesProvider.TELEFONE + " LIKE ?";
        }else{
            selection = ClientesProvider.CELULAR + " LIKE ?";
        }

        String[] selectionArgs = {"%" + text + "%"};
        Cursor cursor = contentResolver.query(ClientesProvider.CONTENT_URI, null,selection, selectionArgs, ClientesProvider.NOME);
        cursor.moveToFirst();

        if(cursor.getCount()>0) {
            list =  new LinkedList<>();

            do {

                Cliente cliente = new Cliente();
                cliente.setBairro(cursor.getString(cursor.getColumnIndex(ClientesProvider.BAIRRO)));
                cliente.setCelular(cursor.getString(cursor.getColumnIndex(ClientesProvider.CELULAR)));
                cliente.setDddCelular(cursor.getString(cursor.getColumnIndex(ClientesProvider.DDD_CELULAR)));
                cliente.setTelefone(cursor.getString(cursor.getColumnIndex(ClientesProvider.TELEFONE)));
                cliente.setDddTelefone(cursor.getString(cursor.getColumnIndex(ClientesProvider.DDD_TELEFONE)));
                cliente.setEndereco(cursor.getString(cursor.getColumnIndex(ClientesProvider.ENDERECO)));
                cliente.setNome(cursor.getString(cursor.getColumnIndex(ClientesProvider.NOME)));
                cliente.setId(cursor.getLong(cursor.getColumnIndex(ClientesProvider.CLIENTE_ID)));

                list.add(cliente);

            } while (cursor.moveToNext());
        }

        cursor.close();

        return list;
    }

    private void configurarSpinnerTurnoEntrega() {
        String[] turnos = {TurnoEntrega.Manha.name(), TurnoEntrega.Tarde.name()};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_dropdown_item_1line, turnos);
        spinnerTurnoEntrega.setAdapter(adapter);

        int position = adapter.getPosition(getVenda().getTurnoEntrega().name());
        spinnerTurnoEntrega.setSelection(position);

    }

    private Venda getVenda() {
        return (Venda) getArguments().get(EditClienteDialog.VENDA);
    }

    public void setClienteDialogListener(ClienteDialogListener clienteDialogListener) {
        this.clienteDialogListener = clienteDialogListener;
    }

    private Cliente getUpdatedCliente() {
        Dialog dialog = getDialog();
        Cliente updatedCliente = new Cliente();

        EditText editTextNome = (EditText) dialog.findViewById(R.id.editTextNome);
        updatedCliente.setNome(editTextNome.getText().toString());

        EditText editTextDDDTelefone = (EditText) dialog.findViewById(R.id.editTextDDDTelefone);
        updatedCliente.setDddTelefone(editTextDDDTelefone.getText().toString());

        EditText editTextTelefone = (EditText) dialog.findViewById(R.id.editTextTelefone);
        updatedCliente.setTelefone(editTextTelefone.getText().toString());

        EditText editTextDDDCelular = (EditText) dialog.findViewById(R.id.editTextDDDCelular);
        updatedCliente.setDddCelular(editTextDDDCelular.getText().toString());

        EditText editTextCelular = (EditText) dialog.findViewById(R.id.editTextCelular);
        updatedCliente.setCelular(editTextCelular.getText().toString());

        EditText editTextEndereco = (EditText) dialog.findViewById(R.id.editTextEndereco);
        updatedCliente.setEndereco(editTextEndereco.getText().toString());

        EditText editTextBairro = (EditText) dialog.findViewById(R.id.editTextBairro);
        updatedCliente.setBairro(editTextBairro.getText().toString());

        return updatedCliente;
    }

}
