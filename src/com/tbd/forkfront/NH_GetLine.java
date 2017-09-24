package com.tbd.forkfront;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.tbd.forkfront.*;


public class NH_GetLine
{
	private static final int MAX_HISTORY = 10;
	private UI mUI;
	private NetHackIO mIO;
	private String mTitle;
	private int mMaxChars;
	private NH_State mState;
	private Context mContext;
	private List<String> mHistory;

	// ____________________________________________________________________________________
	public NH_GetLine(NetHackIO io, NH_State state)
	{
		mIO = io;
		mState = state;
	}

	// ____________________________________________________________________________________
	public void show(Activity context, final String title, final int nMaxChars)
	{
		mContext = context;
		mTitle = title;
		mMaxChars = nMaxChars;
		mHistory = loadHistory();
		mUI = new UI(context, true, true, false, getInitText());
	}
	
	// ____________________________________________________________________________________
	public void showWhoAreYou(Activity context, final int nMaxChars, List<String> history)
	{
		mContext = context;
		mTitle = "Who are you?";
		mMaxChars = nMaxChars;
		mHistory = history;
		mUI = new UI(context, false, false, true, getInitText());
	}

	// ____________________________________________________________________________________
	public void setContext(Activity context)
	{
		mContext = context;
		if(mUI != null)
			mUI = new UI(context, mUI.mSaveHistory, mUI.mSaveHistory, mUI.mShowWizard, getInitText());
	}

	private String getInitText()
	{
		if(mTitle.contains("For what do you wish"))
			return "";
		if(mTitle.startsWith("Replace annotation \""))
		{
			int i = mTitle.lastIndexOf('"');
			if(i > 19)
				return mTitle.substring(20, i);
		}
		if(mTitle.startsWith("Replace previous annotation \""))
		{
			int i = mTitle.lastIndexOf('"');
			if(i > 28)
				return mTitle.substring(29, i);
		}
		if(mTitle.startsWith("What do you want to call") || mTitle.startsWith("Call ")) {
			int i = mTitle.indexOf(" called ");
			if(i > 0)
				return mTitle.substring(i+8, mTitle.length() - 1);
		}
		if(mTitle.startsWith("What do you want to name")) {
			int i = mTitle.indexOf(" named ");
			if(i > 0)
				return mTitle.substring(i+7, mTitle.length() - 1);
		}
		if(mHistory.size() > 0)
			return mHistory.get(0);
		return "";
	}

