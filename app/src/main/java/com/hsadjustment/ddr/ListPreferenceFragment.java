package com.hsadjustment.ddr;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ListPreferenceFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.list_fragment, container, false);
		
		// アダプターにリストビューのアイテム追加
		String listStr[] = new String[]{"ユーザー設定値変更", "実質BPM変更(ソフラン限定)", "開発情報"};
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listStr);
		// アダプターをリストビューにセット
		ListView listView = (ListView)view.findViewById(R.id.listView);
		listView.setAdapter(adapter);
		
		// リストビューのアイテムがタップされた時のリスナーをセット
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				switch (position) {
					case 0:
						// 「ユーザー設定値変更」の場合
						
						// 一時保存した適正表と閾値表の結果データを格納するデータベースを取得し、それらのデータをすべて消去する
						SQLiteDatabase tempDb = new TempTekiseiDBHelper(getActivity()).getWritableDatabase();
						try {
							tempDb.execSQL(TempTekiseiDBHelper.DROP_TABLE);
							tempDb.execSQL(TempTekiseiDBHelper.CREATE_TABLE);
						} finally {
							tempDb.close();
						}
						tempDb = new TempThresholdBPMDBHelper(getActivity()).getWritableDatabase();
						try {
							tempDb.execSQL(TempThresholdBPMDBHelper.DROP_TABLE);
							tempDb.execSQL(TempThresholdBPMDBHelper.CREATE_TABLE);
						} finally {
							tempDb.close();
						}
						
						startActivity(new Intent(getActivity(), Preferences.class));
						
						break;
					case 1:
						// 「実質BPM変更(ソフラン限定)」の場合
						startActivity(new Intent(getActivity(), SetBPMActivity.class));
						break;
					case 2:
						// 「開発情報」の場合
						OKDialogFragment.newInstance(R.string.menu_version, R.string.app_name).show(getFragmentManager(), "dialog_fragment");
				}
			}
		});
		
		return view;
	}
}
