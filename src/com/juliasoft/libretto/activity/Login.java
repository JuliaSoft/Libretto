package com.juliasoft.libretto.activity;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.connection.Esse3HttpClient;
import com.juliasoft.libretto.utils.IDContextMenu;
import com.juliasoft.libretto.utils.Utils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class Login extends Activity implements OnClickListener {

	private static final int SUCCESS = 0;
	private static final int CONNECTION_ERROR = 1;
	private static final int LOGIN_ERROR = 2;
	private static final int IO_ERROR = 3;
	private static final int SHOW_DIALOG = 4;
	private static final int CONTEXT_MENU_ID = 5;
	private static final int ERROR_MESSAGE = 6;

	private static final String PREFS_NAME = "Credentials";
	private static final String PREF_USERNAME = "username";
	private static final String PREF_PASSWORD = "password";
	private static final String PREF_REMEMBER = "remember";

	private ConnectionManager cm;
	private HashMap<String, String> html_pages;
	private Intent intent;

	private EditText uname;
	private EditText pword;
	private CheckBox remeb;
	private Button login_B;
	private IDContextMenu iconContextMenu;
	private AlertDialog builder;
	private String username;
	private String password;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		init();
	}

	private void init() {

		html_pages = new HashMap<String, String>();
		html_pages.put("LIB", null);
		html_pages.put("INFO", null);
		html_pages.put("ISCRIZ", null);

		// initGUI
		uname = (EditText) findViewById(R.id.username);
		uname.addTextChangedListener(new InputValidator(uname));
		pword = (EditText) findViewById(R.id.password);
		remeb = (CheckBox) findViewById(R.id.id_remember_up);

		login_B = (Button) findViewById(R.id.login);
		login_B.setOnClickListener(this);

		uname.setText((CharSequence) loadCredentials(PREF_USERNAME));
		pword.setText((CharSequence) loadCredentials(PREF_PASSWORD));
		remeb.setChecked((Boolean) loadCredentials(PREF_REMEMBER));

		builder = new AlertDialog.Builder(this).setTitle("Login")
				.setIcon(android.R.drawable.ic_dialog_alert).create();
		builder.setButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				if (!isOnline())
					loginHandler.sendEmptyMessage(SUCCESS);
			}

		});

		WindowManager.LayoutParams lp = builder.getWindow().getAttributes();
		lp.dimAmount = 0.5f;

		builder.getWindow().setAttributes(lp);
		builder.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
	}

	private boolean isOnline() {
		return Utils.isNetworkAvailable(Login.this);
	}

	private void login() {
		username = uname.getText().toString();
		password = pword.getText().toString();

		cm.setCredentials(username, password);
		saveCredentials(remeb.isChecked());

		if (!isOnline()) {
			cm.setLogged(false);
			showMessage(CONNECTION_ERROR, "Connessione NON attiva!");
			return;
		}

		try {
			cm.authenticate();
		} catch (ConnectException e) {
			showMessage(CONNECTION_ERROR, e.getMessage());
		} catch (LoginException e) {
			showMessage(LOGIN_ERROR, e.getMessage());
		}
	}

	private void showMessage(int type, String msg) {
		builder.setMessage(msg);
		loginHandler.sendEmptyMessage(type);
	}

	private void saveCredentials(boolean value) {
		if (value) {
			getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
					.putString(PREF_USERNAME, username)
					.putString(PREF_PASSWORD, password)
					.putBoolean(PREF_REMEMBER, true).commit();
		} else {
			getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear()
					.commit();
		}
	}

	private Object loadCredentials(String key) {
		SharedPreferences pref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		if (key.equals(PREF_REMEMBER)) {
			return pref.getBoolean(key, false);
		}
		return pref.getString(key, "");
	}

	private boolean initPage(String url) {

		// Pagina libretto studente
		String page_HTML = cm.connection(ConnectionManager.ESSE3, url);

		if (isMultiID(page_HTML)) {
			selectID(page_HTML);
			return false;
		}
		html_pages.put("LIB", page_HTML);

		// Pagina informazioni studente
		page_HTML = cm.connection(ConnectionManager.ESSE3, Utils.TARGET_INFO);
		html_pages.put("INFO", page_HTML);

		// Pagina iscrizione esami
		page_HTML = cm.getSSOLLoginHTML();

		Elements trs = Utils.jsoupSelect(page_HTML, "table.Border>tbody>tr");

		if (trs.isEmpty()) {
			// OLD
			page_HTML = cm.connection(ConnectionManager.SSOL,
					Utils.TARGET_ISCRIZIONI_OLD);
			html_pages.put("ISCRIZ_OLD", page_HTML);

		} else {
			// NEW
			html_pages.put("ISCRIZ", page_HTML);
		}
		return true;
	}

	// ritorna true se l'utente deve scegliere tra pi� matricole
	private boolean isMultiID(String page_HTML) {
		if (page_HTML == null)
			return false;
		return page_HTML.contains("Scegli carriera");
	}

	// visualizza un menu con le matricole dell'utente(triennale,
	// specialistica...)
	private void selectID(String page_HTML) {
		iconContextMenu = new IDContextMenu(this, CONTEXT_MENU_ID);
		Resources res = getResources();

		Elements trs = Utils.jsoupSelect(page_HTML, "table.detail_table")
				.select("tr");
		for (Element tr : trs) {
			Element a = tr.select("td.detail_table>a.detail_table").first();
			if (a != null) {
				String id = a.text();
				String url = Esse3HttpClient.AUTH_URI + a.attr("href");
				iconContextMenu.addItem(res, id, R.drawable.forward_arrow, url);
			}
		}

		iconContextMenu
				.setOnClickListener(new IDContextMenu.IconContextMenuOnClickListener() {

					@Override
					public void onClick(final String url) {
						new LoginTask().execute(url);
					}

				});

		loginHandler.sendEmptyMessage(SHOW_DIALOG);
	}

	// Handler serve per aggiornare la grafica
	private Handler loginHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case SUCCESS:
				intent = new Intent(getApplicationContext(), TabBar.class);
				String pkg = getPackageName();
				// setto i dati ricavati dal login
				intent.putExtra(pkg + ".lib", html_pages.get("LIB"));
				intent.putExtra(pkg + ".info", html_pages.get("INFO"));
				if (html_pages.containsKey("ISCRIZ"))
					intent.putExtra(pkg + ".iscriz", html_pages.get("ISCRIZ"));
				else if (html_pages.containsKey("ISCRIZ_OLD"))
					intent.putExtra(pkg + ".iscriz_old",
							html_pages.get("ISCRIZ_OLD"));
				startActivity(intent);
				break;
			case CONNECTION_ERROR:
			case LOGIN_ERROR:
			case IO_ERROR:
				showDialog(ERROR_MESSAGE);
				break;
			case SHOW_DIALOG:
				showDialog(CONTEXT_MENU_ID);
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONTEXT_MENU_ID:
			return iconContextMenu.createMenu("Scegli matricola");
		case ERROR_MESSAGE:
			return builder;
		default:
			return super.onCreateDialog(id);
		}
	}

	@Override
	protected void onResume() {
		if (cm != null) {
			cm.reset();
			html_pages.clear();
			Log.i("INFO", "Reset CllientManager!");
		}
		cm = ConnectionManager.getInstance();
		Log.i("INFO", "Start new CllientManager!");

		super.onResume();
	}

	@Override
	public void onClick(View v) {
		if (v == login_B) {
			if (uname.getText().length() == 0) {
				uname.setError("Required");
				return;
			}
			if (pword.getText().length() == 0) {
				pword.setError("Required");
				return;
			}
			new LoginTask().execute();
		}
	}

	private class InputValidator implements TextWatcher {
		private EditText et;

		private InputValidator(EditText editText) {
			this.et = editText;
		}

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
			if (s.length() != 0) {
				switch (et.getId()) {
				case R.id.username:
					if (!Pattern.matches("^[a-z0-9]+$", s)) {
						et.setError("Oops! Prefisso 'id', caratteri accettati a-z e 0-9");
					}

					break;
				}
			}
		}
	}

	private class LoginTask extends AsyncTask<String, Void, Boolean> {

		private ProgressDialog dialog;

		public LoginTask() {
			dialog = new ProgressDialog(Login.this);
			dialog.setTitle("Please wait...");
			dialog.setCancelable(true);
		}

		@Override
		protected void onPreExecute() {
			dialog.setMessage("Loading data ...");
			dialog.show();
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
			if (success) {
				loginHandler.sendEmptyMessage(SUCCESS);
			}
		}

		@Override
		protected Boolean doInBackground(final String... params) {
			if (params != null && params.length > 0) {
				return initPage(params[0]);
			} else if (!cm.isLogged()) {
				login();
			}
			if (cm.isLogged()) {
				return initPage(Utils.TARGET_LIBRETTO);
			} else {
				return false;
			}
		}
	}
}