package com.hsadjustment.ddr;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

public class SpinningProgressDialogFragment extends DialogFragment {
	// デバッグ用のタグ
	private static final String TAG = "SpinningProgressDialogFragment";
	
	private ProgressDialog progressDlg;
	
	public static SpinningProgressDialogFragment newInstance(int title, int message) {
		SpinningProgressDialogFragment progressDlgFragment = new SpinningProgressDialogFragment();
		
		Bundle bundle = new Bundle();
		bundle.putInt("dlgTitle", title);
		bundle.putInt("dlgMessage", message);
		progressDlgFragment.setArguments(bundle);
		
		return progressDlgFragment;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle safedInstanceState) {
		// プログレスダイアログを生成
		progressDlg = new ProgressDialog(getActivity());
		progressDlg.setTitle(getArguments().getInt("dlgTitle"));
		progressDlg.setMessage(getResources().getText(getArguments().getInt("dlgMessage")));
		progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDlg.setIcon(android.R.drawable.ic_dialog_info);
		
		// ログ出力
		Log.d(TAG, TAG + "#onCreateDialog()");
		
		return progressDlg;
	}
}
