package br.com.arrasavendas.venda;

import android.content.Context;
import android.os.AsyncTask;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsultarFreteAsyncTask extends AsyncTask<String, Void, Map<String, String>> {

    private final Context ctx;
    private String cep;

    public interface OnComplete {
        void run(Map<String, String> response);
    }

    private OnComplete onComplete;

    public ConsultarFreteAsyncTask(Context ctx, OnComplete onComplete) {
        this.onComplete = onComplete;
        this.ctx = ctx;
    }

    @Override
    protected Map<String, String> doInBackground(String... params) {

        Map<String, String> map = new HashMap<String, String>();

        try {
            String cepDestino = params[0];
            map.put("PAC", calcularValorPAC(cepDestino));
            map.put("SEDEX", calcularValorSEDEX(cepDestino));

            return map;

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            map.put("Erro",e.getMessage());
            return map;
        }
    }

    private String calcularValorPAC(String cep) throws IllegalArgumentException {
        String xml = makeRequest(cep, "41106");

        Pattern pattern = Pattern.compile("<Valor>(.*?)</Valor>.*<Erro>(.*?)</Erro>.*<MsgErro>(.*?)</MsgErro>");
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

    private String calcularValorSEDEX(String cep) throws IllegalArgumentException{
        String xml = makeRequest(cep, "40010");

        Pattern pattern = Pattern.compile("<Valor>(.*?)</Valor>.*<Erro>(.*?)</Erro>.*<MsgErro>(.*?)</MsgErro>");
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


    private String makeRequest(String sCepDestino, String codigoServico) {

        // instantiates httpclient to make request
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();

            List<NameValuePair> params = new LinkedList<NameValuePair>();
            params.add(new BasicNameValuePair("nCdServico", codigoServico));
            params.add(new BasicNameValuePair("sCepOrigem", "64023620"));
            params.add(new BasicNameValuePair("sCepDestino", sCepDestino.replace("-", "")));
            params.add(new BasicNameValuePair("nVlPeso", "1"));
            params.add(new BasicNameValuePair("nCdFormato", "1"));
            params.add(new BasicNameValuePair("nVlComprimento", "30"));
            params.add(new BasicNameValuePair("nVlAltura", "30"));
            params.add(new BasicNameValuePair("nVlLargura", "30"));
            params.add(new BasicNameValuePair("sCdMaoPropria", "N"));
            params.add(new BasicNameValuePair("nVlValorDeclarado", "0"));
            params.add(new BasicNameValuePair("sCdAvisoRecebimento", "N"));
            params.add(new BasicNameValuePair("StrRetorno", "xml"));

            String paramString = URLEncodedUtils.format(params, "utf-8");

            HttpGet httget = new HttpGet("http://ws.correios.com.br/calculador/CalcPrecoPrazo.aspx?" + paramString);
            HttpResponse response = httpclient.execute(httget);
            StringBuilder stringBuilder = new StringBuilder();

            HttpEntity entity = response.getEntity();
            InputStream content = entity.getContent();
            BufferedReader reader;
            reader = new BufferedReader(new InputStreamReader(content));
            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            return stringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    protected void onPostExecute(Map<String, String> fretes) {
        if (this.onComplete != null) {
            onComplete.run(fretes);
        }
    }


}