package com.hsadjustment.ddr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class EditSetBPMDialogFragment extends DialogFragment {
	// デバッグ用のタグ
	private static final String TAG = "EditSetBPMDialogFragment";
	
	// このダイアログのエディットテキスト
	private EditText editText;
	
	// ID、曲名、変更前実質BPM、最小BPM、最大BPMの保持
	private String id, kyokumei;
	private double oldSetBPM, minBPM, maxBPM;
	
	public static EditSetBPMDialogFragment newInstance(String id, String kyokumei, double setBPM, double minBPM, double maxBPM) {
		EditSetBPMDialogFragment setBPMFragment = new EditSetBPMDialogFragment();
		
		Bundle bundle = new Bundle();
		bundle.putString("dlgId", id);
		bundle.putString("dlgKyokumei", kyokumei);
		bundle.putDouble("dlgSetBPM", setBPM);
		bundle.putDouble("dlgMinBPM", minBPM);
		bundle.putDouble("dlgMaxBPM", maxBPM);
		setBPMFragment.setArguments(bundle);
		
		return setBPMFragment;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle safedInstanceState) {
		// ID、最小、最大BPMをそれぞれ取得
		id = getArguments().getString("dlgId");
		kyokumei = getArguments().getString("dlgKyokumei");
		oldSetBPM = getArguments().getDouble("dlgSetBPM");
		minBPM = getArguments().getDouble("dlgMinBPM");
		maxBPM = getArguments().getDouble("dlgMaxBPM");
		
		// エディットテキストのインスタンスを生成し、デフォルト値に変更前の実質BPMをセット
		editText = new EditText(getActivity());
		editText.setText(String.valueOf(oldSetBPM));
		editText.setInputType(InputType.TYPE_CLASS_NUMBER);
		
		// ダイアログを生成
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(kyokumei)
		.setMessage(String.valueOf(minBPM) + "～" + String.valueOf(maxBPM) + "の実数値を入力して実質BPMを変更して下さい。")
		.setView(editText)
		.setPositiveButton("変更", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// onCreateDialog()の地点では、リスナーの動作を何も記述しないでおく(onStart()で再記述)
			}
		}).setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// このダイアログを消すだけなので、リスナーの動作を何も記述しない
			}
		});
		
		// ログ出力
		Log.d(TAG, TAG + "#onCreateDialog()");
		
		return builder.create();
	}
	
	@Override
	public void onStart() {
		// NOTE : super.onStart()でdialog.show()が実行されるため、その後にこのダイアログの「変更」ボタンのリスナーを再セットする
		super.onStart();
		// このダイアログの取得
		AlertDialog thisDialog = (AlertDialog)getDialog();
		if (thisDialog != null) {
			// 「変更」ボタンの取得
			Button positiveBtn = (Button)thisDialog.getButton(Dialog.BUTTON_POSITIVE);
			positiveBtn.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					try {
						// エディットテキストに記述された変更後の実質BPMを取得し、最小BPM～最大BPMの範囲に存在するかどうかチェック
						double setBPM = Double.parseDouble(editText.getText().toString());
						if (setBPM >= minBPM && setBPM <= maxBPM) {
							// データベースのオープン
							SQLiteDatabase DDRSongData = new BPMListDBHelper(getActivity()).getWritableDatabase();
							try {
								DDRSongData.execSQL("update BPMList set setBPM = " + String.valueOf(setBPM) + " where id = " + String.valueOf(id) + ";");
							} finally {
								DDRSongData.close();
							}
							
							// 以前完成した表の情報をクリアする
							LinearLayout layoutTable = (LinearLayout)getActivity().findViewById(R.id.layoutSetBPMTable);
							layoutTable.removeAllViews();
							
							// 正常に実質BPMが変更できた旨のトーストを表示
							Toast.makeText(getActivity(), kyokumei + "の実質BPMが正常に変更しました！\n" + String.valueOf(oldSetBPM) + " → " + String.valueOf(setBPM) + "\n\n別の曲の実質BPMを変更したい場合は、もう一度ソートしてから変更して下さい", Toast.LENGTH_LONG).show();
							
							// 一時保存した閾値表の結果データを格納するデータベースを取得し、それらのデータをすべて消去する
							SQLiteDatabase tempDb = new TempThresholdBPMDBHelper(getActivity()).getWritableDatabase();
							try {
								tempDb.execSQL(TempThresholdBPMDBHelper.DROP_TABLE);
								tempDb.execSQL(TempThresholdBPMDBHelper.CREATE_TABLE);
							} finally {
								tempDb.close();
							}
							
							// ダイアログの消去
							dismiss();
						} else {
							// ログ出力
							Log.w(TAG, "OutOfRange -> " + TAG + "#onStart()#onClick() : setBPM = " + String.valueOf(setBPM));
							// 最小BPM～最大BPMの範囲に変更後の実質BPMが存在していない旨のトーストを表示
							Toast.makeText(getActivity(), "入力数値が" + String.valueOf(minBPM) + "～" + String.valueOf(maxBPM) + "の範囲外です", Toast.LENGTH_SHORT).show();
						}
					} catch (NumberFormatException e) {
						// ログ出力
						Log.w(TAG, "NumberFormatException -> " + TAG + "#onStart()#onClick() : setBPM");
						// 数値以外の文字列を入力している旨のトーストを表示
						Toast.makeText(getActivity(), "数値が入力されていません", Toast.LENGTH_SHORT).show();
					}
				}
			});
		}
	}
	
	// assetsフォルダ内にあるbpm_list.dbから、データを取得するためのオープンクラス
	private class BPMListDBHelper extends SQLiteOpenHelper {
		// assetsフォルダにあるbpm_list.dbのファイル名
		private static final String SRC_DATABASE_NAME = "DDRSongData.db";
		// コピー先のDB名
		private static final String DATABASE_NAME = "DDRSongData.db.db";
		// エラー用ログのタグ名
		private static final String DATABASE_TAG = "BPMListDBHelper";
		// DBのバージョン
		private static final int DATABASE_VERSION = 1;
		
		private final Context context;
		private final File databasePath;
		private boolean createDatabase = false;
		
		public BPMListDBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			
			this.context = context;
			this.databasePath = context.getDatabasePath(DATABASE_NAME);
		}
		
		@Override
		public synchronized SQLiteDatabase getWritableDatabase() {
			SQLiteDatabase database = super.getWritableDatabase();
			if (createDatabase) {
				try {
					database = copyDatabase(database);
				} catch (IOException e) {
					Log.wtf(DATABASE_TAG, e);
				}
			}
			return database;
		}
		
		private SQLiteDatabase copyDatabase(SQLiteDatabase database) throws IOException {
			// dbがひらきっぱなしなので、書き換えできるように閉じる
			database.close();
			// コピー！
			InputStream input = context.getAssets().open(SRC_DATABASE_NAME);
			OutputStream output = new FileOutputStream(databasePath);
			copy(input, output);
			createDatabase = false;
			// dbを閉じたので、また開く
			return super.getWritableDatabase();
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			super.onOpen(db);
			// getWritableDatabase()したときに呼ばれてくるので、
			// 初期化する必要があることを保存する
			this.createDatabase = true;
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onUpgrade(db, oldVersion, newVersion);
		}
		
		// CopyUtilsからのコピペ
		private int copy(InputStream input, OutputStream output) throws IOException {
			byte[] buffer = new byte[1024 * 4];
			int count = 0;
			int n = 0;
			while (-1 != (n = input.read(buffer))) {
				output.write(buffer, 0, n);
				count += n;
			}
			return count;
		}
	}
	
}
