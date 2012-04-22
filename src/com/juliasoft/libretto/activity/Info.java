package com.juliasoft.libretto.activity;

import java.util.ArrayList;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.utils.Utils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Visualizza le informazioni generali dello studente
 */
public class Info extends Activity {

	public static final String TAG = Info.class.getName();

	private static final int MENU_UPDATE = R.id.info_menu_update;
	private static final int CONNECTION_ERROR = 0;
	private static final int INIT = 3;
	private static final int EDIT = 4;
	private static final int DIALOG_MESSAGE = 5;

	private ArrayList<TextView> listView;
	private ArrayList<EditText> listEdit;
	private ArrayList<String> listInfo;
	private AlertDialog builder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info);
		init();
	}

	private void init() {
		listInfo = new ArrayList<String>();

		TextView utente = (TextView) findViewById(R.id.user);
		TextView matricola = (TextView) findViewById(R.id.id);
		TextView tipo = (TextView) findViewById(R.id.tipo);
		TextView profilo = (TextView) findViewById(R.id.profilo);
		TextView anno = (TextView) findViewById(R.id.anno);
		TextView immatricolazione = (TextView) findViewById(R.id.data_immatr);
		TextView corso = (TextView) findViewById(R.id.corso);
		TextView ordinamento = (TextView) findViewById(R.id.ordinamento);
		TextView percorso = (TextView) findViewById(R.id.percorso);

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View edit_layout = inflater.inflate(R.layout.info_edit, null);

		listEdit = new ArrayList<EditText>();
		EditText et = (EditText) edit_layout.findViewById(R.id.et_info_nome);
		listEdit.add(et);
		et = (EditText) edit_layout.findViewById(R.id.et_info_matricola);
		listEdit.add(et);
		et = (EditText) edit_layout.findViewById(R.id.et_info_tipo);
		listEdit.add(et);
		et = (EditText) edit_layout.findViewById(R.id.et_info_profilo);
		listEdit.add(et);
		et = (EditText) edit_layout.findViewById(R.id.et_info_anno);
		listEdit.add(et);
		et = (EditText) edit_layout.findViewById(R.id.et_info_immat);
		listEdit.add(et);
		et = (EditText) edit_layout.findViewById(R.id.et_info_corso);
		listEdit.add(et);
		et = (EditText) edit_layout.findViewById(R.id.et_info_ordin);
		listEdit.add(et);
		et = (EditText) edit_layout.findViewById(R.id.et_info_percorso);
		listEdit.add(et);

		builder = new AlertDialog.Builder(this).setTitle("Login")
				.setIcon(android.R.drawable.ic_dialog_alert).create();
		builder.setButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}

		});

		WindowManager.LayoutParams lp = builder.getWindow().getAttributes();
		lp.dimAmount = 0.5f;

		builder.getWindow().setAttributes(lp);
		builder.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

		listView = new ArrayList<TextView>();
		listView.add(utente);
		listView.add(matricola);
		listView.add(tipo);
		listView.add(profilo);
		listView.add(anno);
		listView.add(immatricolazione);
		listView.add(corso);
		listView.add(ordinamento);
		listView.add(percorso);

		Intent intent = getIntent();
		String page_HTML = intent.getStringExtra(getPackageName() + ".info");
		retrieveData(page_HTML);
	}

	private void retrieveData(String page_HTML) {
		if (page_HTML == null)
			return;

		try {
			// elimino stringhe speciali
			page_HTML = Utils.removeSpecialString(page_HTML, "&.*?;");

			// recupero la matricola e il nome utente
			Element div = Utils.jsoupSelect(page_HTML, "div.titolopagina").first();
			div.children().remove();
			String split[] = div.text().split(" - ");
			if (split.length == 2) {
				listInfo.add(split[0]);
				listInfo.add(split[1]);
			}

			// recupero tutte le informazioni utili dell'utente
			Elements tds = Utils.jsoupSelect(page_HTML, "td.tplMaster");
			for (Element td : tds) {
				listInfo.add(td.text());
			}
			infoHandler.sendEmptyMessage(INIT);
		} catch (Exception e) {
			Log.e(TAG, "Retrieving data: " + e.getMessage());
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.info_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_UPDATE:
			new UpdateInfoTask().execute();
			return true;
		default:
			return false;
		}
	}

	private void showMessage(int type, String msg) {
		builder.setMessage(msg);
		infoHandler.sendEmptyMessage(type);
	}

	// Handler serve per aggiornare la grafica
	private Handler infoHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int i = 0;

			switch (msg.what) {
			case INIT:
				for (TextView tv : listView)
					tv.setText(listInfo.get(i++));
				break;
			case EDIT:
				for (TextView tv : listView)
					tv.setText(listEdit.get(i++).getText().toString());
				break;
			case CONNECTION_ERROR:
				showDialog(DIALOG_MESSAGE);
				break;
			}
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_MESSAGE:
			return builder;
		default:
			return super.onCreateDialog(id);
		}
	}

	public class UpdateInfoTask extends AsyncTask<Void, Void, Void> {

		private ProgressDialog progressDialog;
		private ConnectionManager cm;

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(Info.this, "Please wait...", "Loading data ...", true);
			cm = ConnectionManager.getInstance();
		}

		@Override
		protected void onPostExecute(Void success) {
			progressDialog.dismiss();
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (!Utils.isNetworkAvailable(Info.this)) {
				cm.setLogged(false);
				showMessage(CONNECTION_ERROR, "Connessione NON attiva!");
			}
			else
				retrieveData(cm.connection(ConnectionManager.ESSE3, Utils.TARGET_INFO));

			return null;
		}
	}
}