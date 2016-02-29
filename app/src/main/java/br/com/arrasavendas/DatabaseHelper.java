package br.com.arrasavendas;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "ArrasaVendas";
	private static final int DATABASE_VERSION = 58;

	public static final String TABLE_VENDAS = "VENDAS";
	private static final String CREATE_TABLE_VENDAS = "create table "
			+ TABLE_VENDAS
			+ "(_id integer primary key, vendedor_id integer not null, carrinho text not null,"
			+ " data_entrega integer not null, forma_pagamento text null,status text null," +
			"turno text null, cliente text not null, anexos_json_array text," +
			"last_updated_timestamp integer);";

	public static final String TABLE_ESTOQUE = "ESTOQUE";
	private static final String CREATE_TABLE_ESTOQUE = "create table "
			+ TABLE_ESTOQUE
			+ "(_id integer primary key, produto_nome text not null, produto_nome_ascii text not null," +
			"produto_id integer not null,prevoAVista REAL,prevoAPrazo REAL, " +
			"unidade text not null, quantidade integer not null, last_updated_timestamp integer);";

	private static final String CREATE_INDEX_PRODUTO_ID = "CREATE INDEX produto_idx on ESTOQUE(produto_id);";

    public static final String TABLE_DOWNLOADED_IMAGES = "DOWNLOADED_IMAGES";
	private static final String CREATE_TABLE_DOWNLOADED_IMAGES = "create table "
			+ TABLE_DOWNLOADED_IMAGES
			+ "(_id integer primary key AUTOINCREMENT, produto_id integer not null, " +
			"produto text,produto_ascii text,image_name text not null,local_path text, " +
			"unidade text not null,is_ignored integer,estoque_id integer, UNIQUE (image_name,unidade,produto_id) " +
			"ON CONFLICT IGNORE)";

	public static final String TABLE_CLIENTES = "CLIENTES";
	private static final String CREATE_TABLE_CLIENTES = "create table "
			+ TABLE_CLIENTES
			+ "(_id integer primary key AUTOINCREMENT, cliente_id integer,nome text, celular " +
			"text,ddd_celular text,telefone text,ddd_telefone text, endereco text,uf text," +
			"id_uf integer,id_cidade integer, cidade text,bairro text, " +
			"last_updated_timestamp integer, UNIQUE(cliente_id) ON CONFLICT REPLACE," +
			"UNIQUE(telefone,celular) ON CONFLICT REPLACE)";

    private static final String CREATE_INDEX_TELEFONE = "CREATE INDEX telefone_idx on CLIENTES(telefone);";
    private static final String CREATE_INDEX_CELULAR = "CREATE INDEX celular_idx on CLIENTES(celular);";

	public static final String TABLE_FINANCEIRO = "FINANCEIRO";
	private static final String CREATE_TABLE_FINANCEIRO = "create table "
			+ TABLE_FINANCEIRO
			+ "(_id integer primary key, json text not null)";

    public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_VENDAS);
		db.execSQL(CREATE_TABLE_ESTOQUE);
		db.execSQL(CREATE_INDEX_PRODUTO_ID);
		db.execSQL(CREATE_TABLE_DOWNLOADED_IMAGES);
		db.execSQL(CREATE_TABLE_CLIENTES);
        db.execSQL(CREATE_INDEX_TELEFONE);
        db.execSQL(CREATE_INDEX_CELULAR);
		db.execSQL(CREATE_TABLE_FINANCEIRO);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_VENDAS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOWNLOADED_IMAGES);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ESTOQUE);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLIENTES);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_FINANCEIRO);

		onCreate(db);
	}

}
