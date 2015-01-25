package br.com.arrasavendas.estoque;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ItemEstoque {
	String nome;
	List<String> unidades;
	Map<String, Integer> quantidades;

	public ItemEstoque(String nome) {
		setNome(nome);
		this.unidades = new LinkedList<String>();
		this.quantidades = new HashMap<String, Integer>();
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}
	
	public void addUnidade(String und,Integer qtde){
		this.unidades.add(und);
		this.quantidades.put(und, qtde);
	}

}