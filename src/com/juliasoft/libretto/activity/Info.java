package com.juliasoft.libretto.activity;

import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.utils.Utils;

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
		TextView utente = (TextView) findViewById(R.id.info_user);
		TextView matricola = (TextView) findViewById(R.id.info_matricola);

		TextView annoAccademico = (TextView) findViewById(R.id.info_annoAccademico);
		TextView statoCariera = (TextView) findViewById(R.id.info_statoCarriera);
		TextView corso = (TextView) findViewById(R.id.info_corso);
		TextView facoltà = (TextView) findViewById(R.id.info_facolta);
		TextView percorso = (TextView) findViewById(R.id.info_percorso);
		TextView durata = (TextView) findViewById(R.id.info_durata);
		TextView annoDiCorso = (TextView) findViewById(R.id.info_annoDiCorso);
		TextView ordinamento = (TextView) findViewById(R.id.info_ordinamento);
		TextView normativa = (TextView) findViewById(R.id.info_normativa);
		TextView dataImmatricolazione = (TextView) findViewById(R.id.info_dataImmatricolazione);
		
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View edit_layout = inflater.inflate(R.layout.info_edit, null);

		listEdit = new ArrayList<EditText>();
		listEdit.add((EditText) edit_layout.findViewById(R.id.et_info_nome));
		listEdit.add((EditText) edit_layout
				.findViewById(R.id.et_info_matricola));
		listEdit.add((EditText) edit_layout.findViewById(R.id.et_info_tipo));
		listEdit.add((EditText) edit_layout.findViewById(R.id.et_info_profilo));
		listEdit.add((EditText) edit_layout.findViewById(R.id.et_info_anno));
		listEdit.add((EditText) edit_layout.findViewById(R.id.et_info_immat));
		listEdit.add((EditText) edit_layout.findViewById(R.id.et_info_corso));
		listEdit.add((EditText) edit_layout.findViewById(R.id.et_info_ordin));
		listEdit.add((EditText) edit_layout.findViewById(R.id.et_info_percorso));

		initLoginButton();

		listView = new ArrayList<TextView>();
		listView.add(utente);
		listView.add(matricola);
		listView.add(annoAccademico);
		listView.add(statoCariera);
		listView.add(corso);
		listView.add(facoltà);
		listView.add(percorso);
		listView.add(durata);
		listView.add(annoDiCorso);
		listView.add(ordinamento);
		listView.add(normativa);
		listView.add(dataImmatricolazione);

		Intent intent = getIntent();
		String page_HTML = intent.getStringExtra(getPackageName() + ".info");
		retrieveData(page_HTML);
	}

	private void initLoginButton() {
		builder = new AlertDialog.Builder(this).setTitle("Login")
				.setIcon(android.R.drawable.ic_dialog_alert).create();
		builder.setButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}

		});

		builder.getWindow().getAttributes().dimAmount = 0.5f;
		builder.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
	}

	private void retrieveData(String page_HTML) {
		if (page_HTML != null)
			try {
				// elimino stringhe speciali
				// page_HTML = Utils.removeSpecialString(page_HTML, "&.*?;");

				// recupero la matricola e il nome utente

				Element h1 = Utils.jsoupSelect(page_HTML, "div#header")
						.select("h1").first();

				String split[] = h1.ownText().split(" - ");
				if (split.length == 2) {
					listInfo.add(split[0]);
					listInfo.add(split[1]);
				}

				// recupero tutte le informazioni utili dell'utente
				Elements statoStudente = Utils.jsoupSelect(page_HTML,
						"div#gu-homepagestudente-cp2Child");

				// Anno accademico e stato cariera

				Elements aa_sc = statoStudente.select("p#textStatusStudente>b");
				for (Element e : aa_sc)
					listInfo.add(e.text());

				// Corso, facoltà e percorso

				Elements cor_fac_per = statoStudente.select("p#textStatusStudenteCorsoFac>b");
				for (Element e : cor_fac_per)
					listInfo.add(e.text());

				// Durata e anno di corso

				Elements dur_adc = statoStudente.select("div#boxStatusStudenteIscriz1>p>b");
				for (Element e : dur_adc)
					listInfo.add(e.text());

				// Ordinamento e Normativa

				Elements ord_norm = statoStudente.select("div#boxStatusStudenteIscriz2>p>b");
				for (Element e : ord_norm)
					listInfo.add(e.text());
				
				// Data immatricolazione
				
				Element data_imm = statoStudente.select("p#textStatusStudenteImma>b").first();
				listInfo.add(data_imm.text());
				
				

				infoHandler.sendEmptyMessage(INIT);
			} catch (Exception e) {
				Log.e(TAG, "Retrieving data: " + e.getMessage());
				e.printStackTrace();
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
			switch (msg.what) {
			case INIT: {
				int i = 0;
				for (TextView tv : listView)
					tv.setText(listInfo.get(i++));
				break;
			}
			case EDIT: {
				int i = 0;
				for (TextView tv : listView)
					tv.setText(listEdit.get(i++).getText().toString());
				break;
			}
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
			progressDialog = ProgressDialog.show(Info.this, "Please wait...",
					"Loading data ...", true);
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
			} else
				retrieveData(cm.connection(ConnectionManager.ESSE3,
						Utils.TARGET_INFO, null));

			return null;
		}
	}
}