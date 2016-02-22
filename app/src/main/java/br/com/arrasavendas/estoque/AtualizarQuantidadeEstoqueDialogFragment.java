package br.com.arrasavendas.estoque;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import br.com.arrasavendas.R;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lsimaocosta on 12/09/15.
 */
public class AtualizarQuantidadeEstoqueDialogFragment extends DialogFragment {

    private TextView txtUnidade;
    private EditText editQuantidade;
    private Map<String, Integer> unidadesQtde;
    private UpdateListener updateListener;


    interface UpdateListener{
        void onSuccess(int novaQuantidade);
        void onFail(String error);
    }

    public static AtualizarQuantidadeEstoqueDialogFragment newInstance(long estoqueId, String unidade, int quantidadeEmEstoque) {
        AtualizarQuantidadeEstoqueDialogFragment dlg = new AtualizarQuantidadeEstoqueDialogFragment();

        Bundle b = new Bundle();
        b.putLong("estoque_id", estoqueId);
        b.putString("unidade", unidade);
        b.putInt("quantidade_em_estoque", quantidadeEmEstoque);
        dlg.setArguments(b);

        return dlg;
    }


    public AtualizarQuantidadeEstoqueDialogFragment() {
        this.unidadesQtde = new HashMap<>();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        final View dialog = inflater.inflate(R.layout.dialog_update_qtde_em_estoque, container);
        final long estoqueId = getArguments().getLong("estoque_id");
        String unidade = getArguments().getString("unidade");
        final Integer quantidadeAtual = getArguments().getInt("quantidade_em_estoque");

        getDialog().setTitle("Editar Estoque");

        txtUnidade = (TextView) dialog.findViewById(R.id.txt_unidade);
        txtUnidade.setText(unidade);

        editQuantidade = (EditText) dialog.findViewById(R.id.edit_quantidade);
        editQuantidade.setText(quantidadeAtual.toString());


        Button btnCancelar = (Button) dialog.findViewById(R.id.btnCancelar);
        btnCancelar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                getDialog().dismiss();
            }
        });


        Button btnSalvar = (Button) dialog.findViewById(R.id.btnSalvar);
        btnSalvar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                final int novaQuantidade = Integer.valueOf(editQuantidade.getText().toString());

                if (novaQuantidade != quantidadeAtual) {
                    final ProgressDialog progressDlg = ProgressDialog.show(getActivity(),
                            "Atualizando informações", "Aguarde ...");

                    new UpdateEstoqueAsyncTask(estoqueId, novaQuantidade, new UpdateEstoqueAsyncTask.OnComplete() {
                        @Override
                        public void run(HttpResponse response) {
                            if (response != null) {
                                StatusLine statusLine = response.getStatusLine();

                                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                                    updateListener.onSuccess(novaQuantidade);
                                } else {
                                    updateListener.onFail("Erro " + statusLine.getStatusCode() + ": " + statusLine.getReasonPhrase());

                                }

                            } else {
                                Toast.makeText(getActivity(), "Erro ao conectar ao servidor", Toast.LENGTH_SHORT).show();
                            }

                            progressDlg.dismiss();
                            getDialog().dismiss();
                        }
                    }).execute();
                }

            }
        });

        return dialog;
    }

    public void setUpdateListener(UpdateListener listener) {
        this.updateListener =listener;
    }

}
