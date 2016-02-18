package com.hsadjustment.ddr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class ThresholdBPMTableFragment extends Fragment implements OnClickListener {
	// デバッグ用のタグ
	private static final String TAG = "ThresholdBPMTableFragment";
	
	// 一時保存したレイアウト上の表の結果データを格納するデータベース
	private static SQLiteDatabase tempDb;
	
	// UIスレッドのハンドラ
	private static Handler mHandler;
	
	// 設定情報を格納するインスタンス
	private static SharedPreferences pref;
	
	// 閾値表の説明文のテキストビューを格納するレイアウトのインスタンス
	private LinearLayout layoutExplanation;
	// 閾値表の追加部分のレイアウトのインスタンス
	private LinearLayout layoutTable;
	// 現在のBPM設定値を表示するテキストビューのインスタンス
	private TextView currentBPM;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.threshold_bpm_table_fragment, container, false);
		
		// 閾値表の説明文のテキストビューを格納するレイアウトを格納
		layoutExplanation = (LinearLayout)view.findViewById(R.id.layoutThresholdBPMTableExplanation);
		
		// 閾値表の追加部分のレイアウトを格納
		layoutTable = (LinearLayout)view.findViewById(R.id.layoutTableThresholdBPM);
		
		// 現在のBPM設定値を表示するテキストビューを格納
		currentBPM = (TextView)view.findViewById(R.id.viewThresholdBPMCurrent);
		
		// 閾値表のトップにある「BPM(実際のBPM) → 調整後BPM(実際の調整後BPM)」のテキストビューをHTML形式で表示
		TextView textTopBPM = new TextView(getActivity());
		textTopBPM.setText(Html.fromHtml("<font color=\"white\">BPM(</font><font color=\"aqua\">実質BPM</font><font color=\"white\">) → 調整後BPM(</font><font color=\"yellow\">調整後実質BPM</font><font color=\"white\">)</font>"));
		LinearLayout topLayoutBPM = (LinearLayout)view.findViewById(R.id.topLayoutThresholdBPM);
		topLayoutBPM.addView(textTopBPM, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		// 実行、クリアボタンにリスナーをセット
		Button runBtn = (Button)view.findViewById(R.id.buttonRunThresholdBPMTable);
		Button clearBtn = (Button)view.findViewById(R.id.buttonClearThresholdBPMTable);
		runBtn.setOnClickListener(this);
		clearBtn.setOnClickListener(this);
		
		// UIスレッドのハンドラの生成
		mHandler = new Handler();
		
		// 設定情報を格納するインスタンスの生成
		pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		
		// ログ出力
		Log.d(TAG, TAG + "#onCreateView()");
		
		return view;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		// 説明文を非表示に設定している場合は、そのテキストビューを格納するレイアウトを非表示にして詰める
		if (Integer.parseInt(pref.getString("is_explanation", "1")) == 0) {
			layoutExplanation.setVisibility(View.GONE);
		}
		
		// 設定にあるBPM適正値とBPMの補正値の情報を取得
		double tekisei = 0.0;
		double correctionBelowValue = -1.0;
		double correctionUpperValue = -1.0;
		try {
			// 初回起動時のようにメモリが存在しない状態ではデフォルト値の400.0、5.0、5.0が取得される
			tekisei = Double.parseDouble(pref.getString("bpm_tekisei", "400.0"));
			correctionBelowValue = Double.parseDouble(pref.getString("correction_below_value", "5.0"));
			correctionUpperValue = Double.parseDouble(pref.getString("correction_upper_value", "5.0"));
		} catch (NumberFormatException e) {
			Log.w(TAG, "NumberFormatException -> " + TAG + "#onStart() : bpm_tekisei or correction_below_value or correction_upper_value");
		}
		// 現在のBPM設定値を表示するテキストビューを文字サイズ18spで格納
		if (tekisei > 0 && correctionBelowValue >= 0.0 && correctionUpperValue >= 0.0) {
			currentBPM.setText(Html.fromHtml("<font color=\"white\">現在のBPM適正範囲 : </font><font color=\"yellow\">" + String.format("%.2f", tekisei - correctionBelowValue) + "～" + String.format("%.2f", tekisei + correctionUpperValue) + "</font>"));
		} else {
			currentBPM.setText(Html.fromHtml("<font color=\"white\">現在のBPM適正範囲 : </font><font color=\"red\">不正な値</font>"));
		}
		currentBPM.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		
		// 一時保存したレイアウト上の表の結果データを格納するデータベースを取得後、それを復元
		tempDb = new TempThresholdBPMDBHelper(getActivity()).getWritableDatabase();
		final Cursor cursor = tempDb.rawQuery("select bairitsu, name, setBPM, minBPM, maxBPM from thresholdBPM;", null);
		// クエリを実行した結果、レコード件数が0件の時以下を実行しない
		if (cursor.moveToFirst()) {
			// 全ての調整表用の数値データを格納するリストを生成
			final ArrayList<Double> highSpeed = new ArrayList<Double>();
			final ArrayList<String> kyokumei = new ArrayList<String>();
			final ArrayList<Double> setBPM = new ArrayList<Double>();
			final ArrayList<Double> minBPM = new ArrayList<Double>();
			final ArrayList<Double> maxBPM = new ArrayList<Double>();
			
			do {
				// 1件毎のレコードからデータをリストに格納する
				highSpeed.add(cursor.getDouble(cursor.getColumnIndex("bairitsu")));
				kyokumei.add(cursor.getString(cursor.getColumnIndex("name")));
				setBPM.add(cursor.getDouble(cursor.getColumnIndex("setBPM")));
				minBPM.add(cursor.getDouble(cursor.getColumnIndex("minBPM")));
				maxBPM.add(cursor.getDouble(cursor.getColumnIndex("maxBPM")));
			} while (cursor.moveToNext());
			
			updateTable(highSpeed, kyokumei, setBPM, minBPM, maxBPM, true);
		}
		cursor.close();
		
		// ログ出力
		Log.d(TAG, TAG + "#onStart()");
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		// メモリリークを避けるため、一時保存したレイアウト上の閾値表の結果データを格納するデータベースをクローズする
		tempDb.close();
		
		// 以前完成した表の情報があればそれをクリアする
		if (layoutTable.getChildCount() != 0) {
			layoutTable.removeAllViews();
		}
		
		// ログ出力
		Log.d(TAG, TAG + "#onStop()");
	}
	
	@SuppressLint("HandlerLeak")
	@Override
	public void onClick(View v) {
		// 以前完成した閾値表の情報があればそれをクリアする
		if (layoutTable.getChildCount() != 0) {
			layoutTable.removeAllViews();
		}
		// 一時保存したレイアウト上の表の結果データもクリアする
		tempDb.execSQL(TempThresholdBPMDBHelper.DROP_TABLE);
		tempDb.execSQL(TempThresholdBPMDBHelper.CREATE_TABLE);
		
		switch (v.getId()) {
			case R.id.buttonRunThresholdBPMTable:
				try {
					// 設定にあるBPM適正値とBPMの補正値の情報を取得
					final double tekisei = Double.parseDouble(pref.getString("bpm_tekisei", "400"));
					final double correctionBelowValue = Double.parseDouble(pref.getString("correction_below_value", "5"));
					final double correctionUpperValue = Double.parseDouble(pref.getString("correction_upper_value", "5"));
					
					// 3つの設定値が正常に入力されているかチェック
					if (tekisei > 0 && correctionBelowValue >= 0.0 && correctionUpperValue >= 0.0) {
						// PASELI使用状態の設定と、それによる倍率を決定して格納
						final double bairitsu[];
						if (Integer.parseInt(pref.getString("is_paseli", "0")) == 1) {
							bairitsu = new double[]{0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 2.5, 2.75, 3.0, 3.25, 3.5, 3.75, 4.0, 4.5, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0};
						} else {
							bairitsu = new double[]{1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0};
						}
						
						// 全ての閾値表用の該当曲データを格納するリストを生成
						final ArrayList<Double> highSpeed = new ArrayList<Double>();
						final ArrayList<String> kyokumei = new ArrayList<String>();
						final ArrayList<Double> setBPM = new ArrayList<Double>();
						final ArrayList<Double> minBPM = new ArrayList<Double>();
						final ArrayList<Double> maxBPM = new ArrayList<Double>();
						
						// 探索中ダイアログの生成後、表示する
						final HorizontalProgressDialogFragment progressDlgFragment = HorizontalProgressDialogFragment.newInstance(R.string.please_wait, R.string.calcuating_tekiseiBPM, bairitsu.length);
						progressDlgFragment.show(getFragmentManager(), "dialog_fragment");
						
						// 後に別スレッドに作るこのデータベースをクローズするために、このメソッドが必要
						tempDb.acquireReference();
						
						// 該当曲を探索していくスレッド内の動作の生成後、実行
						new Thread(new Runnable() {
							public void run() {
								// High Speedのそれぞれの値に応じてデータベースを起動して該当する曲があれば追加していく
								int idIdx = 1;
								for (int i = 0; i < bairitsu.length; i++) {
									final int idx = i;
									// ベータベースのオープン
									SQLiteDatabase DDRSongData = new BPMListDBHelper(getActivity()).getReadableDatabase();
									try {
										// データベースに対しクエリを実行した結果を曲名の昇順で格納
										final Cursor cursor = DDRSongData.rawQuery("select name, setBPM, minBPM, maxBPM from BPMList where setBPM >= " + String.valueOf((tekisei - correctionBelowValue) / bairitsu[i]) + " and setBPM <= " + String.valueOf((tekisei + correctionUpperValue) / bairitsu[i]) + " order by lower(name);", null);
										// クエリを実行した結果、レコード件数が0件の時以下を実行しない
										if (cursor.moveToFirst()) {
											do {
												// 該当曲のデータをリストに格納する
												String name = cursor.getString(cursor.getColumnIndex("name"));
												Double set = cursor.getDouble(cursor.getColumnIndex("setBPM"));
												Double min = cursor.getDouble(cursor.getColumnIndex("minBPM"));
												Double max = cursor.getDouble(cursor.getColumnIndex("maxBPM"));
												highSpeed.add(bairitsu[i]);
												kyokumei.add(name);
												setBPM.add(set);
												minBPM.add(min);
												maxBPM.add(max);
												// 上記のデータを一時保存用のデータベースに格納
												// NOTE : SQLiteは文字列中に含まれるシングルクォーテーションをエスケープ出来ないので2個重ねるようにする
												tempDb.execSQL("insert into thresholdBPM values (" + String.valueOf(idIdx) + ", " + String.valueOf(bairitsu[i]) + ", '" + name.replaceAll("'", "''") + "', " + String.valueOf(set) + ", " + String.valueOf(min) + ", " + String.valueOf(max) + ");");
												idIdx++;
											} while (cursor.moveToNext());
										}
										cursor.close();
									} finally {
										DDRSongData.close();
									}
									
									// 探索中ダイアログの進捗バーのカウントを1個分増加
									mHandler.post(new Runnable() {
										public void run() {
											progressDlgFragment.setProgress(idx + 1);
										}
									});
								}
								
								// メモリリークを避けるため、一時保存したレイアウト上の表の結果データを格納する別スレッド上のデータベースをクローズする
								tempDb.close();
								
								// UIスレッド上の探索中ダイアログを消去し、閾値表を新しく更新
								mHandler.post(new Runnable() {
									public void run() {
										progressDlgFragment.dismiss();
										updateTable(highSpeed, kyokumei, setBPM, minBPM, maxBPM, false);
									}
								});
							}
						}).start();
					} else {
						// BPM適正範囲が正常に入力されていない旨のアラートダイアログを表示
						OKDialogFragment.newInstance(R.string.title_tekiseiRange_error, R.string.message_tekiseiRange_error).show(getFragmentManager(), "dialog_fragment");
					}
				} catch (NumberFormatException e) {
					// ログ出力
					Log.w(TAG, "NumberFormatException -> " + TAG + "#onClick() : bpm_tekisei or correction_below_value or correction_upper_value");
					
					// 3つのBPM値のどれかが数値以外の文字列を入力している旨のアラートダイアログを表示
					OKDialogFragment.newInstance(R.string.title_numberFormat_error, R.string.message_thresholdBPMNumberFormat_error).show(getFragmentManager(), "dialog_fragment");
				}
				break;
			case R.id.buttonClearThresholdBPMTable:
				// 正常に表がクリアされた旨のトーストを表示
				Toast.makeText(getActivity(), "ハイスピ閾値表がクリアされました", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void updateTable(final List<Double> highSpeed, final List<String> kyokumei, final List<Double> setBPM, final List<Double> minBPM, final List<Double> maxBPM, boolean isRestoring) {
		// 生成or復元中ダイアログの生成後、表示する(isRestoringがtrueの時は「復元中」、falseの時は「生成中」)
		int dlgMsg = R.string.creating_layout;
		if (isRestoring) {
			dlgMsg = R.string.restoring_layout;
		}
		final HorizontalProgressDialogFragment progressDlgFragment = HorizontalProgressDialogFragment.newInstance(R.string.please_wait, dlgMsg, highSpeed.size());
		progressDlgFragment.show(getFragmentManager(), "dialog_fragment");
		
		// 閾値表を更新していくスレッド内の動作の生成後、実行
		new Thread(new Runnable() {
			public void run() {
				// 引数のリストのインデックス
				int i = 0;
				// クエリを実行した結果、レコード件数が0件かどうか判定
				if (i != highSpeed.size()) {
					// 新しいHigh Speedの倍率のテキストビューを格納するレイアウトのマージンをセット
					final LayoutParams layoutPrmHS = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					layoutPrmHS.setMargins(0, 0, 0, (int)getResources().getDisplayMetrics().density);
					// 新しいHigh Speedでの複数の曲データを格納するレイアウトのマージンをセット
					final LayoutParams layoutPrmSongs = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					layoutPrmSongs.setMargins(0, 0, 0, 2 * (int)getResources().getDisplayMetrics().density);
					// 各「BPM(実際のBPM) → 調整後BPM(実際の調整後BPM)」のテキストビューのマージンをセット
					LayoutParams layoutPrmTextBPM = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					layoutPrmTextBPM.setMargins(0, 0, 0, (int)getResources().getDisplayMetrics().density);
					
					// High Speedのそれぞれの値に応じてデータベースを起動して該当する曲があれば追加していく
					while (i < highSpeed.size()) {
						// 緑文字のサイズが18spのHigh Speed用の倍率のテキストビューをレイアウトに追加
						TextView textHS = new TextView(getActivity());
						textHS.setText("×" + String.valueOf(highSpeed.get(i)));
						textHS.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
						textHS.setTextColor(Color.rgb(0, 255, 0));
						
						// 新しいHigh Speedの倍率のテキストビューを格納するレイアウトを生成して格納
						final LinearLayout layoutHS = new LinearLayout(getActivity());
						layoutHS.setOrientation(LinearLayout.VERTICAL);
						layoutHS.setBackgroundColor(Color.rgb(51, 51, 51));
						layoutHS.addView(textHS, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
						
						// 新しいHigh Speedでの複数の曲データを格納するレイアウトを生成
						final LinearLayout layoutSongs = new LinearLayout(getActivity());
						layoutSongs.setOrientation(LinearLayout.VERTICAL);
						layoutSongs.setBackgroundColor(Color.rgb(51, 51, 51));
						layoutSongs.setGravity(Gravity.CENTER);
						
						// 現在のHigh Speedの倍率を一時格納する文字列の生成
						double tmpHS = highSpeed.get(i);
						
						while (tmpHS == highSpeed.get(i)) {
							// 白文字のサイズが18spの曲名用のテキストビューを生成し、新しいHigh Speedでの複数の曲データを格納するレイアウトに追加
							TextView textKyokumei = new TextView(getActivity());
							textKyokumei.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
							// SQLiteでエスケープできないシングルクォーテーション2個を1個に置換
							textKyokumei.setText(Html.fromHtml("<font color=\"white\">" + kyokumei.get(i).replaceAll("''", "'") + "</font>"));
							layoutSongs.addView(textKyokumei, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
							
							// 「BPM(実際のBPM) → 調整後BPM(実際の調整後BPM)」のテキストビューを生成
							TextView textBPM = new TextView(getActivity());
							// 「BPM(実際のBPM) → 調整後BPM(実際の調整後BPM)」のテキストビューの文字列を決定してセット
							if (minBPM.get(i) == maxBPM.get(i)) {
								textBPM.setText(Html.fromHtml("<font color=\"white\">" + setBPM.get(i) + "(</font><font color=\"aqua\">" + setBPM.get(i) + "</font><font color=\"white\">) → " + String.format("%.2f", setBPM.get(i) * highSpeed.get(i)) + "(</font><font color=\"yellow\">" + String.format("%.2f", setBPM.get(i) * highSpeed.get(i)) + "</font><font color=\"white\">)</font>"));
							} else {
								textBPM.setText(Html.fromHtml("<font color=\"white\">" + minBPM.get(i) + "-" + maxBPM.get(i) + "(</font><font color=\"aqua\">" + setBPM.get(i) + "</font><font color=\"white\">) → " + String.format("%.2f", minBPM.get(i) * highSpeed.get(i)) + "-" + String.format("%.2f", maxBPM.get(i) * highSpeed.get(i)) + "(</font><font color=\"yellow\">" + String.format("%.2f", setBPM.get(i) * highSpeed.get(i)) + "</font><font color=\"white\">)</font>"));
							}
							// 「BPM(実際のBPM) → 調整後BPM(実際の調整後BPM)」のテキストビューを追加用のレイアウトに追加
							layoutSongs.addView(textBPM, layoutPrmTextBPM);
							
							// 引数のリストのインデックスをインクリメント
							i++;
							
							// UIスレッド上の生成中ダイアログの進捗バーのカウントを1個分増加
							final int idx = i;
							mHandler.post(new Runnable() {
								public void run() {
									progressDlgFragment.setProgress(idx + 1);
								}
							});
							
							// 引数のリストのインデックスが終端を過ぎていたらbreak
							if (i == highSpeed.size()) break;
						}
						
						// UIスレッド上の閾値表のレイアウトを更新
						mHandler.post(new Runnable() {
							public void run() {
								layoutTable.addView(layoutHS, layoutPrmHS);
								layoutTable.addView(layoutSongs, layoutPrmSongs);
							}
						});
					}
				} else {
					// クエリを実行してもレコード件数の結果が0件だった旨のトーストを表示
					mHandler.post(new Runnable() {
						public void run() {
							Toast.makeText(getActivity(), "1つも曲がデータベースからヒットしませんでした", Toast.LENGTH_SHORT).show();
						}
					});
				}
				
				// UIスレッド上の生成中ダイアログを消去する
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
