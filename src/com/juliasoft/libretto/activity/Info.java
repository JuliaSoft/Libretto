package com.juliasoft.libretto.activity;

import java.util.ArrayList;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.connection.Esse3HttpClient;
import com.juliasoft.libretto.utils.Utils;

/**
 * Visualizza le informazioni generali dello studente
 */
public class Info extends Activity {

	private static final boolean DEBUG = true;
	private static final String TAG = Info.class.getName();

	private static final int INIT = 1;
	private static final int ERROR_MESSAGE = 2;

	private static final int ERROR_DIALOG_ID = 3;
	private static final int PROGRESS_DIALOG_ID = 4;

	private static final int MENU_UPDATE = R.id.info_menu_update;

	private InformationTask infoTask;
	private ArrayList<TextView> listView;
	private ImageView statoImg;
	private ImageView fotoImg;
	private ArrayList<String> listDetails;

	private AlertDialog allertDialog;
	private ProgressDialog progressDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info);
		init();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.info_menu, menu);
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case ERROR_DIALOG_ID:
			return allertDialog;
		case PROGRESS_DIALOG_ID:
			return progressDialog;
		default:
			return super.onCreateDialog(id);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_UPDATE:
			doUpdate();
			return true;
		default:
			return false;
		}
	}

	private Handler infoHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case INIT: {
				int i = 0;
				for (TextView tv : listView)
					tv.setText(listDetails.get(i++));
				break;
			}
			case ERROR_MESSAGE:
				showDialog(ERROR_DIALOG_ID);
				break;
			case PROGRESS_DIALOG_ID:
				showDialog(PROGRESS_DIALOG_ID);
				break;
			default:
			}
		}
	};

	private void init() {
		listDetails = new ArrayList<String>();
		listView = new ArrayList<TextView>();

		Intent intent = getIntent();
		String page_HTML = intent.getStringExtra(getPackageName() + ".info");
		retrieveData(page_HTML);
		listView.add((TextView) findViewById(R.id.info_user));
		listView.add((TextView) findViewById(R.id.info_matricola));
		listView.add((TextView) findViewById(R.id.info_annoAccademico));
		listView.add((TextView) findViewById(R.id.info_statoCarriera));
		listView.add((TextView) findViewById(R.id.info_corso));
		listView.add((TextView) findViewById(R.id.info_facolta));
		listView.add((TextView) findViewById(R.id.info_percorso));
		listView.add((TextView) findViewById(R.id.info_durata));
		listView.add((TextView) findViewById(R.id.info_annoDiCorso));
		listView.add((TextView) findViewById(R.id.info_ordinamento));
		listView.add((TextView) findViewById(R.id.info_normativa));
		listView.add((TextView) findViewById(R.id.info_dataImmatricolazione));
		statoImg = (ImageView) findViewById(R.id.info_imageStato);
		fotoImg = (ImageView) findViewById(R.id.info_foto);
		initDialog();
		doUpdate();
	}

	private void initDialog() {
		progressDialog = new ProgressDialog(Info.this);
		progressDialog.setTitle("Please wait...");
		progressDialog.setMessage("Loading data ...");
		progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				infoTask.cancel(true);
			}
		});

		allertDialog = new AlertDialog
				.Builder(Info.this)
				.setTitle("Informazioni")
				.setIcon(android.R.drawable.ic_dialog_alert)
				.create();
		allertDialog.setButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}

		});

		allertDialog.getWindow().getAttributes().dimAmount = 0.5f;
		allertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
	}

	private void doUpdate() {
		showDialog(PROGRESS_DIALOG_ID);
		infoTask = (InformationTask) new InformationTask().execute();
	}

	private Boolean retrieveData(final String page_HTML) {
		if (page_HTML != null) {
			try {
				// Recupero la matricola e il nome utente

				Element h1 = Utils.jsoupSelect(page_HTML, "div#header")
						.select("h1").first();

				String split[] = h1.ownText().split(" - ");
				if (split.length == 2) {
					listDetails.add(split[0]);
					listDetails.add(split[1]);
				}

				// Recupero tutte le informazioni utili dell'utente
				final Elements statoStudente = Utils.jsoupSelect(page_HTML,
						"div#gu-homepagestudente-cp2Child");

				if (!statoStudente.isEmpty()) {
					// Anno accademico e stato cariera

					Elements aa_sc = statoStudente
							.select("p#textStatusStudente>b");
					for (Element e : aa_sc)
						listDetails.add(e.text());
					
					// Stato immagine bandierina e foto profilo
					
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							Element stato_img = statoStudente.select("p#textStatusStudente>img").first();
							statoImg.setImageBitmap(Utils.downloadBitmap(Info.this, Esse3HttpClient.AUTH_URI + stato_img.attr("src")));
							Element foto_img = Utils.jsoupSelect(page_HTML, "div#gu-hpstu-boxDatiPersonali").first().siblingElements().select("img").first();
							fotoImg.setImageBitmap(Utils.downloadBitmap(Info.this, Esse3HttpClient.AUTH_URI + foto_img.attr("src")));
						}
					});
					
					// Corso, facoltà e percorso

					Elements cor_fac_per = statoStudente
							.select("p#textStatusStudenteCorsoFac>b");
					for (Element e : cor_fac_per)
						listDetails.add(e.text());

					// Durata e anno di corso

					Elements dur_adc = statoStudente
							.select("div#boxStatusStudenteIscriz1>p>b");
					for (Element e : dur_adc)
						listDetails.add(e.text());

					// Ordinamento e Normativa

					Elements ord_norm = statoStudente
							.select("div#boxStatusStudenteIscriz2>p>b");
					for (Element e : ord_norm)
						listDetails.add(e.text());

					// Data immatricolazione

					Element data_imm = statoStudente.select(
							"p#textStatusStudenteImma>b").first();
					listDetails.add(data_imm.text());

					return true;
				}
			} catch (Exception e) {
				if (DEBUG)
					Log.e(TAG, "Retrieving data: " + e.getMessage());
			}
		}
		return false;
	}

	private void onTaskCompleted(boolean success) {
		if (success)
			infoHandler.sendEmptyMessage(INIT);
		removeDialog(PROGRESS_DIALOG_ID);
		infoTask.cancel(true);
		infoTask = null;
		if (DEBUG)
			Log.i(TAG, "Task complete.");
	}

	private void showErrorMessage(String msg) {
		allertDialog.setMessage(msg);
		infoHandler.sendEmptyMessage(ERROR_MESSAGE);
	}

	private class InformationTask extends AsyncTask<Void, Void, Boolean> {

		private ConnectionManager cm;

		public InformationTask() {
			cm = ConnectionManager.getInstance();
		}

		@Override
		protected void onPostExecute(Boolean success) {
			super.onPostExecute(success);
			onTaskCompleted(success);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			if (Utils.isNetworkAvailable(Info.this)) {
				if (!isCancelled())
					return retrieveData(cm.connection(ConnectionManager.ESSE3, Utils.TARGET_HOME, null));
			} else {
				cm.setLogged(false);
				showErrorMessage("Connessione NON attiva!");
			}
			return false;
		}
	}
}