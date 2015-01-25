package br.com.arrasavendas.venda;

import org.json.JSONException;
import org.json.JSONObject;

public class Cliente {

	private String nome;
	private String celular, dddCelular;
	private String telefone, dddTelefone;
	private String endereco;
	private String bairro;
	private Long id;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getCelular() {
		return celular;
	}

	public void setCelular(String celular) {
		this.celular = celular;
	}

	public String getDddCelular() {
		return dddCelular;
	}

	public void setDddCelular(String dddCelular) {
		this.dddCelular = dddCelular;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public String getDddTelefone() {
		return dddTelefone;
	}

	public void setDddTelefone(String dddTelefone) {
		this.dddTelefone = dddTelefone;
	}

	public String getEndereco() {
		return endereco;
	}

	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}

	public String getBairro() {
		return bairro;
	}

	public void setBairro(String bairro) {
		this.bairro = bairro;
	}

	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public JSONObject toJson() {
		JSONObject objCliente = new JSONObject();
		try {
			objCliente.put("nome", this.nome);
			objCliente.put("dddTelefone", this.dddTelefone);
			objCliente.put("telefone", this.telefone);
			objCliente.put("dddCelular", this.dddCelular);
			objCliente.put("celular", this.celular);
			

			JSONObject enderecoObj = new JSONObject();
			enderecoObj.put("complemento", this.endereco);
			enderecoObj.put("bairro", this.bairro);
			enderecoObj.put("cidade", new JSONObject().put("id",3582));
			enderecoObj.put("cidade", new JSONObject().put("id",3582));
			enderecoObj.put("uf", new JSONObject().put("id",17));

			objCliente.put("endereco", enderecoObj);
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return objCliente;

	}


}