package br.com.arrasavendas.model;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.Serializable;

import br.com.arrasavendas.providers.CidadesProvider;

/**
 * Created by lsimaocosta on 14/03/16.
 */
public class Cidade implements Serializable,Cloneable{
    private String nome;
    private int id;
    private Uf uf;
    public final static int TERESINA_ID = 3582;


    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Uf getUf() {
        return uf;
    }

    public void setUf(Uf uf) {
        this.uf = uf;
    }

    public static Cidade fromId(Long idCity, Context ctx) {
        Uri uri = CidadesProvider.CONTENT_URI.
                buildUpon().appendPath(idCity.toString()).build();

        Cursor c = ctx.getContentResolver().query(uri,null,null,null,null);

        c.moveToFirst();

        Cidade city = null;
        if (c.moveToFirst()){
            city = new Cidade();

            city.setNome(c.getString(c.getColumnIndex(CidadesProvider.NOME)));
            city.setId(c.getInt(c.getColumnIndex(CidadesProvider._ID)));

            Uf uf = Uf.valueOf(c.getString(c.getColumnIndex(CidadesProvider.UF)));
            city.setUf(uf);
        }
        c.close();
        return city;
    }
}
