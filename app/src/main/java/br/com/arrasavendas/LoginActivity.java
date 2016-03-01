package br.com.arrasavendas;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

import br.com.arrasavendas.util.Response;


public class LoginActivity extends Activity {


    private TextView editTextLogin, editTextSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Application app = (Application) getApplication();

        if (app.isAuthenticated()) {
            showMainActivity();
            finish();
        } else {
            setContentView(R.layout.activity_login);
            this.editTextLogin = (TextView) findViewById(R.id.editTextLogin);
            this.editTextSenha = (TextView) findViewById(R.id.editTextSenha);
        }


    }

    public void onClickButtonLogin(View view) {


        String login = this.editTextLogin.getText().toString();
        String senha = this.editTextSenha.getText().toString();

        final ProgressDialog progressDlg = ProgressDialog.show(this, "Autenticando", "Aguarde ...");
        final LoginAsyncTask loginAsyncTask = new LoginAsyncTask(new LoginAsyncTask.OnLogin() {
            @Override
            public void run(Response response) {
                progressDlg.dismiss();

                switch(response.getStatus()){
                    case HttpURLConnection.HTTP_OK:

                        try {

                            JSONObject obj = new JSONObject(response.getMessage());
                            Application.salvarToken(obj.getString("username"),
                                    obj.getString("roles"),obj.getString("access_token"));
                            showMainActivity();

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(LoginActivity.this,
                                    "Erro: " + response.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        break;
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        Toast.makeText(LoginActivity.this,
                                "Login ou senha inválido(s)", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(LoginActivity.this,
                                "Erro " + response.getStatus() +": " +
                                        response.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        },this);

        loginAsyncTask.execute(new LoginAsyncTask.LoginSenha(login, senha));

    }



    private void showMainActivity() {
        Intent i = new Intent(getBaseContext(), MainActivity.class);
        startActivity(i);
        finish();
    }


}
