package br.com.arrasavendas.venda;

public enum Vendedor {

	Lucas(2), Adna(3), MariaClara(6);

	private int id;

	Vendedor(int id) {
		this.id = id;
	}

    public int getId() {
        return id;
    }
}
