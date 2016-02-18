package com.hsadjustment.ddr;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TempTekiseiDBHelper extends SQLiteOpenHelper {
	// 一時保存したレイアウト上の表の結果データを格納するデータベースのCREATE TABLE文で使用するSQLの文字列
	protected static final String CREATE_TABLE = "create table tekisei (id integer primary key autoincrement, bairitsu real not null, min integer not null, max integer not null);";
	// 一時保存したレイアウト上の表の結果データを格納するデータベースのDROP TABLE文で使用するSQLの文字列
	protected static final String DROP_TABLE = "drop table tekisei;";
	// DBのバージョン
	private static final int DATABASE_VERSION = 1;
	
	public TempTekiseiDBHelper(Context context) {
		super(context, "tempTekiseiDB.db", null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		resetDb(db);
	}
	
	public void resetDb(SQLiteDatabase db) {
		db.execSQL(DROP_TABLE);
		db.execSQL(CREATE_TABLE);
	}
}
