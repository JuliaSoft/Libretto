package com.juliasoft.libretto.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.utils.Utils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class IscrizioniOld extends Activity {

	private static final String TAG = IscrizioniOld.class.getName();

	private ConnectionManager cm;

	private int startIndex = 0;

	private String currentCorso = "";
	private String currentFacolta = "2";
	private String currentDocente = "";
	private String currentInsegn = "";
	private String page_data;

	private ArrayList<ArrayAdapter<String>> adapters;
	private ArrayAdapter<String> adapterFacolta;
	private ArrayAdapter<String> adapterCorsi;
	private ArrayAdapter<String> adapterInsegn;
	private ArrayAdapter<String> adapterDocenti;

	private ArrayList<Map<String, String>> maps;

	private Spinner corso;
	private Spinner insegnamento;
	private Spinner docente;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.iscrizione_old);
		init();
	}

	private void init() {
		Intent intent = getIntent();
		String pkg = getPackageName();
		String page_HTML = intent.getStringExtra(pkg + ".iscriz");

		cm = ConnectionManager.getInstance();

		final Map<String, String> facoltaMap = new HashMap<String, String>();
		final Map<String, String> corsoMap = new HashMap<String, String>();
		final Map<String, String> insegnMap = new HashMap<String, String>();
		final Map<String, String> docMap = new HashMap<String, String>();

		maps = new ArrayList<Map<String, String>>();
		maps.add(facoltaMap);
		maps.add(corsoMap);
		maps.add(insegnMap);
		maps.add(docMap);

		Spinner facolta = (Spinner) findViewById(R.id.facolta);
		corso = (Spinner) findViewById(R.id.corso);
		insegnamento = (Spinner) findViewById(R.id.insegnamento);
		docente = (Spinner) findViewById(R.id.docente);
		findViewById(R.id.mostra_appelli).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!currentCorso.equals("") && !currentInsegn.equals("")) {
					String url = Utils.TARGET_ISCRIZIONI_OLD + "&id_facolta="
							+ currentFacolta + "&id_corso=" + currentCorso
							+ "&id_insegn=" + currentInsegn + "&id_docente="
							+ currentDocente;
					new MyAsyncTask(true).execute(url);
				}
			}
		});

		adapterFacolta = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		adapterFacolta.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		facolta.setAdapter(adapterFacolta);
		adapterCorsi = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		adapterCorsi.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		corso.setAdapter(adapterCorsi);
		adapterInsegn = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		adapterInsegn.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		insegnamento.setAdapter(adapterInsegn);
		adapterDocenti = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		adapterDocenti
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		docente.setAdapter(adapterDocenti);

		adapters = new ArrayList<ArrayAdapter<String>>();
		adapters.add(adapterFacolta);
		adapters.add(adapterCorsi);
		adapters.add(adapterInsegn);
		adapters.add(adapterDocenti);

		facolta.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				String key = (String) parent.getItemAtPosition(pos);
				String num = facoltaMap.get(key);
				if (currentFacolta.equals(num))
					return;

				currentFacolta = num;
				startIndex = 1;
				new MyAsyncTask(false).execute(Utils.TARGET_ISCRIZIONI_OLD
						+ "&id_facolta=" + num + "&id_corso=" + currentCorso);
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		corso.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				String key = (String) parent.getItemAtPosition(pos);
				String num = corsoMap.get(key);
				if (currentCorso.equals(num))
					return;

				currentCorso = num;
				startIndex = 2;
				new MyAsyncTask(false).execute(Utils.TARGET_ISCRIZIONI_OLD
						+ "&id_facolta=" + currentFacolta + "&id_corso=" + num);
				corso.setSelection(pos);
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		insegnamento.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				String key = (String) parent.getItemAtPosition(pos);
				String num = insegnMap.get(key);
				if (currentInsegn.equals(num))
					return;

				currentInsegn = num;
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		docente.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				String key = (String) parent.getItemAtPosition(pos);
				String num = docMap.get(key);
				if (currentDocente.equals(num))
					return;

				currentDocente = num;
			}

			public void onNothingSelected(AdapterView<?> parent) {}
		});

		retrieveData(page_HTML);
	}

	private void reset() {
		adapters.clear();

		if (startIndex == 1) {
			// reset item corsi
			adapterCorsi.clear();
			adapterCorsi = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
			adapterCorsi.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}
		adapterInsegn.clear();
		adapterDocenti.clear();
		// reset item insegnamenti
		adapterInsegn = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		adapterInsegn.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// reset item docenti
		adapterDocenti = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		adapterDocenti.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		adapters.add(adapterFacolta);
		adapters.add(adapterCorsi);
		adapters.add(adapterInsegn);
		adapters.add(adapterDocenti);
	}

	private void retrieveData(String page_HTML) {
		if (page_HTML != null) {
			// Ricavo le informazioni degli appelli di ogni singolo esame a cui lo
			// studente si pu� iscrivere
			Elements selects = Utils.jsoupSelect(page_HTML, "select.TopTabText");
	
			for (int i = startIndex; i < selects.size(); i++) {
				Elements options = selects.get(i).select("option");
				for (Element option : options) {
					String key = option.text();
					String value = (option.attr("value").equals("-1") ? "" : option.attr("value"));
					adapters.get(i).add(key);
					if (!maps.get(i).containsKey(key))
						maps.get(i).put(key, value);
				}
			}
		}
	}

	private String connect(String url) {
		cm.getSsolConnection().get(url);
		try {
			return Utils.inputStreamToString(cm.getSsolConnection().getEntity().getContent());
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			return "";
		}
	}

	public class MyAsyncTask extends AsyncTask<String, Void, Void> {

		private ProgressDialog progressDialog;
		private final boolean connect;

		public MyAsyncTask(boolean connect) {
			this.connect = connect;
		}

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(IscrizioniOld.this, "Please wait...", "Loading data ...", true);
			if (!connect)
				reset();
		}

		@Override
		protected void onPostExecute(Void result) {
			if (connect) {
				Intent intent = new Intent(getApplicationContext(), Iscrizioni.class);
				String pkg = getPackageName();
				// setto i dati ricavati dal login
				intent.putExtra(pkg + ".iscriz", page_data);
				intent.putExtra(pkg + ".type", "OLD");
				startActivity(intent);
			} else {
				if (startIndex == 1)
					corso.setAdapter(adapterCorsi);

				insegnamento.setAdapter(adapterInsegn);
				docente.setAdapter(adapterDocenti);
			}
			progressDialog.dismiss();
		}

		@Override
		protected Void doInBackground(String... params) {
			if (params != null && params.length > 0)
				if (connect)
					page_data = connect(params[0]);
				else
					retrieveData(connect(params[0]));

			return null;
		}
	}
}