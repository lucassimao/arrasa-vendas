package br.com.arrasavendas.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Cliente implements Serializable, Cloneable {

    private String nome;
    private String celular, dddCelular;
    private String telefone, dddTelefone;
    private String endereco;
    private String bairro;
    private String cep;
    private Cidade cidade;
    private Long id;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Cliente c = (Cliente) super.clone();
        c.setCidade((Cidade) cidade.clone());

        return c;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

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

    public Cidade getCidade() {
        return cidade;
    }

    public void setCidade(Cidade cidade) {
        this.cidade = cidade;
    }

    public Uf getUf() {
        if (this.cidade != null) return this.cidade.getUf();
        else return null;
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
            enderecoObj.put("cep", this.cep);


            int idCity = this.cidade.getId();
            enderecoObj.put("cidade", new JSONObject().put("id", idCity));

            int idUf = this.cidade.getUf().getId();
            enderecoObj.put("uf", new JSONObject().put("id", idUf));

            objCliente.put("endereco", enderecoObj);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return objCliente;

    }

    @Override
    public String toString() {
        return "Cliente{" +
                "nome='" + nome + '\'' +
                ", celular='" + celular + '\'' +
                ", dddCelular='" + dddCelular + '\'' +
                ", telefone='" + telefone + '\'' +
                ", dddTelefone='" + dddTelefone + '\'' +
                ", endereco='" + endereco + '\'' +
                ", bairro='" + bairro + '\'' +
                ", id=" + id +
                '}';
    }
}