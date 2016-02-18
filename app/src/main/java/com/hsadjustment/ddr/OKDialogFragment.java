package com.hsadjustment.ddr;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

public class OKDialogFragment extends DialogFragment {
	// デバッグ用のタグ
	private static final String TAG = "OKDialogFragment";
	
	public static OKDialogFragment newInstance(int title, int message) {
		OKDialogFragment okDlg = new OKDialogFragment();
		
		Bundle bundle = new Bundle();
		bundle.putInt("dlgTitle", title);
		bundle.putInt("dlgMessage", message);
		okDlg.setArguments(bundle);
		
		return okDlg;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle safedInstanceState) {
		// ダイアログを生成
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getArguments().getInt("dlgTitle")).setPositiveButton("OK", null);
		// メッセージがこのアプリの名前の場合は開発情報のメッセージなので、そのメッセージを生成しアイコンも別の画像を用いる
		final int message = getArguments().getInt("dlgMessage");
		if (message == R.string.app_name) {
			String versionName = "null";
			try {
				versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
			} catch (NameNotFoundException e) {
				Log.e(TAG, "NameNotFoundException -> " + TAG + "#onCreateDialog()");
			}
			// ダイアログを生成
			builder.setMessage("HS4D2R\nVersion : " + versionName + "\nDeveloper : Kido Takeru").setIcon(android.R.drawable.ic_dialog_info);
		} else {
			// ダイアログを生成
			builder.setMessage(message).setIcon(android.R.drawable.ic_dialog_alert);
		}
		
		// ログ出力
		Log.d(TAG, TAG + "#onCreateDialog()");
		
		return builder.create();
	}
}
