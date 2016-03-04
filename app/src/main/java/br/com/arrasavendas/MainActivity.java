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
import br.com.arrasavendas.util.Response;
import br.com.arrasavendas.venda.VendaActivity;

import java.net.HttpURLConnection;
import java.util.Map;

public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main);
        atualizarEnderecos();
    }


    /**
     * Método chamado no onCreate e caso
     * seja a primeira execuçao do aplicativo,
     * faz o 1º agendamento do sincronizador de endereços e baixa o estoque
     */
    private void atualizarEnderecos() {
        final String ARRASAVENDAS = "br.com.arrasavendas";
        final String KEY_PREFS_FIRST_LAUNCH = "br.com.arrasavendas.first_launch";

        SharedPreferences prefs = getSharedPreferences(ARRASAVENDAS,Activity.MODE_PRIVATE);

        if(prefs.getBoolean(KEY_PREFS_FIRST_LAUNCH,true)){
            prefs.edit().putBoolean(KEY_PREFS_FIRST_LAUNCH,false).commit();
            new DownloadJSONAsyncTask(this,null).execute(RemotePath.EstoquePath);
            Utilities.registrarSyncEnderecoAlarm(this);
        }
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
                    public void run(Map<String, String> fretesMap) {
                        progressDialog.dismiss();

                        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                        alert.setTitle("Valor do frete para " + cep);

                        if (fretesMap != null) {
                            String fretesAsString = fretesMap.toString().replace("}", "").replace("{", "");

                            ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = android.content.ClipData.newPlainText("fretesMap", fretesAsString);
                            clipboard.setPrimaryClip(clip);

                            alert.setMessage(fretesAsString);
                        } else
                            alert.setMessage("Serviço dos correios indisponível");

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
        final ProgressDialog progressDlg = ProgressDialog.show(this,
                "Atualizando informações", "Aguarde ...");

        new DownloadJSONAsyncTask(this, new DownloadJSONAsyncTask.OnCompleteListener() {

            @Override
            public void run(Response response) {
                progressDlg.dismiss();

                switch(response.getStatus()){
                    case HttpURLConnection.HTTP_OK:
                    case HttpURLConnection.HTTP_NO_CONTENT:

                        startEstoqueActivity();
                        break;
                    default:
                        Toast.makeText(getApplicationContext(),
                                "Erro " + response.getStatus()+ ": "+ response.getMessage(),
                                Toast.LENGTH_LONG).show();
                }

            }
        }).execute(RemotePath.EstoquePath);

    }

    public void onClickBtnNovaVenda(View v) {
        final ProgressDialog progressDlg = ProgressDialog.show(this, "Atualizando informações", "Aguarde ...");

        new DownloadJSONAsyncTask(this, new DownloadJSONAsyncTask.OnCompleteListener() {

            @Override
            public void run(Response response) {
                progressDlg.dismiss();

                switch(response.getStatus()){
                    case HttpURLConnection.HTTP_OK:
                    case HttpURLConnection.HTTP_NO_CONTENT:

                        Intent i = new Intent(getBaseContext(), VendaActivity.class);
                        startActivity(i);
                        break;
                    default:
                        Toast.makeText(getApplicationContext(),
                                "Erro " + response.getStatus()+ ": "+ response.getMessage(),
                                Toast.LENGTH_LONG).show();
                }

            }
        }).execute(RemotePath.EstoquePath);



    }

    public void onClickBtnEntregas(View v) {
        final ProgressDialog progressDlg = ProgressDialog.show(this, "Atualizando informações", "Aguarde ...");

        new DownloadJSONAsyncTask(this, new DownloadJSONAsyncTask.OnCompleteListener() {

            @Override
            public void run(Response response) {
                progressDlg.dismiss();

                switch(response.getStatus()){
                    case HttpURLConnection.HTTP_OK:
                    case HttpURLConnection.HTTP_NO_CONTENT:

                        Intent i = new Intent(getBaseContext(), EntregasActivity.class);
                        startActivity(i);
                        break;
                    default:
                        Toast.makeText(getApplicationContext(),
                                "Erro " + response.getStatus()+ ": "+ response.getMessage(),
                                Toast.LENGTH_LONG).show();
                }

            }
        }).execute(RemotePath.VendaPath);

    }

    public void onClickBtnFinanceiro(View v){
        final ProgressDialog progressDlg = ProgressDialog.show(this, "Atualizando informações", "Aguarde ...");

        new DownloadJSONAsyncTask(this, new DownloadJSONAsyncTask.OnCompleteListener() {

            @Override
            public void run(Response response) {
                progressDlg.dismiss();

                switch(response.getStatus()){
                    case HttpURLConnection.HTTP_OK:
                    case HttpURLConnection.HTTP_NO_CONTENT:

                        Intent i = new Intent(getBaseContext(), FinanceiroActivity.class);
                        startActivity(i);
                        break;
                    default:
                        Toast.makeText(getApplicationContext(),
                                "Erro " + response.getStatus()+ ": "+ response.getMessage(),
                                Toast.LENGTH_LONG).show();
                }

            }
        }).execute(RemotePath.CaixaPath);


    }

    public void onClickBtnImagesManager(View view){
        Intent i = new Intent(getBaseContext(), ImagesManagerActivity.class);
        startActivity(i);
    }

    private void startEstoqueActivity() {
        Intent i = new Intent(getBaseContext(), EstoqueActivity.class);
        startActivity(i);

    }

}
