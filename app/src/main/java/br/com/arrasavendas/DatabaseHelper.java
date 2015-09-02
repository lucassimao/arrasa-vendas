package br.com.arrasavendas;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import br.com.arrasavendas.providers.DownloadedImagesProvider;

import static br.com.arrasavendas.providers.DownloadedImagesProvider.IMAGE_NAME;
import static br.com.arrasavendas.providers.DownloadedImagesProvider.PRODUTO_ID;
import static br.com.arrasavendas.providers.DownloadedImagesProvider.UNIDADE;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "ArrasaVendas";
	private static final int DATABASE_VERSION = 35;

	public static final String TABLE_VENDAS = "VENDAS";
	private static final String DATABASE_CREATE_TABLE_VENDAS = "create table "
			+ TABLE_VENDAS
			+ "(_id integer primary key, vendedor_id integer not null, carrinho text not null,"
			+ " data_entrega integer not null, forma_pagamento text null,status text null,turno text null, cliente text not null, remote_id integer);";

	public static final String TABLE_ESTOQUE = "ESTOQUE";
	private static final String DATABASE_CREATE_TABLE_ESTOQUE = "create table "
			+ TABLE_ESTOQUE
			+ "(_id integer primary key AUTOINCREMENT, produto_nome text not null, produto_nome_ascii text not null," +
			"produto_id integer not null,prevoAVista REAL,prevoAPrazo REAL, " +
			"unidade text not null, quantidade integer not null);";

    public static final String TABLE_DOWNLOADED_IMAGES = "DOWNLOADED_IMAGES";
	private static final String DATABASE_CREATE_TABLE_DOWNLOADED_IMAGES = "create table "
			+ TABLE_DOWNLOADED_IMAGES
			+ "(_id integer primary key AUTOINCREMENT, produto_id integer not null, produto text,produto_ascii text," +
			"image_name text not null,local_path text, unidade text not null,is_ignored integer," +
			"UNIQUE (image_name,unidade,produto_id) ON CONFLICT IGNORE)";

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE_TABLE_VENDAS);
		db.execSQL(DATABASE_CREATE_TABLE_ESTOQUE);
		db.execSQL(DATABASE_CREATE_TABLE_DOWNLOADED_IMAGES);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_VENDAS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOWNLOADED_IMAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ESTOQUE);
		onCreate(db);
	}

}
