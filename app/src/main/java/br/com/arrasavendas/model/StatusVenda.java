package br.com.arrasavendas.model;

/**
 * Created by lsimaocosta on 24/08/14.
 */
public enum StatusVenda {

    AguardandoPagamento("Aguardando pagamento"),
    EmAnalise("Em análise"),
    PagamentoRecebido("Paga"),
    Disponivel("Disponível"),
    EmDisputa("Em disputa"),
    Devolvida("Devolvida"),
    Cancelada("Cancelada"),
    Entregue("Entregue");


    String descricao;

    StatusVenda(String desc) {
        this.descricao = desc;
    }

    public String toString() {
        return this.descricao;
    }

    }
