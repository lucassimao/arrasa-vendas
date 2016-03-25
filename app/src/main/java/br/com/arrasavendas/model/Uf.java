package br.com.arrasavendas.model;

/**
 * Created by lsimaocosta on 13/03/16.
 */
public enum Uf {

    AC(1),
    AL(2),
    AM(3),
    AP(4),
    BA(5),
    CE(6),
    DF(7),
    ES(8),
    GO(9),
    MA(10),
    MG(11),
    MS(12),
    MT(13),
    PR(18),
    PB(15),
    PA(14),
    PE(16),
    PI(17),
    RJ(19),
    RN(20),
    RS(23),
    RO(21),
    RR(22),
    SC(24),
    SE(25),
    SP(26),
    TO(27);

    private int id;

    Uf(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
