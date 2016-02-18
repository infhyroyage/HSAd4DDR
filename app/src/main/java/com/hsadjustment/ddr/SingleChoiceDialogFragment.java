package com.hsadjustment.ddr;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class SingleChoiceDialogFragment extends DialogFragment {
	private DialogInterface.OnClickListener singleChoiceItemsClickListener = null;
	private DialogInterface.OnClickListener positiveButtonClickListener = null;
	private DialogInterface.OnClickListener negativeButtonClickListener = null;
	
	public static SingleChoiceDialogFragment newInstance(int title, String[] items, int checkedItem) {
		SingleChoiceDialogFragment singleDlgFragment = new SingleChoiceDialogFragment();
		
		Bundle bundle = new Bundle();
		bundle.putInt("dlgTitle", title);
		bundle.putStringArray("dlgItems", items);
		bundle.putInt("dlgCheckedItem", checkedItem);
		singleDlgFragment.setArguments(bundle);
		
		return singleDlgFragment;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle safedInstanceState) {
		// ダイヤログの生成
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getArguments().getInt("dlgTitle")).setIcon(android.R.drawable.ic_dialog_info).setSingleChoiceItems(getArguments().getStringArray("dlgItems"), getArguments().getInt("dlgCheckedItem"), this.singleChoiceItemsClickListener).setPositiveButton("決定", this.positiveButtonClickListener).setNegativeButton("キャンセル", this.negativeButtonClickListener);
		
		return builder.create();
	}
	
	public void setOnSingleChoiceItemsClickListener(DialogInterface.OnClickListener listener) {
		this.singleChoiceItemsClickListener = listener;
	}
	
	public void setOnPositiveButtonClickListener(DialogInterface.OnClickListener listener) {
		this.positiveButtonClickListener = listener;
	}
	
	public void setOnNegativeButtonClickListener(DialogInterface.OnClickListener listener) {
		this.negativeButtonClickListener = listener;
	}
}
