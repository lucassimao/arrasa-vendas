package br.com.arrasavendas.financeiro;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import br.com.arrasavendas.R;
import br.com.arrasavendas.model.FormaPagamento;
import br.com.arrasavendas.model.MovimentoCaixa;
import br.com.arrasavendas.model.TipoMovimento;
import br.com.arrasavendas.util.Response;

/**
 * Created by lsimaocosta on 15/02/16.
 */
public class MovimentoCaixaDialog extends DialogFragment {

    private TextView txtViewData;
    private EditText edtTxtDescricao;
    private RadioGroup radioGrpTipoMovimento, radioGrpFormaPagamento;
    private EditText edtTxtValor;
    private Date data;
    private FinanceiroDAO dao;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        Bundle args = getArguments();
        this.dao = (FinanceiroDAO) args.get(FinanceiroActivity.FINANCEIRO_DAO);

        final View view = inflater.inflate(R.layout.dialog_movimento_caixa, null);
        edtTxtDescricao = (EditText) view.findViewById(R.id.editTxtDescricao);
        radioGrpFormaPagamento = (RadioGroup) view.findViewById(R.id.radioGrpFormaPagamento);
        radioGrpTipoMovimento = (RadioGroup) view.findViewById(R.id.radioGroupTipoMovimento);

        txtViewData = (TextView) view.findViewById(R.id.txtViewData);
        txtViewData.setOnClickListener(new TxtViewDataOnClickListener());

        edtTxtValor = (EditText) view.findViewById(R.id.editTxtValor);


        final AlertDialog d = new AlertDialog.Builder(getActivity()).setView(view).setTitle("Lançar Movimentos")
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dismiss();
                            }
                        }).create();

        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (validarCampos()) {
                            final MovimentoCaixa mc = getMovimentoCaixa();
                            final FragmentActivity activity = getActivity();
                            final ProgressDialog progressDialog = ProgressDialog.show(activity, "Salvando Movimento", "Aguarde ...");

                            new SalvarMovimentoDeCaixaAsyncTask(mc, activity, new SalvarMovimentoDeCaixaAsyncTask.OnComplete() {
                                @Override
                                public void run(Response response) {
                                    progressDialog.dismiss();
                                    d.dismiss();

                                    String msg = "Movimento salvo!";
                                    if (response.getStatus() == HttpURLConnection.HTTP_CREATED) {
                                        try {
                                            dao.addMovimento(new JSONObject(response.getMessage()));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        msg = String.format("Erro %d: (%s)",
                                                response.getStatus(),response.getMessage());
                                    }
                                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();


                                }
                            }).execute();
                        }
                    }
                });

            }
        });


        return d;
    }

    @NonNull
    private MovimentoCaixa getMovimentoCaixa() {
        final MovimentoCaixa mc = new MovimentoCaixa();
        mc.setData(data);
        mc.setDescricao(edtTxtDescricao.getText().toString());

        switch (radioGrpTipoMovimento.getCheckedRadioButtonId()) {
            case R.id.radioTipoMovimentoPositivo:
                mc.setTipoMovimento(TipoMovimento.POSITIVO);
                break;
            case R.id.radioTipoMovimentoNegativo:
                mc.setTipoMovimento(TipoMovimento.NEGATIVO);
                break;
        }

        switch (radioGrpFormaPagamento.getCheckedRadioButtonId()) {
            case R.id.radioFormaPagamentoAVista:
                mc.setFormaPagamento(FormaPagamento.AVista);
                break;
            case R.id.radioFormaPagamentoParcelado:
                mc.setFormaPagamento(FormaPagamento.PagSeguro);
                break;
        }

        mc.setValor(new BigDecimal(edtTxtValor.getText().toString()));
        mc.setFormaPagamento(FormaPagamento.AVista);
        return mc;
    }

    private boolean validarCampos() {
        if (TextUtils.isEmpty(edtTxtDescricao.getText())) {
            Toast.makeText(getActivity(), "Informe a descrição", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (data == null) {
            Toast.makeText(getActivity(), "Informe a data do movimento", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(edtTxtValor.getText())) {
            Toast.makeText(getActivity(), "Informe o valor do movimento", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private class TxtViewDataOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final Calendar hoje = Calendar.getInstance();

            DatePickerDialog dlg = new DatePickerDialog(getActivity(),
                    new DatePickerDialog.OnDateSetListener() {

                        @Override
                        public void onDateSet(DatePicker arg0, int ano, int mes, int diaDoMes) {

                            Calendar calendar = Calendar.getInstance();
                            calendar.set(Calendar.YEAR, ano);
                            calendar.set(Calendar.MONTH, mes);
                            calendar.set(Calendar.DAY_OF_MONTH, diaDoMes);

                            MovimentoCaixaDialog.this.data = calendar.getTime();

                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - EEEE", Locale.getDefault());
                            txtViewData.setText(sdf.format(calendar.getTime()));

                        }
                    }, hoje.get(Calendar.YEAR), hoje.get(Calendar.MONTH), hoje
                    .get(Calendar.DAY_OF_MONTH));

            dlg.setTitle("Escolha a data da entrega");

            dlg.show();
        }
    }
}