	// ____________________________________________________________________________________
	public KeyEventResult handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		if(mUI == null)
			return KeyEventResult.IGNORED;
		return mUI.handleKeyDown(ch, nhKey, keyCode, modifiers, repeatCount, bSoftInput);
	}
	
	// ____________________________________________________________________________________
	private void storeHistory(List<String> history, String newString)
	{
		if( newString.trim().length() == 0 )
			return;
		history.remove(newString);
		history.add(0, newString);
		if(history.size() > MAX_HISTORY)
			history = history.subList(0, MAX_HISTORY);
		StringBuilder builder = new StringBuilder();
		for(String h : history)
		{
			h = h.replace("%", "%1");
			h = h.replace(";", "%2");
			builder.append(h);
			builder.append(';');
		}
		Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
		editor.putString("lineHistory", builder.toString());
		editor.commit();
	}

	// ____________________________________________________________________________________
	private List<String> loadHistory()
	{
		String value = PreferenceManager.getDefaultSharedPreferences(mContext).getString("lineHistory", "");
		String[] strings = value.split(";");
		List<String> history = new ArrayList<>(strings.length);
		for(String s : strings)
		{
			s = s.replace("%2", ";");
			s = s.replace("%1", "%");
			history.add(s);
		}
		return history;
	}

	// ____________________________________________________________________________________
	public void setOrientation(int orientation)
	{
		if(mUI != null)
			mUI.setOrientation(orientation);
	}

	// ____________________________________________________________________________________ //
	// 																						//
	// ____________________________________________________________________________________ //
	private class UI
	{
		private final Movable mMovable;
		private Context mContext;
		private EditText mInput;
		private ListView mHistoryList;
		private CheckBox mWizardCheck;
		//private NH_Dialog mDialog;
		private View mRoot;
		private ArrayAdapter<String> mAdapter;
		public boolean mSaveHistory;
		public boolean mShowWizard;

		// ____________________________________________________________________________________
		public UI(Activity context, boolean saveHistory, boolean showKeyboard, boolean showWizard, String initText)
		{
			mContext = context;
			
			mSaveHistory = saveHistory;
			mShowWizard = showWizard;

			mRoot = Util.inflate(context, R.layout.dialog_getline, R.id.dlg_frame);
			mInput = (EditText)mRoot.findViewById(R.id.input);
			mInput.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(mMaxChars) });
			mInput.setOnKeyListener(new OnKeyListener()
			{
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event)
				{
					if(event.getAction() != KeyEvent.ACTION_DOWN)
						return false;

					if(keyCode == KeyEvent.KEYCODE_ENTER)
						ok();
					else if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE)
						cancel();
					else if(keyCode == KeyEvent.KEYCODE_SEARCH) // This is doing weird stuff, might as well block it 
						return true;
					return false;
				}
			});
			mInput.addTextChangedListener(new TextWatcher() {
				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					if(mShowWizard) {
						mWizardCheck.setEnabled(true);
						String text = mInput.getText().toString();
						for(String h : mHistory) {
							if( h.equals( text ) ) {
								mWizardCheck.setEnabled(false);
								break;
							}
						}
					}
				}
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
				@Override
				public void afterTextChanged(Editable s) { }
			});

			((TextView)mRoot.findViewById(R.id.title)).setText(mTitle);

			mRoot.findViewById(R.id.history).setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if(v != null)
					{
						toggleHistory();
					}
				}
			});
			
			mHistoryList = (ListView)mRoot.findViewById(R.id.history_list);
			
			mWizardCheck = (CheckBox)mRoot.findViewById(R.id.wizard);
			mWizardCheck.setVisibility(showWizard ? View.VISIBLE : View.GONE);
			
			mAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, mHistory);
			mHistoryList.setAdapter(mAdapter);
			
			mHistoryList.setVisibility(View.GONE);

			mHistoryList.setOnItemClickListener(new OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id)
				{
					mInput.setText(mAdapter.getItem(position));
					mInput.selectAll();
					mHistoryList.setVisibility(View.GONE);
					mWizardCheck.setEnabled(false);
				}
			});

			mHistoryList.setOnKeyListener(new OnKeyListener()
			{
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event)
				{
					return false;
				}
			});
			
			mRoot.findViewById(R.id.btn_0).setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if(v != null)
					{
						ok();
					}
				}
			});
			mRoot.findViewById(R.id.btn_1).setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					cancel();
				}
			});

			mState.hideControls();
			mInput.requestFocus();
			
			mInput.setText(initText);
			mInput.selectAll();
			
			if(showKeyboard)
				Util.showKeyboard(context, mInput);

			mMovable = new Movable(context, mRoot);
		}

		// ____________________________________________________________________________________
		public void setOrientation(int orientation)
		{
			mMovable.setOrientation(orientation);
		}

		// ____________________________________________________________________________________
		protected void toggleHistory() {
			mHistoryList.setVisibility(mHistoryList.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE );
		}

		// ____________________________________________________________________________________
		public KeyEventResult handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
		{
			if(mRoot == null)
				return KeyEventResult.IGNORED;

			switch(keyCode)
			{
			case KeyEvent.KEYCODE_BACK:
				cancel();
			break;

			case KeyEvent.KEYCODE_ENTER:
				ok();
			break;

			default:
				if(ch == '\033')
					cancel();
				else if(bSoftInput)
				{
					if(mInput.hasSelection())
						mInput.setText(mInput.getText().replace(mInput.getSelectionStart(), mInput.getSelectionEnd(), ""));
					mInput.append(""+ch);
					return KeyEventResult.HANDLED;
				}
				else
					return KeyEventResult.RETURN_TO_SYSTEM;
			}
			return KeyEventResult.HANDLED;
		}
		
		// ____________________________________________________________________________________
		public void dismiss()
		{
			Util.hideKeyboard(mContext, mInput);
			if(mRoot != null)
			{
				mRoot.setVisibility(View.GONE);
				((ViewGroup)mRoot.getParent()).removeView(mRoot);
				mRoot = null;
				mState.showControls();
			}
			mUI = null;
		}

		// ____________________________________________________________________________________
		private void ok()
		{
			if(mRoot != null)
			{
				String text = mInput.getText().toString();
				String app = "";
				if(mShowWizard && text.length() > 0)
					app = mWizardCheck.isEnabled() && mWizardCheck.isChecked() ? "1" : "0";
				mIO.sendLineCmd(text + app);
				if(mSaveHistory)
					storeHistory(mHistory, text);
				dismiss();
			}
		}

		// ____________________________________________________________________________________
		private void cancel()
		{
			if(mRoot != null)
			{
				mIO.sendLineCmd("\033 ");
				dismiss();
			}
		}
	}
}
