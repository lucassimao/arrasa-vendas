package br.com.arrasavendas;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsultarFreteAsyncTask extends AsyncTask<String, Void, Map<String, String>> {

    private static final String XML_PATTERN = "<Valor>(.*?)</Valor>.*<Erro>(.*?)</Erro>.*<MsgErro>(.*?)</MsgErro>";
    private OnComplete onComplete;

    public ConsultarFreteAsyncTask(OnComplete onComplete) {
        this.onComplete = onComplete;
    }

    @Override
    protected Map<String, String> doInBackground(String... params) {

        Map<String, String> map = new HashMap<String, String>();

        try {
            String cepDestino = params[0];
            map.put("PAC", calcularValorPAC(cepDestino));
            map.put("SEDEX", calcularValorSEDEX(cepDestino));

            return map;

        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            map.put("Erro", e.getMessage());
            return map;
        }
    }

    private String calcularValorPAC(String cep) throws IllegalArgumentException, IOException {
        String xml = makeRequest(cep, "41106");

        Pattern pattern = Pattern.compile(XML_PATTERN);
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            String valor = matcher.group(1);
            String erro = matcher.group(2);

            if (erro.trim().equals("0"))
                return valor;
            else
                throw new IllegalArgumentException(matcher.group(3));

        } else {
            return "0";
        }
    }

    private String calcularValorSEDEX(String cep) throws IllegalArgumentException, IOException {
        String xml = makeRequest(cep, "40010");

        Pattern pattern = Pattern.compile(XML_PATTERN);
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            String valor = matcher.group(1);
            String erro = matcher.group(2);

            if (erro.trim().equals("0"))
                return valor;
            else
                throw new IllegalArgumentException(matcher.group(3));

        } else {
            return "0";
        }
    }

    private String makeRequest(String sCepDestino, String codigoServico) throws IOException {


        Uri uri = Uri.parse("http://ws.correios.com.br/calculador/CalcPrecoPrazo.aspx").
                buildUpon().appendQueryParameter("nCdServico", codigoServico).
                appendQueryParameter("sCepOrigem", "64023620").
                appendQueryParameter("sCepDestino", sCepDestino.replace("-", "")).
                appendQueryParameter("nVlPeso", "1").
                appendQueryParameter("nCdFormato", "1").
                appendQueryParameter("nVlComprimento", "30").
                appendQueryParameter("nVlAltura", "30").
                appendQueryParameter("nVlLargura", "30").
                appendQueryParameter("sCdMaoPropria", "N").
                appendQueryParameter("nVlValorDeclarado", "0").
                appendQueryParameter("sCdAvisoRecebimento", "N").
                appendQueryParameter("StrRetorno", "xml").build();

        URL url = new URL(uri.toString());
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        httpConnection.setDoInput(true);
        httpConnection.setDoOutput(false);
        httpConnection.setConnectTimeout(4000);
        httpConnection.setRequestMethod("GET");
        httpConnection.setUseCaches(false);
        httpConnection.connect();

        StringBuilder stringBuilder = new StringBuilder();
        InputStream inputStream = null;
        String line;

        if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
            inputStream = httpConnection.getInputStream();
        else
            inputStream = httpConnection.getErrorStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }

        return stringBuilder.toString();
    }

    @Override
    protected void onPostExecute(Map<String, String> fretes) {
        if (this.onComplete != null) {
            onComplete.run(fretes);
        }
    }

    public interface OnComplete {
        void run(Map<String, String> response);
    }


}