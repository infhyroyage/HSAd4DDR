package com.hsadjustment.ddr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class SetBPMActivity extends FragmentActivity implements OnClickListener {
	// デバッグ用のタグ
	private static final String TAG = "SetBPMActivity";
	// ラジオボタンで頭文字のアルファベットを選択させる項目の文字列
	private static final String abcItems[] = {"数値", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "ひらがな", "カタカナ", "漢字"};
	// ラジオボタンで現在の実質BPMの範囲を選択させる項目の文字列
	private static final String setBPMItems[] = {"100未満", "100以上125未満", "125以上150未満", "150以上175未満", "175以上200未満", "200以上225未満", "225以上250未満", "250以上275未満", "275以上300未満", "300以上"};
	
	// 表の追加部分のレイアウトのインスタンス
	private LinearLayout layoutTable;
	
	// ラジオボタンで1つだけ選択した項目の添え字を保持するインスタンス
	private int chosenItem;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// タイトルバーを非表示
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.set_bpm_layout);
		
		// 表の先頭と、表の追加部分のレイアウトのインスタンスを取得
		LinearLayout layoutTableTop = (LinearLayout)findViewById(R.id.layoutSetBPMTableTop);
		layoutTable = (LinearLayout)findViewById(R.id.layoutSetBPMTable);
		
		// 表のトップに表示する「現在の実質BPM (実質BPMの調整範囲)」の文字列を格納したテキストビューを生成して表に格納
		TextView textTableTop = new TextView(this);
		textTableTop.setText(Html.fromHtml("<font color=\"aqua\">現在の実質BPM</font> <font color=\"white\">(</font><font color=\"lime\">実質BPMの調整可能範囲</font><font color=\"white\">)</font>"));
		layoutTableTop.addView(textTableTop, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		// 2つのソートボタンにリスナーをセット
		Button abcSortBtn = (Button)findViewById(R.id.buttonABCSort);
		Button setBPMSortBtn = (Button)findViewById(R.id.buttonSetBPMSort);
		abcSortBtn.setOnClickListener(this);
		setBPMSortBtn.setOnClickListener(this);
		
		// ログ生成
		Log.d(TAG, TAG + "#onCreate()");
	}
	
	@Override
	public void onClick(View v) {
		// 以前完成した表の情報があればそれをクリアする
		if (layoutTable.getChildCount() != 0) {
			layoutTable.removeAllViews();
		}
		
		// 1つだけラジオボタンで選択させるダイアログのインスタンスを生成
		SingleChoiceDialogFragment dialogFragment;
		// 選択項目の初期添え字を一番最初にセット
		chosenItem = 0;
		
		switch (v.getId()) {
			case R.id.buttonABCSort:
				// ラジオボタンで選択させるダイアログを生成してリスナーを格納
				dialogFragment = SingleChoiceDialogFragment.newInstance(R.string.button_abc_sort, abcItems, 0);
				dialogFragment.setOnSingleChoiceItemsClickListener(new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						chosenItem = item;
					}
				});
				dialogFragment.setOnPositiveButtonClickListener(new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						showTableAfterChoice(R.id.buttonABCSort);
					}
				});
				dialogFragment.show(getSupportFragmentManager(), "dialog_fragment");
				break;
			case R.id.buttonSetBPMSort:
				// ラジオボタンで選択させるダイアログを生成してリスナーを格納
				dialogFragment = SingleChoiceDialogFragment.newInstance(R.string.button_abc_sort, setBPMItems, 0);
				dialogFragment.setOnSingleChoiceItemsClickListener(new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						chosenItem = item;
					}
				});
				dialogFragment.setOnPositiveButtonClickListener(new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						showTableAfterChoice(R.id.buttonSetBPMSort);
					}
				});
				dialogFragment.show(getSupportFragmentManager(), "dialog_fragment");
		}
		
	}
	
	private void showTableAfterChoice(int btnId) {
		// 該当曲データを全て表に格納するリストを生成
		final ArrayList<String> id = new ArrayList<String>();
		final ArrayList<String> kyokumei = new ArrayList<String>();
		final ArrayList<Double> setBPM = new ArrayList<Double>();
		final ArrayList<Double> minBPM = new ArrayList<Double>();
		final ArrayList<Double> maxBPM = new ArrayList<Double>();
		
		// UIスレッドのハンドラの生成
		final Handler mHandler = new Handler();
		
		// 該当の曲をデータベースから探索して表が作成するまで表示するプログレスダイアログを生成して表示
		final SpinningProgressDialogFragment progressDlgFragment = SpinningProgressDialogFragment.newInstance(R.string.please_wait, R.string.searching_and_creating);
		progressDlgFragment.show(getSupportFragmentManager(), "dialog_fragment");
		
		final FragmentActivity thisActivity = this;
		final int finalBtnid = btnId;
		new Thread(new Runnable() {
			public void run() {
				// データベースのオープン
				SQLiteDatabase DDRSongData = new BPMListDBHelper(thisActivity).getReadableDatabase();
				try {
					Cursor cursor;
					switch (finalBtnid) {
						case R.id.buttonABCSort:
							// データベースに対しクエリを実行した結果を曲名の昇順で格納
							cursor = DDRSongData.rawQuery("select id, name, setBPM, minBPM, maxBPM from BPMList where abcType == " + String.valueOf(chosenItem) + " and minBPM != maxBPM order by lower(name);", null);
							// クエリを実行した結果、レコード件数が0件の時以下を実行しない
							if (cursor.moveToFirst()) {
								do {
									// 該当曲のデータをリストに格納する
									id.add(cursor.getString(cursor.getColumnIndex("id")));
									kyokumei.add(cursor.getString(cursor.getColumnIndex("name")));
									setBPM.add(cursor.getDouble(cursor.getColumnIndex("setBPM")));
									minBPM.add(cursor.getDouble(cursor.getColumnIndex("minBPM")));
									maxBPM.add(cursor.getDouble(cursor.getColumnIndex("maxBPM")));
								} while (cursor.moveToNext());
							}
							cursor.close();
							break;
						case R.id.buttonSetBPMSort:
							// データベースに対しクエリを実行した結果を格納
							String queryStr = "select id, name, setBPM, minBPM, maxBPM from BPMList where ";
							if (chosenItem == 0) {
								queryStr += "setBPM < 100";
							} else if (chosenItem == setBPMItems.length - 1) {
								queryStr += "setBPM >= 300";
							} else {
								queryStr += "setBPM >= " + String.valueOf(chosenItem * 25 + 75) + " and setBPM < " + String.valueOf(chosenItem * 25 + 100);
							}
							cursor = DDRSongData.rawQuery(queryStr + " and minBPM != maxBPM order by lower(name);", null);
							// クエリを実行した結果、レコード件数が0件の時以下を実行しない
							if (cursor.moveToFirst()) {
								do {
									// 該当曲のデータをリストに格納する
									id.add(cursor.getString(cursor.getColumnIndex("id")));
									kyokumei.add(cursor.getString(cursor.getColumnIndex("name")));
									setBPM.add(cursor.getDouble(cursor.getColumnIndex("setBPM")));
									minBPM.add(cursor.getDouble(cursor.getColumnIndex("minBPM")));
									maxBPM.add(cursor.getDouble(cursor.getColumnIndex("maxBPM")));
								} while (cursor.moveToNext());
							}
							cursor.close();
					}
				} finally {
					DDRSongData.close();
				}
				
				// 引数のリストのインデックス
				int i = 0;
				// クエリを実行した結果、レコード件数が0件かどうか判定
				if (i != kyokumei.size()) {
					// 1行毎に表にテキストビュー2個を格納したレイアウトを生成して格納
					while (i < kyokumei.size()) {
						final int finalIdx = i;
						
						// 白文字のサイズが18spの曲名用のテキストビューを生成
						TextView textKyokumei = new TextView(thisActivity);
						textKyokumei.setText(Html.fromHtml("<font color=\"white\">" + kyokumei.get(i) + "</font>"));
						textKyokumei.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
						// 「現在の実質BPM (実質BPMの調整範囲)」の文字列を格納したテキストビューを生成
						TextView textBPMInfo = new TextView(thisActivity);
						textBPMInfo.setText(Html.fromHtml("<font color=\"aqua\">" + String.valueOf(setBPM.get(i)) + "</font> <font color=\"white\">(</font><font color=\"lime\">" + String.valueOf(minBPM.get(i)) + "～" + String.valueOf(maxBPM.get(i)) + "</font><font color=\"white\">)</font>"));
						// 上記の2つのテキストビューを格納するレイアウトを生成
						final LinearLayout layoutRowTexts = new LinearLayout(thisActivity);
						layoutRowTexts.setOrientation(LinearLayout.VERTICAL);
						layoutRowTexts.setGravity(Gravity.CENTER);
						layoutRowTexts.addView(textKyokumei, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
						layoutRowTexts.addView(textBPMInfo, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
						
						// エディットテキスト付きダイアログを表示するリスナーをセットした「変更」ボタンの生成
						final Button rowButton = new Button(thisActivity);
						rowButton.setText(R.string.button_change);
						rowButton.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								EditSetBPMDialogFragment.newInstance(id.get(finalIdx), kyokumei.get(finalIdx), setBPM.get(finalIdx), minBPM.get(finalIdx), maxBPM.get(finalIdx)).show(getSupportFragmentManager(), "dialog_fragment");
							}
						});
						
						// 2つのテキストビューを格納したレイアウトと「変更」ボタンを格納した1行分のレイアウトの生成
						final LinearLayout layoutRow = new LinearLayout(thisActivity);
						layoutRow.setOrientation(LinearLayout.HORIZONTAL);
						layoutRow.setBackgroundColor(Color.rgb(51, 51, 51));
						layoutRow.addView(layoutRowTexts, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
						layoutRow.addView(rowButton, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 2.5f));
						
						// 表に格納する1行分のレイアウトのパラメータのインスタンスを生成
						final LayoutParams layoutTablePrm = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						if (i != kyokumei.size() - 1) {
							layoutTablePrm.setMargins(0, 0, 0, (int)getResources().getDisplayMetrics().density);
						} else {
							layoutTablePrm.setMargins(0, 0, 0, 2 * (int)getResources().getDisplayMetrics().density);
						}
						// UIスレッド上の表のレイアウトを1行分更新
						mHandler.post(new Runnable() {
							public void run() {
								layoutTable.addView(layoutRow, layoutTablePrm);
							}
						});
						
						i++;
					}
				} else {
					// クエリを実行してもレコード件数の結果が0件だった旨のトーストを表示
					mHandler.post(new Runnable() {
						public void run() {
							Toast.makeText(thisActivity, "1つも曲がデータベースからヒットしませんでした", Toast.LENGTH_SHORT).show();
						}
					});
				}
				
				mHandler.post(new Runnable() {
					public void run() {
						progressDlgFragment.dismiss();
					}
				});
			}
		}).start();
	}
	
	// assetsフォルダ内にあるbpm_list.dbから、読み込み専用のデータを取得するためのオープンクラス
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
		public synchronized SQLiteDatabase getReadableDatabase() {
			SQLiteDatabase database = super.getReadableDatabase();
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
			return super.getReadableDatabase();
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			super.onOpen(db);
			// getReadableDatabase()したときに呼ばれてくるので、
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
