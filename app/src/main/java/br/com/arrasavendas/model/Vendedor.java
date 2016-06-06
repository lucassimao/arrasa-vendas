package br.com.arrasavendas.model;

public enum Vendedor {

	Lucas(2,"lsimaocosta@gmail.com"), Adna(3,"fisio.adnadantas@gmail.com"), MariaClara(6,"mariaclaravn26@gmail.com");

	private final String login;
	private int id;

	Vendedor(int id, String login) {
		this.id = id;
		this.login=login;
	}

	public String getLogin() {
		return login;
	}

	public int getId() {
        return id;
    }
}
