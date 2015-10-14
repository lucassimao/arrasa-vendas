package br.com.arrasavendas.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Venda implements Serializable{
	
	private Long id;
	private Date dataEntrega;
	private Cliente cliente;
    private Vendedor vendedor;
	private List<ItemVenda> itens;
    private FormaPagamento formaDePagamento;
    private StatusVenda status;
    private TurnoEntrega turnoEntrega;
    private Double taxaEntrega = 2d;

    public Date getDataEntrega() {
		return dataEntrega;
	}

	public void setDataEntrega(Date dataEntrega) {
		this.dataEntrega = dataEntrega;
	}

	public Cliente getCliente() {
		return this.cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public List<ItemVenda> getItens() {
		return itens;
	}

	public void setItens(List<ItemVenda> itens) {
		this.itens = itens;
	}

	public void addToItens(ItemVenda item) {
		if (this.itens == null)
			this.itens = new LinkedList<ItemVenda>();

		this.itens.add(item);

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

    public Vendedor getVendedor() {
        return vendedor;
    }

    public void setVendedor(Vendedor vendedor) {
        this.vendedor = vendedor;
    }

    public void setVendedor(String email){
        switch(email){
            case "lsimaocosta@gmail.com":
                this.vendedor = Vendedor.Lucas;
                break;
            case "fisio.adnadantas@gmail.com":
                this.vendedor = Vendedor.Adna;
                break;
            case "mariaclaravn26@gmail.com":
                this.vendedor = Vendedor.MariaClara;
                break;

        }
    }

    public void setFormaDePagamento(FormaPagamento formaDePagamento) {
        this.formaDePagamento = formaDePagamento;
    }

    public FormaPagamento getFormaDePagamento() {
        return formaDePagamento;
    }

    public void setStatus(StatusVenda status) {
        this.status = status;
    }

    public StatusVenda getStatus() {
        return status;
    }

    public TurnoEntrega getTurnoEntrega() {
        return turnoEntrega;
    }

    public void setTurnoEntrega(TurnoEntrega turnoEntrega) {
        this.turnoEntrega = turnoEntrega;
    }

    public Double getValorTotal() {
        BigDecimal valorTotal = BigDecimal.valueOf(taxaEntrega);
        BigDecimal subTotal = null;

        for(ItemVenda item: itens){
            switch (formaDePagamento){
                case AVista:
                    subTotal = item.getPrecoAVista().multiply(BigDecimal.valueOf(item.getQuantidade()));
                    valorTotal = valorTotal.add(subTotal);
                    break;
                case PagSeguro:
                    subTotal = item.getPrecoAPrazo().multiply(BigDecimal.valueOf(item.getQuantidade()));
                    valorTotal = valorTotal.add(subTotal);
                    break;
            }
        }

        return valorTotal.doubleValue();
    }
}
