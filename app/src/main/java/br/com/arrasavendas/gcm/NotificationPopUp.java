package br.com.arrasavendas.gcm;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import br.com.arrasavendas.R;

/**
 * Created by lsimaocosta on 27/03/16.
 */
public class NotificationPopUp extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_pop_up);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String title = bundle.getString(MsgListenerService.TITLE);
        String msg = bundle.getString(MsgListenerService.MESSAGE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
//        builder.setNegativeButton("Dunno", null);

        builder.show();
    }
}