package br.com.arrasavendas;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class LoginActivity extends Activity {


    private TextView editTextLogin, editTextSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TODO tratar a inexistencia do TOKEN
        Application app = (Application) getApplication();
        String accessToken = app.getAccessToken();

        if (accessToken != null) {
            showMainActivity();
            finish();
        }else{
            setContentView(R.layout.activity_login);
            this.editTextLogin = (TextView) findViewById(R.id.editTextLogin);
            this.editTextSenha = (TextView) findViewById(R.id.editTextSenha);
        }


    }

    public void onClickButtonLogin(View view) {

        SharedPreferences sp = getSharedPreferences("br.com.arrasaamiga.auth", MODE_PRIVATE);

        if (sp.getString("access_token", null) != null) {
            showMainActivity();
        } else {
            String login = this.editTextLogin.getText().toString();
            String senha = this.editTextSenha.getText().toString();

            final ProgressDialog progressDlg = ProgressDialog.show(this, "Autenticando", "Aguarde ...");
            final LoginAsyncTask loginAsyncTask = new LoginAsyncTask(new Runnable() {

                @Override
                public void run() {
                    progressDlg.dismiss();
                    showMainActivity();

                }
            }, new Runnable() {
                @Override
                public void run() {
                    progressDlg.dismiss();
                    Toast.makeText(LoginActivity.this,"Login ou senha inválido(s)",Toast.LENGTH_SHORT).show();
                }
            },this);

            loginAsyncTask.execute(new LoginAsyncTask.LoginSenha(login, senha));


        }
    }

    private void showMainActivity() {
        Intent i = new Intent(getBaseContext(), MainActivity.class);
        startActivity(i);
        finish();
    }


}
