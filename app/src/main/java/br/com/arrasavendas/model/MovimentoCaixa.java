package br.com.arrasavendas.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by lsimaocosta on 19/02/16.
 */
public class MovimentoCaixa {
    private String descricao;
    private TipoMovimento tipoMovimento;
    private Date data;
    private FormaPagamento formaPagamento;
    private BigDecimal valor;

    public MovimentoCaixa(JSONObject jsonObject) {
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");

        try {
            descricao = jsonObject.getString("descricao");
            valor = BigDecimal.valueOf(jsonObject.getLong("valorEmCentavos") / 100.0);
            tipoMovimento = TipoMovimento.valueOf(jsonObject.getString("tipoMovimentoCaixa"));
            formaPagamento = FormaPagamento.valueOf(jsonObject.getString("formaPagamento"));
            data = df.parse(jsonObject.getString("data"));
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public MovimentoCaixa() {

    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public TipoMovimento getTipoMovimento() {
        return tipoMovimento;
    }

    public void setTipoMovimento(TipoMovimento tipoMovimento) {
        this.tipoMovimento = tipoMovimento;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public Date getData() {
        return data;
    }

    public void setData(Date data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "MovimentoCaixa{" +
                "descricao='" + descricao + '\'' +
                ", tipoMovimento=" + tipoMovimento +
                ", data=" + data +
                ", formaPagamento=" + formaPagamento +
                ", valor=" + valor +
                '}';
    }

    public JSONObject toJSONObject() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'", Locale.getDefault());

        JSONObject obj = new JSONObject();
        try {
            obj.put("descricao",this.descricao);
            obj.put("tipoMovimentoCaixa", tipoMovimento.name());
            long valorEmCentavos = valor.multiply(BigDecimal.valueOf(100)).longValue();
            obj.put("valorEmCentavos", valorEmCentavos);
            obj.put("formaPagamento",formaPagamento.name());
            obj.put("data", sdf.format(data));
            return obj;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public BigDecimal getValor() {
        return valor;
    }

}
