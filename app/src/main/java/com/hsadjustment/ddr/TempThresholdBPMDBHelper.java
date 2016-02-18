package com.hsadjustment.ddr;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TempThresholdBPMDBHelper extends SQLiteOpenHelper {
	// 一時保存したレイアウト上の表の結果データを格納するデータベースのCREATE TABLE文で使用するSQLの文字列
	protected static final String CREATE_TABLE = "create table thresholdBPM (id integer primary key autoincrement, bairitsu real not null, name text not null, setBPM real not null, minBPM real not null, maxBPM real not null);";
	// 一時保存したレイアウト上の表の結果データを格納するデータベースのDROP TABLE文で使用するSQLの文字列
	protected static final String DROP_TABLE = "drop table thresholdBPM;";
	// DBのバージョン
	private static final int DATABASE_VERSION = 1;
	
	public TempThresholdBPMDBHelper(Context context) {
		super(context, "tempThresholdBPMDB.db", null, DATABASE_VERSION);
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
