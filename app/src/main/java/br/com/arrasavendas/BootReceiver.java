package br.com.arrasavendas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by lsimaocosta on 02/11/15.
 * <p/>
 * Receiver utilizado para reconfigurar o alarme que dispara o sicronizador
 * de endereço depois da reinicialização do dispositivo
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent i) {
        Log.d(":: BOOT RECEIVER :: ", "Configurando alarme");
//        Utilities.registrarSyncEnderecoAlarm(context);
    }




}
