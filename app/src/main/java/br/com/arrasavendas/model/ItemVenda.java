package br.com.arrasavendas.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

public class ItemVenda implements Serializable {

    private String nomeProduto;
    private String unidade;
    private Long produtoID;
	private Integer quantidade;
	private BigDecimal precoAVista, precoAPrazo;
	
	
	public ItemVenda(Long produtoID, String nomeProduto,String unidade, Integer quantidade,BigDecimal precoAVista,BigDecimal precoAPrazo) {
		this.produtoID = produtoID;
        this.nomeProduto = nomeProduto;
		this.unidade = unidade;
		this.quantidade = quantidade;
		this.precoAVista = precoAVista;
		this.precoAPrazo = precoAPrazo;
	}

    public Integer getQuantidade() {
        return quantidade;
    }

    public Long getProdutoID() {
        return produtoID;
    }

    public String getUnidade() {
        return unidade;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public JSONObject asJsonObject() {
		JSONObject obj = new JSONObject();
		try {

            JSONObject produtoJSONObj = new JSONObject();
            produtoJSONObj.put("id",this.produtoID);
			obj.put("produto", produtoJSONObj);

			obj.put("unidade", this.unidade);
			obj.put("quantidade", this.quantidade);
			obj.put("precoAVistaEmCentavos", precoAVista.multiply(BigDecimal.valueOf(100)));
			obj.put("precoAPrazoEmCentavos", precoAPrazo.multiply(BigDecimal.valueOf(100)));

			return obj;

		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public BigDecimal getPrecoAVista() {
		return precoAVista;
	}

	public BigDecimal getPrecoAPrazo() {
		return precoAPrazo;
	}
	
	@Override
	public String toString() {
		return String.format(Locale.getDefault(), "%d %s (%s)", this.quantidade,this.nomeProduto,this.unidade);
	}
	
	
	
	
	
	
}
