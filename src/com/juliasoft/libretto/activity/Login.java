package com.juliasoft.libretto.activity;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.connection.Esse3HttpClient;
import com.juliasoft.libretto.utils.IDContextMenu;
import com.juliasoft.libretto.utils.Utils;

public class Login extends Activity {

	private static final boolean DEBUG = true;
	private static final String TAG = Login.class.getName();

	private static final int SUCCESS = 0;
	private static final int ERROR_MESSAGE = 1;
	private static final int USER_ID_MENU = 2;
	private static final int ERROR_DIALOG_ID = 3;
	private static final int MENU_DIALOG_ID = 4;
	private static final int PROGRESS_DIALOG_ID = 5;

	private static final String PREFS_NAME = "Credentials";
	private static final String PREF_USERNAME = "username";
	private static final String PREF_PASSWORD = "password";
	private static final String PREF_REMEMBER = "remember";

	private ConnectionManager cm;
	private LoginTask loginTask;
	private Map<String, String> html_pages;
	private SharedPreferences loginPreferences;
	private SharedPreferences.Editor loginPrefsEditor;

	private EditText usernameEdit;
	private EditText passwordEdit;
	private CheckBox rememberCheck;
	private Button signinButton;
	private Dialog idMenuDialog;
	private AlertDialog allertDialog;
	private ProgressDialog progressDialog;
	private String username;
	private String password;
	private String url = Utils.TARGET_HOME;
	private boolean isMultiID = false;
	private String page_HTML;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		init();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			switch (resultCode) {
			case Activity.RESULT_OK:
				doReset();
				break;
			default:
				showErrorMessage("Errore durante la chiusura dell'activity. Reset ClientManager NON avvenuto!");
				break;
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case MENU_DIALOG_ID:
			return idMenuDialog;
		case ERROR_DIALOG_ID:
			return allertDialog;
		case PROGRESS_DIALOG_ID:
			return progressDialog;
		default:
			return super.onCreateDialog(id);
		}
	}

	private Handler loginHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case SUCCESS:
				onLoginSuccess();
				Intent tabActivitiy = new Intent(getApplicationContext(),
						TabBar.class);
				startActivityForResult(tabActivitiy, 1);
				break;
			case ERROR_MESSAGE:
				showDialog(ERROR_DIALOG_ID);
				break;
			case USER_ID_MENU:
				showDialog(MENU_DIALOG_ID);
				break;
			default:
				break;
			}
		}
	};

	private void init() {
		html_pages = new HashMap<String, String>();

		loginPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		loginPrefsEditor = loginPreferences.edit();
		Boolean saveLogin = loginPreferences.getBoolean(PREF_REMEMBER, false);

		initUserNameEditTextWithConstraints(saveLogin);
		initPasswordEditText(saveLogin);
		initRememberPassword(saveLogin);
		initDialog();
		makeLoginPasswordRequired();
		setLinkToJuliaSrl();
		
		if(saveLogin)
			doLogin();
	}

	private void initDialog() {
		progressDialog = new ProgressDialog(Login.this);
		progressDialog.setTitle("Please wait...");
		progressDialog.setMessage("Loading data ...");
		progressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						loginTask.cancel(true);
					}
				});

		allertDialog = new AlertDialog.Builder(Login.this).setTitle("Login")
				.setIcon(android.R.drawable.ic_dialog_alert).create();
		allertDialog.setButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		WindowManager.LayoutParams lp = allertDialog.getWindow()
				.getAttributes();
		lp.dimAmount = 0.5f;

		allertDialog.getWindow().setAttributes(lp);
		allertDialog.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
	}

	private void initRememberPassword(boolean saveLogin) {
		rememberCheck = (CheckBox) findViewById(R.id.id_remember_up);
		rememberCheck.setChecked(saveLogin);
	}

	private void initPasswordEditText(boolean saveUser) {
		passwordEdit = (EditText) findViewById(R.id.password);
		final ImageView clearButtonImage = (ImageView) findViewById(R.id.login_clear_password);

		clearButtonImage.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				passwordEdit.setText("");
				clearButtonImage.setVisibility(View.GONE);
			}
		});
		
		passwordEdit.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus)
					clearButtonImage.setVisibility(View.GONE);
				
			}
		});
		passwordEdit.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (s.length() > 0)
					clearButtonImage.setVisibility(View.VISIBLE);
				else
					clearButtonImage.setVisibility(View.GONE);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				
			}
		});
		
		if (saveUser) {
			password = loginPreferences.getString(PREF_PASSWORD, "");
			passwordEdit.setText(password);
		}
	}

	private void initUserNameEditTextWithConstraints(boolean savePass) {
		usernameEdit = (EditText) findViewById(R.id.username);
		final ImageView clearButtonImage = (ImageView) findViewById(R.id.login_clear_username);

		clearButtonImage.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				usernameEdit.setText("");
				clearButtonImage.setVisibility(View.GONE);
			}
		});
		
		usernameEdit.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus)
					clearButtonImage.setVisibility(View.GONE);
				
			}
		});
		
		usernameEdit.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (s.length() > 0) {
					clearButtonImage.setVisibility(View.VISIBLE);

					if (!Pattern.matches("^[a-z0-9]+$", s))
						usernameEdit
								.setError("Oops! Prefisso 'id', caratteri accettati a-z e 0-9");
				} else
					clearButtonImage.setVisibility(View.GONE);
			}
		});

		if (savePass) {
			username = loginPreferences.getString(PREF_USERNAME, "");
			usernameEdit.setText(username);
		}

		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(usernameEdit.getWindowToken(), 0);
	}

	private void makeLoginPasswordRequired() {
		signinButton = (Button) findViewById(R.id.login);
		signinButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				username = usernameEdit.getText().toString();
				password = passwordEdit.getText().toString();

				if (username.length() == 0)
					usernameEdit.setError("Required");
				else if (password.length() == 0)
					passwordEdit.setError("Required");
				else
					doLogin();
			}
		});
	}

	private void setLinkToJuliaSrl() {
		TextView disclaimer = (TextView) findViewById(R.id.footer_message);
		Linkify.addLinks(disclaimer, Linkify.WEB_URLS);
	}

	private void enableLogin() {
		usernameEdit.setEnabled(true);
		passwordEdit.setEnabled(true);
		rememberCheck.setEnabled(true);
		signinButton.setEnabled(true);
	}

	private void disableLogin() {
		usernameEdit.setEnabled(false);
		passwordEdit.setEnabled(false);
		rememberCheck.setEnabled(false);
		signinButton.setEnabled(false);
	}

	private void doLogin() {
		disableLogin();
		showDialog(PROGRESS_DIALOG_ID);
		loginTask = (LoginTask) new LoginTask().execute();
	}

	private void doReset() {
		cm.reset();
		html_pages.clear();
		url = Utils.TARGET_HOME;
		idMenuDialog = null;
		System.gc();
	}

	private void onTaskCompleted(boolean success) {
		if (isMultiID)
			selectID();
		if (success)
			loginHandler.sendEmptyMessage(SUCCESS);

		removeDialog(PROGRESS_DIALOG_ID);
		enableLogin();
		if (DEBUG)
			Log.i(TAG, "Task complete.");
	}

	private void onLoginSuccess() {
		if (rememberCheck.isChecked()) {
			loginPrefsEditor.putBoolean(PREF_REMEMBER, true);
			loginPrefsEditor.putString(PREF_USERNAME, username);
			loginPrefsEditor.putString(PREF_PASSWORD, password);
			loginPrefsEditor.commit();
		} else {
			loginPrefsEditor.clear();
			loginPrefsEditor.commit();
		}
	}

	private void showErrorMessage(String msg) {
		allertDialog.setMessage(msg);
		loginHandler.sendEmptyMessage(ERROR_MESSAGE);
	}

	private void selectID() {
		IDContextMenu menu = new IDContextMenu(this, MENU_DIALOG_ID,
				new IDContextMenu.IconContextMenuOnClickListener() {

					@Override
					public void onClick(String url) {
						Login.this.url = url;
						isMultiID = false;
						idMenuDialog.dismiss();
						doLogin();
					}
				});

		Resources res = getResources();

		for (Element tr : Utils.jsoupSelect(page_HTML, "table.detail_table")
				.select("tr")) {
			Element a = tr.select("td.detail_table>a.detail_table").first();
			if (a != null) {
				String id = a.text();
				String url = Esse3HttpClient.AUTH_URI + a.attr("href");
				menu.addItem(res, id, R.drawable.forward_arrow, url);
			}
		}

		idMenuDialog = menu.createMenu("Scegli cariera");
		idMenuDialog.show();
		// loginHandler.sendEmptyMessage(USER_ID_MENU);
	}

	private class LoginTask extends AsyncTask<Void, Void, Boolean> {

		public LoginTask() {
			cm = ConnectionManager.getInstance();
			cm.setCredentials(username, password);
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			super.onPostExecute(success);
			onTaskCompleted(success);
		}

		@Override
		protected Boolean doInBackground(final Void... params) {
			if (Utils.isNetworkAvailable(Login.this)) {
				if (!cm.isLogged() && !isCancelled()) {
					try {
						cm.authenticate();
					} catch (Exception e) {
						Utils.appendToLogFile("Login authenticate()", e.getMessage());
						showErrorMessage(e.getMessage());
					}
				}
				if (cm.isLogged() && !isCancelled()) {
					page_HTML = cm.connection(ConnectionManager.ESSE3, url,
							null);
					if (isMultiID(page_HTML)) {
						// selectID(page_HTML);
						isMultiID = true;
					} else
						return true;
				}
			} else {
				cm.setLogged(false);
				showErrorMessage("Connessione NON attiva!");
			}

			return false;
		}

		private boolean isMultiID(String page_HTML) {
			return page_HTML == null ? false : page_HTML
					.contains("Scegli carriera");
		}
	}
}