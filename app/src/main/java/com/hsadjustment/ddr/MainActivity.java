package com.hsadjustment.ddr;

import java.util.ArrayList;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity implements ActionBar.TabListener {
	// デバッグ用のタグ
	private static final String TAG = "MainActivity";
	// 3つのフラグメントを格納するリスト
	private static ArrayList<Fragment> mTabFragments = new ArrayList<Fragment>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.container);
		
		// ActionBarを取得する
		ActionBar ab = getSupportActionBar();
		// ActionBarのNavigationModeを「タブ」にセット
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		// 各フラグメントのインスタンスの生成
		TekiseiTableFragment tekiseiTableFragment = new TekiseiTableFragment();
		ThresholdBPMTableFragment thresholdBPMTableFragment = new ThresholdBPMTableFragment();
		ListPreferenceFragment listPreferenceFragment = new ListPreferenceFragment();
		// フラグメントの切り替え時に必要なリストを生成し、3つのフラグメントインスタンスを追加
		mTabFragments.add(tekiseiTableFragment);
		mTabFragments.add(thresholdBPMTableFragment);
		mTabFragments.add(listPreferenceFragment);
		
		// リスナーを追加したタブをActionBarに追加
		ab.addTab(ab.newTab().setText("調整表").setTabListener(this));
		ab.addTab(ab.newTab().setText("閾値表").setTabListener(this));
		ab.addTab(ab.newTab().setText("設定・開発情報").setTabListener(this));
		
		// ログ出力
		Log.d(TAG, TAG + "#onCreate()");
	}
	
	// 選択されているタブが再度選択された場合に実行
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}
	
	// タブが選択された場合に実行
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		ft.replace(R.id.tabContainer, mTabFragments.get(tab.getPosition()));
	}
	
	// タブの選択が外れた場合に実行
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_settings:
				// 「ユーザー設定値変更」の場合
				
				// 一時保存した適正表と閾値表の結果データを格納するデータベースを取得し、それらのデータをすべて消去する
				SQLiteDatabase tempDb = new TempTekiseiDBHelper(this).getWritableDatabase();
				try {
					tempDb.execSQL(TempTekiseiDBHelper.DROP_TABLE);
					tempDb.execSQL(TempTekiseiDBHelper.CREATE_TABLE);
				} finally {
					tempDb.close();
				}
				tempDb = new TempThresholdBPMDBHelper(this).getWritableDatabase();
				try {
					tempDb.execSQL(TempThresholdBPMDBHelper.DROP_TABLE);
					tempDb.execSQL(TempThresholdBPMDBHelper.CREATE_TABLE);
				} finally {
					tempDb.close();
				}
				
				startActivity(new Intent(this, Preferences.class));
				
				return true;
			case R.id.menu_setBPM:
				// 「実質BPM変更(ソフラン限定)」の場合
				startActivity(new Intent(this, SetBPMActivity.class));
				
				return true;
			case R.id.menu_version:
				// 「開発情報」の場合
				OKDialogFragment.newInstance(R.string.menu_version, R.string.app_name).show(getSupportFragmentManager(), "dialog_fragment");
				
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}