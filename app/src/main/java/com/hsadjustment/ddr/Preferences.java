package com.hsadjustment.ddr;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Window;

public class Preferences extends PreferenceActivity {
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// タイトルバーを非表示
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		// 設定画面のPreferenceScreenを表示
		addPreferencesFromResource(R.xml.preference);
	}
}