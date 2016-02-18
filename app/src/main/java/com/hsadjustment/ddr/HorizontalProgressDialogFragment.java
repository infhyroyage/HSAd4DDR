package com.hsadjustment.ddr;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

public class HorizontalProgressDialogFragment extends DialogFragment {
	// デバッグ用のタグ
	private static final String TAG = "HorizontalProgressDialogFragment";
	
	private ProgressDialog progressDlg;
	
	public static HorizontalProgressDialogFragment newInstance(int title, int message, int max) {
		HorizontalProgressDialogFragment progressDlgFragment = new HorizontalProgressDialogFragment();
		
		Bundle bundle = new Bundle();
		bundle.putInt("dlgTitle", title);
		bundle.putInt("dlgMessage", message);
		bundle.putInt("dlgMax", max);
		progressDlgFragment.setArguments(bundle);
		
		return progressDlgFragment;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle safedInstanceState) {
		final int title = getArguments().getInt("dlgTitle");
		final int message = getArguments().getInt("dlgMessage");
		final int max = getArguments().getInt("dlgMax");
		
		progressDlg = new ProgressDialog(getActivity());
		progressDlg.setTitle(title);
		progressDlg.setMessage(getResources().getText(message));
		progressDlg.setIndeterminate(false);
		progressDlg.setMax(max);
		progressDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDlg.incrementProgressBy(0);
		progressDlg.setIcon(android.R.drawable.ic_dialog_info);
		
		// ログ出力
		Log.d(TAG, TAG + "#onCreateDialog()");
		
		return progressDlg;
	}
	
	public int getProgress() {
		return progressDlg.getProgress();
	}
	
	public void setProgress(int value) {
		progressDlg.setProgress(value);
	}
}
