package br.com.arrasavendas.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ItemEstoque {
	private String nome;
	private long idProduto;
	private List<String> unidades;
	private Map<String, Integer> quantidades;

	public ItemEstoque(String nome,long idProduto) {
		this.nome = nome;
		this.idProduto = idProduto;
		this.unidades = new LinkedList<String>();
		this.quantidades = new HashMap<String, Integer>();
	}

	public String getNome() {
		return nome;
	}

	public void addUnidade(String und,Integer qtde){
		this.unidades.add(und);
		this.quantidades.put(und, qtde);
	}

    public long getIdProduto() {
        return idProduto;
    }

    public List<String> getUnidades() {
		return unidades;
	}

	public Map<String, Integer> getQuantidades() {
		return quantidades;
	}
}