package br.com.arrasavendas.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Venda implements Serializable,Cloneable{
	
	private Long id;
	private Date dataEntrega;
	private Cliente cliente;
    private Vendedor vendedor;
	private List<ItemVenda> itens;
    private FormaPagamento formaDePagamento;
    private StatusVenda status;
    private ServicoCorreios servicoCorreios;
    private TurnoEntrega turnoEntrega;
    private Double taxaEntrega = 2d;
    private String[] anexos;
    private int abatimento;
    private boolean flagVaiBuscar;
    private boolean flagJaBuscou;
    private long freteEmCentavos;
    private String codigoRastreio;


    @Override
    public Object clone() throws CloneNotSupportedException {
        // deep copy
        Venda v = (Venda) super.clone();
        v.cliente = (Cliente) cliente.clone();
        v.itens = new LinkedList<>();

        for(ItemVenda i: this.itens)
            v.addToItens((ItemVenda) i.clone());

        return v;
    }



    public ServicoCorreios getServicoCorreios() {
        return servicoCorreios;
    }

    public void setServicoCorreios(ServicoCorreios servicoCorreios) {
        this.servicoCorreios = servicoCorreios;
    }

    public long getFreteEmCentavos() {
        return freteEmCentavos;
    }

    public void setFreteEmCentavos(long freteEmCentavos) {
        this.freteEmCentavos = freteEmCentavos;
    }

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

	public void addToItens(ItemVenda novoItem) {
		if (this.itens == null)
			this.itens = new LinkedList<ItemVenda>();

        boolean added = false;
        for(ItemVenda item: this.itens){
            if (item.getProdutoID().equals(novoItem.getProdutoID()) && item.getUnidade().equals(novoItem.getUnidade())){
                item.setQuantidade(item.getQuantidade()+novoItem.getQuantidade());
                added = true;
                break;
            }
        }
        if (!added)
            this.itens.add(novoItem);

	}

    public boolean isFlagVaiBuscar() {
        return flagVaiBuscar;
    }

    public void setFlagVaiBuscar(boolean flagVaiBuscar) {
        this.flagVaiBuscar = flagVaiBuscar;
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
        BigDecimal valorTotal = (flagVaiBuscar)?BigDecimal.ZERO:BigDecimal.valueOf(taxaEntrega);
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

        return valorTotal.doubleValue() - getAbatimentoEmReais();
    }

    public boolean contemItem(Long produtoID, String unidade) {

        for(ItemVenda item: this.itens){
            if (item.getProdutoID().equals(produtoID) && item.getUnidade().equals(unidade)){
                return true;
            }
        }

        return false;
    }

    public String[] getAnexos() {
        return anexos;
    }

    public void setAnexos(String[] anexos) {
        this.anexos = anexos;
    }

    public void setAbatimentoEmCentavos(int abatimento) {
        this.abatimento = abatimento;
    }

    public int getAbatimentoEmCentavos() {
        return abatimento;
    }

    public double getAbatimentoEmReais(){
        return this.abatimento/100.0;
    }

    public String getCodigoRastreio() {
        return codigoRastreio;
    }

    public void setCodigoRastreio(String codigoRastreio) {
        this.codigoRastreio = codigoRastreio;
    }

    public void setFlagJaBuscou(boolean flagJaBuscou) {
        this.flagJaBuscou = flagJaBuscou;
    }

    public boolean isFlagJaBuscou() {
        return flagJaBuscou;
    }
}
