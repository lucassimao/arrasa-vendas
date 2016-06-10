package br.com.arrasavendas.entregas.edit.customer;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.math.BigDecimal;
import java.util.Map;

import br.com.arrasavendas.ConsultarFreteAsyncTask;
import br.com.arrasavendas.R;
import br.com.arrasavendas.model.Cliente;
import br.com.arrasavendas.model.ServicoCorreios;
import br.com.arrasavendas.model.Venda;

/**
 * Created by lsimaocosta on 11/02/16.
 */
public class EditCorreiosFragment extends Fragment implements EditVendaListener {


    private static final String TAG = EditCorreiosFragment.class.getSimpleName();
    private Venda venda;


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(
                R.layout.fragment_edit_venda_correios, container, false);

        Bundle args = getArguments();
        this.venda = (Venda) args.getSerializable(EditVendaDialog.VENDA);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG,"onViewCreated");

        Spinner spinner = (Spinner) view.findViewById(R.id.spinner_envio);

        String[] metodos = {"Nenhum", ServicoCorreios.PAC.name(), ServicoCorreios.SEDEX.name()};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, metodos);

        spinner.setAdapter(adapter);
        setupBtnComputeShippingCost(view);

        setupView();
    }



    @Override
    public void setupView() {
        Log.d(TAG,"setupView");

        View view = getView();

        EditText editTextZipCode = (EditText) view.findViewById(R.id.edit_text_cep);
        editTextZipCode.setText(venda.getCliente().getCep());

        EditText editShippingCost = (EditText) view.findViewById(R.id.edit_text_frete);
        editShippingCost.setText("" + venda.getFreteEmCentavos() / 100.0);

        EditText editTrackingCode = (EditText) view.findViewById(R.id.edit_text_rastreio);
        editTrackingCode.setText(venda.getCodigoRastreio());

        // TODO melhorar essas numeros 1 e 2 em variaveis constantes
        Spinner spinnerDeliveryMethod = (Spinner) view.findViewById(R.id.spinner_envio);
        if (venda.getServicoCorreios() == null)
            spinnerDeliveryMethod.setSelection(0);
        else
            spinnerDeliveryMethod.setSelection((venda.getServicoCorreios() == ServicoCorreios.PAC) ? 1 : 2); // ids dos array acima
    }



    private void setupBtnComputeShippingCost(final View view) {
        Button btn = (Button) view.findViewById(R.id.btn_calcular_frete);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final ProgressDialog progressDialog = ProgressDialog.show(getActivity(),
                        "Consultando Frete", "Aguarde ...");

                EditText editTextZipCode = (EditText) view.findViewById(R.id.edit_text_cep);
                String zipCode = editTextZipCode.getText().toString();

                if (TextUtils.isEmpty(zipCode.trim())) {
                    progressDialog.dismiss();
                    Toast.makeText(getActivity(), "Informe o CEP", Toast.LENGTH_SHORT).show();
                    return;
                }

                new ConsultarFreteAsyncTask(new ConsultarFreteAsyncTask.OnComplete() {
                    @Override
                    public void run(Map response) {

                        progressDialog.dismiss();

                        if (!response.containsKey(ConsultarFreteAsyncTask.ERROR_FIELD)) {

                            Spinner spinnerDeliverMethod = (Spinner) view.findViewById(R.id.spinner_envio);
                            String deliveryMethod = spinnerDeliverMethod.getSelectedItem().toString();
                            ServicoCorreios servicoCorreios = ServicoCorreios.valueOf(deliveryMethod);
                            BigDecimal value = null;

                            EditText editTextFrete = (EditText) view.findViewById(R.id.edit_text_frete);

                            switch (servicoCorreios) {
                                case PAC:
                                    value = (BigDecimal) response.get(ServicoCorreios.PAC);
                                    editTextFrete.setText(value.toString());
                                    break;
                                case SEDEX:
                                    value = (BigDecimal) response.get(ServicoCorreios.SEDEX);
                                    editTextFrete.setText(value.toString());

                                    break;
                            }
                        } else {
                            String msg = (String) response.get(ConsultarFreteAsyncTask.ERROR_FIELD);
                            Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
                        }

                    }
                }).execute(zipCode);
            }
        });
    }



    @Override
    public void writeChanges() {
        View view = getView();
        if (view == null) {
            Log.d(TAG,"getView()== null skiping ...");
            return;
        }
        Log.d(TAG, "writing changes ....");


        Spinner spinnerDeliveryMethod = (Spinner) view.findViewById(R.id.spinner_envio);
        if (spinnerDeliveryMethod.getSelectedItemPosition() == 0)
            venda.setServicoCorreios(null);
        else {
            String deliveryMethod = (String) spinnerDeliveryMethod.getSelectedItem();
            venda.setServicoCorreios(ServicoCorreios.valueOf(deliveryMethod));
        }

        Cliente cliente = venda.getCliente();
        EditText editTextZipCode = (EditText) view.findViewById(R.id.edit_text_cep);
        String zipCode = editTextZipCode.getText().toString().trim();
        if (zipCode.length() > 0)
            cliente.setCep(zipCode);

        EditText editShippingCost = (EditText) view.findViewById(R.id.edit_text_frete);
        String shippingCost = editShippingCost.getText().toString().trim();
        if (shippingCost.length() > 0) {
            BigDecimal cost = new BigDecimal(shippingCost);
            cost = cost.multiply(BigDecimal.valueOf(100));
            venda.setFreteEmCentavos(cost.longValue());
        }


        EditText editTrackingCode = (EditText) view.findViewById(R.id.edit_text_rastreio);
        venda.setCodigoRastreio(editTrackingCode.getText().toString());

    }


}

