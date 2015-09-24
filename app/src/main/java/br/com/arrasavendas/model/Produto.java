package br.com.arrasavendas.model;

import java.util.*;

public class Produto {
	private long id;
	private String nome;
	List<String> unidades;
	private Map<String,Long> unidadesIdMapping;
	private Map<String, Integer> quantidades;

	private Comparator<String> quantityComparator  = new Comparator<String>() {

		public int compare(String a, String b) {
            if (quantidades.get(a) == quantidades.get(b))
                return b.compareToIgnoreCase(a);
            else
			    return quantidades.get(b).compareTo(quantidades.get(a));
		}
	};

	public Produto(long produtoId, String nome) {
		this.id = produtoId;
		this.nome = nome;
		this.unidades = new LinkedList<>();
		this.quantidades = new HashMap<String, Integer>();
		this.unidadesIdMapping = new HashMap<String,Long>();

	}

	public String getNome() {
		return nome;
	}

	public void addUnidade(long estoqueId,String und,Integer qtde){
		this.unidadesIdMapping.put(und, estoqueId);
		this.quantidades.put(und, qtde);
		this.unidades.add(und);

        Collections.sort(unidades, quantityComparator);
    }

	public void updateQuantidade(String un,int qtde){
        this.quantidades.put(un, qtde);
        Collections.sort(unidades, quantityComparator);
	}

	public long getEstoqueId(String unidade) {
		return this.unidadesIdMapping.get(unidade);
	}

	public long getId() {
        return id;
    }

    public List<String> getUnidades() {
		return this.unidades;
	}

	public Map<String, Integer> getQuantidades() {
		return quantidades;
	}
}