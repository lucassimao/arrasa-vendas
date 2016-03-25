package br.com.arrasavendas;

import android.net.Uri;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.arrasavendas.model.ServicoCorreios;

public class ConsultarFreteAsyncTask extends AsyncTask<String, Void, Map> {

    public static final String ERROR_FIELD = "Erro";
    private static final String XML_PATTERN = "<Valor>(.*?)</Valor>.*<Erro>(.*?)</Erro>.*<MsgErro>(.*?)</MsgErro>";
    private OnComplete onComplete;

    public ConsultarFreteAsyncTask(OnComplete onComplete) {
        this.onComplete = onComplete;
    }

    @Override
    protected Map doInBackground(String... params) {

        Map map = new HashMap();

        try {
            String cepDestino = params[0];
            String valor = calcularValorPAC(cepDestino).replace(",", ".");
            map.put(ServicoCorreios.PAC, new BigDecimal(valor));

            valor = calcularValorSEDEX(cepDestino).replace(",", ".");
            map.put(ServicoCorreios.SEDEX, new BigDecimal(valor));

            return map;

        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            map.put(ERROR_FIELD, e.getMessage());
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
    protected void onPostExecute(Map response) {
        if (this.onComplete != null) {
            onComplete.run(response);
        }
    }

    public interface OnComplete {
        void run(Map response);
    }


}