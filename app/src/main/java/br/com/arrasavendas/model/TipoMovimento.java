package br.com.arrasavendas.model;

/**
 * Created by lsimaocosta on 19/02/16.
 */
public enum TipoMovimento {
    POSITIVO("Positivo"),NEGATIVO("Negativo");

    private final String str;

    TipoMovimento(String str){
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
