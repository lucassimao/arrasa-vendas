package br.com.arrasavendas;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import br.com.arrasavendas.entregas.EntregasActivity;
import br.com.arrasavendas.estoque.EstoqueActivity;
import br.com.arrasavendas.financeiro.FinanceiroActivity;
import br.com.arrasavendas.imagesManager.ImagesManagerActivity;
import br.com.arrasavendas.providers.EstoqueProvider;
import br.com.arrasavendas.util.Response;
import br.com.arrasavendas.venda.VendaActivity;

import java.net.HttpURLConnection;
import java.util.Map;

public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main);
    }

    public void onClickBtnCalcularFrete(View v) {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Consultar Frete");
        alert.setMessage("CEP: ");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        alert.setView(input);


        alert.setPositiveButton("Calcular", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final String cep = input.getText().toString();

                final ProgressDialog progressDialog = ProgressDialog.show(MainActivity.this, "Consultando Frete", "Aguarde ...");
                new ConsultarFreteAsyncTask(new ConsultarFreteAsyncTask.OnComplete() {

                    @Override
                    public void run(Map response) {
                        progressDialog.dismiss();

                        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                        alert.setTitle("Valor do frete para " + cep);

                        if (!response.containsKey(ConsultarFreteAsyncTask.ERROR_FIELD)) {
                            String fretesAsString = response.toString().replace("}", "").replace("{", "");

                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("response", fretesAsString);
                            clipboard.setPrimaryClip(clip);

                            alert.setMessage(fretesAsString);
                        } else {
                            String msg = (String) response.get(ConsultarFreteAsyncTask.ERROR_FIELD);
                            alert.setMessage(msg);
                        }

                        alert.setPositiveButton("OK", null);
                        alert.show();

                    }
                }).execute(cep);

                dialog.dismiss();
            }
        });

        alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });

        alert.show();
    }

    public void onClickBtnEstoque(View v) {
        Intent i = new Intent(getBaseContext(),  EstoqueActivity.class);

        if (Application.isDBUpdated()){
            startActivity(i);
        }else{
            updateDB(i);
        }

    }

    public void onClickBtnNovaVenda(View v) {

        Intent i = new Intent(getBaseContext(),  VendaActivity.class);

        if (Application.isDBUpdated()){
            startActivity(i);
        }else{
            updateDB(i);
        }

    }

    public void onClickBtnEntregas(View v) {
        Intent i = new Intent(getBaseContext(),  EntregasActivity.class);

        if (Application.isDBUpdated()){
            startActivity(i);
        }else{
            updateDB(i);
        }
    }

    private void updateDB(final Intent i) {
        final ProgressDialog progressDlg = ProgressDialog.show(this, "Atualizando informações", "Aguarde ...");

        new UpdateDBAsyncTask(this, new UpdateDBAsyncTask.OnCompleteListener() {

            @Override
            public void run(Response response) {
                progressDlg.dismiss();

                switch(response.getStatus()){
                    case HttpURLConnection.HTTP_OK:
                    case HttpURLConnection.HTTP_NO_CONTENT:
                        startActivity(i);
                        break;
                    default:
                        Toast.makeText(getApplicationContext(),
                                "Erro " + response.getStatus()+ ": "+ response.getMessage(),
                                Toast.LENGTH_LONG).show();
                }

            }
        }).execute();
    }

    public void onClickBtnFinanceiro(View v){
        Intent i = new Intent(getBaseContext(),  FinanceiroActivity.class);

        if (Application.isDBUpdated()){
            startActivity(i);
        }else{
            updateDB(i);
        }
    }

    public void onClickBtnImagesManager(View view){
        Intent i = new Intent(getBaseContext(), ImagesManagerActivity.class);
        startActivity(i);
    }

}
