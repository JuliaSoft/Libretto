package com.juliasoft.libretto.activity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.utils.Utils;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlSerializer;

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
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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

	private static final int MENU_EXPORT_XML = R.id.info_menu_export;
	private static final int MENU_UPDATE = R.id.info_menu_update;
	private static final int MENU_CLEAR_XML = R.id.info_menu_clear;
	private static final int MENU_EDIT = R.id.info_menu_edit;
	private static final int CONNECTION_ERROR = 0;
	private static final int EXPORT_XML_MESSAGE = 1;
	private static final int CLEAR_XML_MESSAGE = 2;
	private static final int INIT = 3;
	private static final int EDIT = 4;
	private static final int DIALOG_MESSAGE = 5;

	private ConnectionManager cm;

	private static final String INFO_XML_FILE = "Info.xml";

	private ArrayList<TextView> listView;
	private ArrayList<EditText> listEdit;
	private ArrayList<String> listInfo;
	private AlertDialog ad;
	private AlertDialog builder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info);
		init();
	}

	private void init() {
		cm = ConnectionManager.getInstance();
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

		ad = new AlertDialog.Builder(this)
				.setView(edit_layout)
				.setTitle("Edit")
				.setIcon(R.drawable.edit)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						infoHandler.sendEmptyMessage(EDIT);
						dialog.dismiss();
					}

				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}

						}).create();

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
		String pkg = getPackageName();
		String page_HTML = intent.getStringExtra(pkg + ".info");
		// estrazione dei dati dalla pagina HTML

		if (!loadXML())
			retriveData(page_HTML);
	}

	private void retriveData(String page_HTML) {
		if (page_HTML == null) {
			return;
		}

		try {
			// elimino stringhe speciali
			page_HTML = Utils.removeSpecialString(page_HTML, "&.*?;");

			// recupero la matricola e il nome utente
			Element div = Utils.jsoupSelect(page_HTML, "div.titolopagina")
					.first();
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
			Log.e(TAG, "Retrive data: " + e.getMessage());
		}

	}

	private boolean loadXML() {
		Log.i(TAG, "Load XML");
		FileInputStream fileis = Utils.loadXMLFile(INFO_XML_FILE);
		if (fileis == null) {
			return false;
		}

		Attributes attrs = Utils
				.jsoupSelect(Utils.inputStreamToString(fileis), "info>dettagli")
				.first().attributes();

		int i = 0;
		for (Attribute attr : attrs) {
			listView.get(i).setText(attr.getValue());
			i++;
		}

		Log.i(TAG, "Load XML completato");
		return true;
	}

	private void exportToXML() {

		FileOutputStream fileos = Utils.createXMLFile(INFO_XML_FILE);

		if (fileos == null) {
			showMessage(EXPORT_XML_MESSAGE,
					"Errore durante la creazione del file!");
			return;
		}
		// we create a XmlSerializer in order to write xml data
		XmlSerializer serializer = Xml.newSerializer();
		try {
			// we set the FileOutputStream as output for the serializer, using
			// UTF-8 encoding
			serializer.setOutput(fileos, "UTF-8");
			// Write <?xml declaration with encoding (if encoding not null) and
			// standalone flag (if standalone not null)
			serializer.startDocument(null, Boolean.valueOf(true));
			// set indentation option
			serializer.setFeature(
					"http://xmlpull.org/v1/doc/features.html#indent-output",
					true);
			serializer.startTag(null, "INFO");

			serializer.startTag(null, "DETTAGLI");
			serializer.attribute(null, "nome", listView.get(0).getText()
					.toString());
			serializer.attribute(null, "matricola", listView.get(1).getText()
					.toString());
			serializer.attribute(null, "tipo", listView.get(2).getText()
					.toString());
			serializer.attribute(null, "profilo", listView.get(3).getText()
					.toString());
			serializer.attribute(null, "anno", listView.get(4).getText()
					.toString());
			serializer.attribute(null, "immatricolazione ", listView.get(5)
					.getText().toString());
			serializer.attribute(null, "corso", listView.get(6).getText()
					.toString());
			serializer.attribute(null, "ordinamento", listView.get(7).getText()
					.toString());
			serializer.attribute(null, "percorso", listView.get(8).getText()
					.toString());
			serializer.endTag(null, "DETTAGLI");

			serializer.endTag(null, "INFO");
			serializer.endDocument();
			// write xml data into the FileOutputStream
			serializer.flush();
			// finally we close the file stream
			fileos.close();
			showMessage(EXPORT_XML_MESSAGE,
					"Esportazione riuscita con successo!");
		} catch (Exception e) {
			showMessage(EXPORT_XML_MESSAGE,
					"Errore durante il popolamento del file xml!");
			Log.e(TAG, "Exception error occurred while creating xml file");
		}
	}

	/**
	 * 
	 * @param menu
	 * @return
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater mInflater = getMenuInflater();
		mInflater.inflate(R.menu.info_menu, menu);
		return true;
	}

	/**
	 * 
	 * @param item
	 * @return
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case MENU_UPDATE:
			new UpdateInfoTask().execute();
			return true;
		case MENU_EXPORT_XML:
			exportToXML();
			return true;
		case MENU_EDIT:
			setEditDialog();
			return true;
		case MENU_CLEAR_XML:
			boolean success = Utils.deleteFile(INFO_XML_FILE);
			if (success)
				showMessage(CLEAR_XML_MESSAGE, "Dati cancellati.");
			return success;
		default:
			return false;
		}
	}

	private void showMessage(int type, String msg) {
		builder.setMessage(msg);
		infoHandler.sendEmptyMessage(type);
	}

	private void setEditDialog() {
		// LayoutInflater inflater = (LayoutInflater)
		// getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		// final View layout = inflater.inflate(R.layout.info_edit, null);

		int i = 0;
		for (EditText et : listEdit) {
			et.setText(listView.get(i).getText().toString());
			i++;
		}

		ad.show();
	}

	// Handler serve per aggiornare la grafica
	private Handler infoHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int i = 0;

			switch (msg.what) {
			case INIT:
				for (TextView tv : listView) {
					tv.setText(listInfo.get(i));
					i++;
				}
				break;
			case EDIT:
				for (TextView tv : listView) {
					tv.setText(listEdit.get(i).getText().toString());
					i++;
				}
				break;
			case CONNECTION_ERROR:
			case EXPORT_XML_MESSAGE:
			case CLEAR_XML_MESSAGE:
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

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(Info.this, "Please wait...",
					"Loading data ...", true);
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
				return null;
			}
			retriveData(cm.connection(ConnectionManager.ESSE3,
					Utils.TARGET_INFO));
			return null;
		}
	}
}