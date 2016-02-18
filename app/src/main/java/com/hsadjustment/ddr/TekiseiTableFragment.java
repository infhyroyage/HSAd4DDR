package com.hsadjustment.ddr;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class TekiseiTableFragment extends Fragment implements OnClickListener {
	// デバッグ用のタグ
	private static final String TAG = "TekiseiTableFragment";
	
	// 一時保存したレイアウト上の表の結果データを格納するデータベース
	private static SQLiteDatabase tempDb;
	
	// UIスレッドのハンドラ
	private static Handler mHandler;
	
	// 設定情報を格納するインスタンス
	private static SharedPreferences pref;
	
	// 調整表の説明文のテキストビューを格納するレイアウトのインスタンス
	private LinearLayout layoutExplanation;
	// 調整表の各列の先頭のテキストビューと追加部分のレイアウトを格納するインスタンス
	private LinearLayout layoutHS, layoutGaitou, layoutTyousei;
	// 調整表の各列の追加部分のレイアウトのインスタンス
	private LinearLayout rowHS, rowGaitou, rowTyousei;
	// 現在のBPM設定値と現在の表示対象BPM範囲を表示するテキストビュー
	private TextView currentBPM, currentRange;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.tekisei_table_fragment, container, false);
		
		// 調整表の説明文のテキストビューを格納するレイアウトを格納
		layoutExplanation = (LinearLayout)view.findViewById(R.id.layoutTekiseiTableExplanation);
		
		// 調整表の各列の先頭のテキストビューと追加部分のレイアウトを格納するインスタンスを生成
		layoutHS = (LinearLayout)view.findViewById(R.id.layoutTekiseiTableHS);
		layoutGaitou = (LinearLayout)view.findViewById(R.id.layoutTekiseiTableGaitou);
		layoutTyousei = (LinearLayout)view.findViewById(R.id.layoutTekiseiTableTyousei);
		
		// 調整表の追加部分のレイアウトを生成
		rowHS = new LinearLayout(getActivity());
		rowGaitou = new LinearLayout(getActivity());
		rowTyousei = new LinearLayout(getActivity());
		rowHS.setBackgroundColor(Color.rgb(51, 51, 51));
		rowGaitou.setBackgroundColor(Color.rgb(51, 51, 51));
		rowTyousei.setBackgroundColor(Color.rgb(51, 51, 51));
		rowHS.setOrientation(LinearLayout.VERTICAL);
		rowGaitou.setOrientation(LinearLayout.VERTICAL);
		rowTyousei.setOrientation(LinearLayout.VERTICAL);
		
		// 実行、クリアボタンにリスナーをセット
		Button runBtn = (Button)view.findViewById(R.id.buttonRunTekiseiTable);
		Button clearBtn = (Button)view.findViewById(R.id.buttonClearTekiseiTable);
		runBtn.setOnClickListener(this);
		clearBtn.setOnClickListener(this);
		
		// 現在のBPM設定値を表示するテキストビューを格納
		currentBPM = (TextView)view.findViewById(R.id.viewTekiseiTableCurrentBPM);
		
		// 現在の表示対象BPM範囲を表示するテキストビューを格納
		currentRange = (TextView)view.findViewById(R.id.viewTekiseiTableCurrentRange);
		
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
		
		// BPM適正値の設定情報を取得
		double tekisei = 0.0;
		try {
			// 初回起動時のようにメモリが存在しない状態ではデフォルト値の400.0が取得される
			tekisei = Double.parseDouble(pref.getString("bpm_tekisei", "400.0"));
		} catch (NumberFormatException e) {
			Log.w(TAG, "NumberFormatException -> " + TAG + "#onStart() : bpm_tekisei");
		}
		// 現在のBPM設定値を表示するテキストビューにその情報を文字サイズ18spでセット
		if (tekisei > 0.0) {
			currentBPM.setText(Html.fromHtml("<font color=\"white\">現在のBPM適正値 : </font><font color=\"yellow\">" + String.format("%.2f", tekisei) + "</font>"));
		} else {
			currentBPM.setText(Html.fromHtml("<font color=\"white\">現在のBPM適正値 : </font><font color=\"red\">不正な値</font>"));
		}
		currentBPM.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		
		// 表示対象BPM範囲の設定情報を取得
		int bpmMin = 0;
		int bpmMax = 0;
		try {
			// 初回起動時のようにメモリが存在しない状態ではデフォルト値の65、600が取得される
			bpmMin = Integer.parseInt(pref.getString("bpm_min", "65"));
			bpmMax = Integer.parseInt(pref.getString("bpm_max", "600"));
		} catch (NumberFormatException e) {
			Log.w(TAG, "NumberFormatException -> " + TAG + "#onStart() : bpm_min or bpm_max");
		}
		// 現在の表示対象BPM範囲を表示するテキストビューにその情報を文字サイズ18spでセット
		if (bpmMin > 0 && bpmMax > 0 && bpmMin <= bpmMax) {
			currentRange.setText(Html.fromHtml("<font color=\"white\">現在の表示対象BPM範囲 : </font><font color=\"aqua\">" + String.valueOf(bpmMin) + "～" + String.valueOf(bpmMax) + "</font>"));
		} else {
			currentRange.setText(Html.fromHtml("<font color=\"white\">現在の表示対象BPM範囲 : </font><font color=\"red\">不正な値</font>"));
		}
		currentRange.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		
		// 一時保存したレイアウト上の表の結果データを格納するデータベースを取得後、それを復元
		tempDb = new TempTekiseiDBHelper(getActivity()).getWritableDatabase();
		final Cursor cursor = tempDb.rawQuery("select bairitsu, min, max from tekisei;", null);
		// クエリを実行した結果、レコード件数が0件の時以下を実行しない
		if (cursor.moveToFirst()) {
			// 全ての調整表用の数値データを格納するリストを生成
			final ArrayList<String> highSpeed = new ArrayList<String>();
			final ArrayList<Integer> gaitouMin = new ArrayList<Integer>();
			final ArrayList<Integer> gaitouMax = new ArrayList<Integer>();
			final ArrayList<Double> tyouseiMin = new ArrayList<Double>();
			final ArrayList<Double> tyouseiMax = new ArrayList<Double>();
			
			do {
				// 1件毎のレコードからデータを取得
				final double bairitsu = cursor.getDouble(cursor.getColumnIndex("bairitsu"));
				final int min = cursor.getInt(cursor.getColumnIndex("min"));
				final int max = cursor.getInt(cursor.getColumnIndex("max"));
				// 該当曲のデータをリストに格納する
				if (bairitsu == -1.0) {
					highSpeed.add("調整不可");
					gaitouMin.add(min);
					gaitouMax.add(max);
					tyouseiMin.add(-1.0);
					tyouseiMax.add(-1.0);
				} else {
					highSpeed.add("×" + String.valueOf(bairitsu));
					gaitouMin.add(min);
					gaitouMax.add(max);
					tyouseiMin.add(min * bairitsu);
					tyouseiMax.add(max * bairitsu);
				}
			} while (cursor.moveToNext());
			
			updateTable(highSpeed, gaitouMin, gaitouMax, tyouseiMin, tyouseiMax, true);
		}
		cursor.close();
		
		// ログ出力
		Log.d(TAG, TAG + "#onStart()");
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		// メモリリークを避けるため、一時保存したレイアウト上の調整表の結果データを格納するデータベースをクローズする
		tempDb.close();
		
		// 以前完成した調整表の情報があればそれをクリアする
		if (rowHS.getChildCount() > 0) {
			rowHS.removeAllViews();
			rowGaitou.removeAllViews();
			rowTyousei.removeAllViews();
			layoutHS.removeViewAt(1);
			layoutGaitou.removeViewAt(1);
			layoutTyousei.removeViewAt(1);
		}
		
		// ログ出力
		Log.d(TAG, TAG + "#onStop()");
	}
	
	@Override
	public void onClick(View v) {
		// 以前完成した調整表の情報があればそれをクリアする
		if (rowHS.getChildCount() > 0) {
			rowHS.removeAllViews();
			rowGaitou.removeAllViews();
			rowTyousei.removeAllViews();
			layoutHS.removeViewAt(1);
			layoutGaitou.removeViewAt(1);
			layoutTyousei.removeViewAt(1);
		}
		// 一時保存したレイアウト上の調整表の結果データもクリアする
		tempDb.execSQL(TempTekiseiDBHelper.DROP_TABLE);
		tempDb.execSQL(TempTekiseiDBHelper.CREATE_TABLE);
		
		switch (v.getId()) {
			case R.id.buttonRunTekiseiTable:
				try {
					// 設定にある3つのBPMの情報を取得し、もし初回起動時のようにメモリが存在しない状態だと、デフォルト値の400.0、65、600が取得される
					final double tekisei = Double.parseDouble(pref.getString("bpm_tekisei", "400.0"));
					final int min = Integer.parseInt(pref.getString("bpm_min", "65"));
					final int max = Integer.parseInt(pref.getString("bpm_max", "600"));
					
					// 3つの設定値が正常に入力されているかチェック
					if (tekisei > 0.0 && min > 0 && max > 0 && min <= max) {
						// PASELI使用状態の設定と、それによる倍率を決定して格納
						final double bairitsu[];
						if (Integer.parseInt(pref.getString("is_paseli", "0")) == 1) {
							bairitsu = new double[]{0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 2.5, 2.75, 3.0, 3.25, 3.5, 3.75, 4.0, 4.5, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0};
						} else {
							// 初回起動時のようにメモリが存在しない状態ではPASELI未使用の倍率が取得される
							bairitsu = new double[]{1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0};
						}
						
						// 調整表にデータを追加して出力するが、あまりにも自己最高BPM適正値が大きいならエラーダイアログを出力する
						if (tekisei / bairitsu[0] >= min) {
							// 全ての調整表用の数値データを格納するリストを生成
							final ArrayList<String> highSpeed = new ArrayList<String>();
							final ArrayList<Integer> gaitouMin = new ArrayList<Integer>();
							final ArrayList<Integer> gaitouMax = new ArrayList<Integer>();
							final ArrayList<Double> tyouseiMin = new ArrayList<Double>();
							final ArrayList<Double> tyouseiMax = new ArrayList<Double>();
							
							// 計算中ダイアログの生成後、表示する
							final HorizontalProgressDialogFragment progressDlgFragment = HorizontalProgressDialogFragment.newInstance(R.string.please_wait, R.string.calcuating_tekiseiBPM, bairitsu.length - 1);
							progressDlgFragment.show(getFragmentManager(), "dialog_fragment");
							
							// 後に別スレッドに作るこのデータベースをクローズするために、このメソッドが必要
							tempDb.acquireReference();
							
							new Thread(new Runnable() {
								public void run() {
									double tmpMax = max;
									
									if (tmpMax > tekisei / bairitsu[0]) {
										// 1行分のデータを計算してリストに格納
										highSpeed.add("調整不可");
										gaitouMin.add((int)(tekisei / bairitsu[0] + 1.0));
										gaitouMax.add((int)tmpMax);
										tyouseiMin.add(-1.0);
										tyouseiMax.add(-1.0);
										// High Speedは調整不可なので-1.0にして、1行文のデータを一時保存用のデータベースに格納
										tempDb.execSQL("insert into tekisei values (0, -1.0, " + String.valueOf((int)(tekisei / bairitsu[0] + 1.0)) + ", " + String.valueOf((int)tmpMax) + ");");
										
										tmpMax = tekisei / bairitsu[0];
									}
									for (int i = 1; i < bairitsu.length; i++) {
										if (tmpMax > tekisei / bairitsu[i]) {
											if (Math.ceil(tekisei / bairitsu[i]) >= min) {
												// 1行分のデータを計算してリストに格納
												highSpeed.add("×" + String.valueOf(bairitsu[i - 1]));
												gaitouMin.add((int)(Math.floor(tekisei / bairitsu[i]) + 1.0));
												gaitouMax.add((int)Math.floor(tmpMax));
												tyouseiMin.add((Math.floor(tekisei / bairitsu[i]) + 1) * bairitsu[i - 1]);
												tyouseiMax.add(Math.floor(tmpMax) * bairitsu[i - 1]);
												// 1行文のデータを一時保存用のデータベースに格納
												tempDb.execSQL("insert into tekisei values (" + String.valueOf(i) + ", " + String.valueOf(bairitsu[i - 1]) + ", " + String.valueOf((int)(Math.floor(tekisei / bairitsu[i]) + 1.0)) + ", " + String.valueOf((int)Math.floor(tmpMax)) + ");");
												
												tmpMax = tekisei / bairitsu[i];
											} else {
												// 1行分のデータを計算してリストに格納し、これが最後の行となるのでforループをbreakする
												highSpeed.add("×" + String.valueOf(bairitsu[i - 1]));
												gaitouMin.add(min);
												gaitouMax.add((int)Math.floor(tmpMax));
												tyouseiMin.add(min * bairitsu[i - 1]);
												tyouseiMax.add(Math.floor(tmpMax) * bairitsu[i - 1]);
												// 1行文のデータを一時保存用のデータベースに格納
												tempDb.execSQL("insert into tekisei values (" + String.valueOf(i) + ", " + String.valueOf(bairitsu[i - 1]) + ", " + String.valueOf(min) + ", " + String.valueOf((int)Math.floor(tmpMax)) + ");");
												
												tmpMax = tekisei / bairitsu[i];
												break;
											}
										}
										
										final int idx = i;
										// UIスレッド上の計算中ダイアログの進捗バーのカウントを1個分増加
										mHandler.post(new Runnable() {
											public void run() {
												progressDlgFragment.setProgress(idx + 1);
											}
										});
									}
									if (Math.ceil(tekisei / bairitsu[bairitsu.length - 1]) >= min && min <= Math.floor(tmpMax)) {
										// 1行分のデータを計算してリストに格納
										highSpeed.add("×8.0");
										gaitouMin.add(min);
										gaitouMax.add((int)Math.floor(tmpMax));
										tyouseiMin.add(min * 8.0);
										tyouseiMax.add(Math.floor(tmpMax) * 8.0);
										// 1行文のデータを一時保存用のデータベースに格納
										tempDb.execSQL("insert into tekisei values (" + String.valueOf(bairitsu.length) + ", 8.0, " + String.valueOf(min) + ", " + String.valueOf((int)Math.floor(tmpMax)) + ");");
									}
									
									// メモリリークを避けるため、一時保存したレイアウト上の表の結果データを格納する別スレッド上のデータベースをクローズする
									tempDb.close();
									
									// UIスレッド上の計算中ダイアログを消去し、調整表を新しく更新
									mHandler.post(new Runnable() {
										public void run() {
											progressDlgFragment.dismiss();
											updateTable(highSpeed, gaitouMin, gaitouMax, tyouseiMin, tyouseiMax, false);
										}
									});
								}
							}).start();
						} else {
							// 結果が1つも出力できない旨のアラートダイアログを表示
							OKDialogFragment.newInstance(R.string.title_output_error, R.string.message_output_error).show(getFragmentManager(), "dialog_fragment");
						}
					} else if (tekisei <= 0.0) {
						// BPM適正値が正常に入力されていない旨のアラートダイアログを表示
						OKDialogFragment.newInstance(R.string.title_tekiseiValue_error, R.string.message_tekiseiValue_error).show(getFragmentManager(), "dialog_fragment");
					} else {
						// 表示対象BPM範囲が正常に入力されていない旨のアラートダイアログを表示
						OKDialogFragment.newInstance(R.string.title_showRange_error, R.string.message_showRange_error).show(getFragmentManager(), "dialog_fragment");
					}
				} catch (NumberFormatException e) {
					// ログ出力
					Log.w(TAG, "NumberFormatException -> " + TAG + "#onClick() : bpm_tekisei or bpm_min or bpm_max");
					
					// 3つのBPM値のどれかが数値以外の文字列を入力している旨のアラートダイアログを表示
					OKDialogFragment.newInstance(R.string.title_numberFormat_error, R.string.message_tekiseiNumberFormat_error).show(getFragmentManager(), "dialog_fragment");
				}
				break;
			case R.id.buttonClearTekiseiTable:
				// 正常に表がクリアされた旨のトーストを表示
				Toast.makeText(getActivity(), "ハイスピ調整表がクリアされました", Toast.LENGTH_SHORT).show();
		}
	}
	
	@SuppressLint("DefaultLocale")
	private void updateTable(final List<String> highSpeed, final List<Integer> gaitouMin, final List<Integer> gaitouMax, final List<Double> tyouseiMin, final List<Double> tyouseiMax, boolean isRestoring) {
		// 生成or復元中ダイアログの生成後、表示する(isRestoringがtrueの時は「復元中」、falseの時は「生成中」)
		int dlgMsg = R.string.creating_layout;
		if (isRestoring) {
			dlgMsg = R.string.restoring_layout;
		}
		final HorizontalProgressDialogFragment progressDlgFragment = HorizontalProgressDialogFragment.newInstance(R.string.please_wait, dlgMsg, highSpeed.size());
		progressDlgFragment.show(getFragmentManager(), "dialog_fragment");
		
		new Thread(new Runnable() {
			public void run() {
				// 調整表の各列の追加部分のレイアウトを追加する
				final LayoutParams layoutPrmHS = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				final LayoutParams layoutPrmGaitou = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				final LayoutParams layoutPrmTyousei = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				layoutPrmHS.setMargins(0, 0, (int)getResources().getDisplayMetrics().density, 2 * (int)getResources().getDisplayMetrics().density);
				
				layoutPrmGaitou.setMargins(0, 0, 0, 2 * (int)getResources().getDisplayMetrics().density);
				
				layoutPrmTyousei.setMargins((int)getResources().getDisplayMetrics().density, 0, 0, 2 * (int)getResources().getDisplayMetrics().density);
				mHandler.post(new Runnable() {
					public void run() {
						layoutHS.addView(rowHS, layoutPrmHS);
						layoutGaitou.addView(rowGaitou, layoutPrmGaitou);
						layoutTyousei.addView(rowTyousei, layoutPrmTyousei);
					}
				});
				
				for (int i = 0; i < highSpeed.size(); i++) {
					// 該当BPM帯、調整後BPM帯の表示する部分的にHTML形式で構成した色付き文字列の決定
					String tyouseiBPM = "<font color=\"white\">" + String.format("%.2f", tyouseiMin.get(i));
					if (tyouseiMax.get(i) == -1.0) {
						tyouseiBPM = "<font color=\"white\">***</font>";
					} else if (gaitouMin.get(i) != gaitouMax.get(i)) {
						tyouseiBPM += "～</font><font color=\"yellow\">" + String.format("%.2f", tyouseiMax.get(i)) + "</font>";
					} else {
						tyouseiBPM += "</font>";
					}
					// 1行分データをレイアウトに追加
					addRowViews(highSpeed.get(i), "<font color=\"aqua\">" + String.valueOf(gaitouMin.get(i)) + "～" + String.valueOf(gaitouMax.get(i)) + "</font>", tyouseiBPM);
					
					final int idx = i;
					// UIスレッド上の生成中ダイアログの進捗バーのカウントを1個分増加
					mHandler.post(new Runnable() {
						public void run() {
							progressDlgFragment.setProgress(idx + 1);
						}
					});
				}
				
				// UIスレッド上の生成中ダイアログを消去
				mHandler.post(new Runnable() {
					public void run() {
						progressDlgFragment.dismiss();
					}
				});
			}
		}).start();
	}
	
	private void addRowViews(String highSpeed, String gaitouBPM, String tyouseiBPM) {
		// 3つのテキストビューを生成
		final TextView textHS = new TextView(getActivity());
		textHS.setText(highSpeed);
		textHS.setTextColor(Color.rgb(0, 255, 0));
		
		// 該当BPM帯用のテキストビューを生成
		final TextView textGaitou = new TextView(getActivity());
		textGaitou.setText(Html.fromHtml(gaitouBPM));
		
		// 調整後BPM帯用のテキストビューを生成
		final TextView textTyousei = new TextView(getActivity());
		textTyousei.setText(Html.fromHtml(tyouseiBPM));
		
		// UIスレッド上の調整表を更新する
		mHandler.post(new Runnable() {
			public void run() {
				rowHS.addView(textHS, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				rowGaitou.addView(textGaitou, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				rowTyousei.addView(textTyousei, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			}
		});
	}
}
