package com.juliasoft.libretto.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.utils.Utils;

public class IscrizioniOld extends Activity {

	private static final boolean DEBUG = true;
	private static final String TAG = IscrizioniOld.class.getName();

	private static final int SUCCESS = 0;
	private static final int ERROR_MESSAGE = 1;

	private static final int ERROR_DIALOG_ID = 2;
	private static final int PROGRESS_DIALOG_ID = 3;

	private ConnectionManager cm;
	private IscrizioniTask iscrizioniTask;
	private ArrayList<ArrayAdapter<String>> adapters;
	private ArrayList<Map<String, String>> maps;
	private HashMap<String, String> params;
	private int startIndex = 0;
	private String page_HTML;

	private AlertDialog allertDialog;
	private ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.iscrizione_old);
		init();
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

	private Handler iscrizioniHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SUCCESS:
				Intent intent = new Intent(getApplicationContext(),
						Iscrizioni.class);
				String pkg = getPackageName();
				intent.putExtra(pkg + ".page_HTML", page_HTML);
				intent.putExtra(pkg + ".parent", "OLD");
				startActivity(intent);
				break;
			case ERROR_MESSAGE:
				showDialog(ERROR_DIALOG_ID);
				break;
			default:
				break;
			}
		}
	};

	private void init() {
		cm = ConnectionManager.getInstance();

		params = new HashMap<String, String>();
		params.put("id_facolta", "");
		params.put("id_corso", "");
		params.put("id_insegn", "");
		params.put("id_docente", "");

		maps = new ArrayList<Map<String, String>>();
		for (int i = 0; i < 4; i++) {
			maps.add(new HashMap<String, String>());
		}

		adapters = new ArrayList<ArrayAdapter<String>>();
		for (int i = 0; i < 4; i++) {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			adapters.add(adapter);
		}

		Spinner facolta = (Spinner) findViewById(R.id.facolta);
		Spinner corso = (Spinner) findViewById(R.id.corso);
		Spinner insegnamento = (Spinner) findViewById(R.id.insegnamento);
		Spinner docente = (Spinner) findViewById(R.id.docente);
		findViewById(R.id.mostra_appelli).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						if (!params.get("id_corso").equals("")
								&& !params.get("id_insegn").equals("")) {
							doUpdate(true);
						}
					}
				});

		facolta.setAdapter(adapters.get(0));
		corso.setAdapter(adapters.get(1));
		insegnamento.setAdapter(adapters.get(2));
		docente.setAdapter(adapters.get(3));

		facolta.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				String key = (String) parent.getItemAtPosition(pos);
				String num = maps.get(0).get(key);
				if (!params.get("id_facolta").equals(num)) {
					params.put("id_facolta", num);
					startIndex = 1;
					doUpdate(false);
				}
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		corso.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				String key = (String) parent.getItemAtPosition(pos);
				String num = maps.get(1).get(key);
				if (!params.get("id_corso").equals(num)) {
					params.put("id_corso", num);
					startIndex = 2;
					doUpdate(false);
				}
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		insegnamento
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					public void onItemSelected(AdapterView<?> parent,
							View view, int pos, long id) {
						String key = (String) parent.getItemAtPosition(pos);
						String num = maps.get(2).get(key);
						if (!params.get("id_insegn").equals(num))
							params.put("id_insegn", num);
					}

					public void onNothingSelected(AdapterView<?> parent) {
					}
				});

		docente.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				String key = (String) parent.getItemAtPosition(pos);
				String num = maps.get(3).get(key);
				if (!params.get("id_docente").equals(num))
					params.put("id_docente", num);
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		initDialog();
		doUpdate(false);
	}

	private void initDialog() {
		progressDialog = new ProgressDialog(IscrizioniOld.this);
		progressDialog.setTitle("Please wait...");
		progressDialog.setMessage("Loading data ...");
		progressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						dialog.dismiss();
					}
				});

		allertDialog = new AlertDialog.Builder(IscrizioniOld.this)
				.setTitle("Iscrizioni")
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

	private void doUpdate(boolean connect) {
		showDialog(PROGRESS_DIALOG_ID);
		iscrizioniTask = (IscrizioniTask) new IscrizioniTask(connect).execute();
	}

	private void reset() {
		if (startIndex == 1) {
			adapters.get(startIndex).clear();
		}
		adapters.get(2).clear();
		adapters.get(3).clear();
	}

	private void showErrorMessage(String msg) {
		allertDialog.setMessage(msg);
		iscrizioniHandler.sendEmptyMessage(ERROR_MESSAGE);
	}

	private void onTaskCompleted() {
		if (iscrizioniTask.connect)
			iscrizioniHandler.sendEmptyMessage(SUCCESS);
		iscrizioniTask = null;
		removeDialog(PROGRESS_DIALOG_ID);
		if (DEBUG)
			Log.d(TAG, "Task complete.");
	}

	public class IscrizioniTask extends AsyncTask<Void, Void, Void> {

		public boolean connect;

		public IscrizioniTask(boolean connect) {
			this.connect = connect;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			onTaskCompleted();
		}

		@Override
		protected Void doInBackground(Void... value) {
			if (Utils.isNetworkAvailable(IscrizioniOld.this)) {
				if (!isCancelled()) {
					page_HTML = cm.connection(ConnectionManager.SSOL,
							Utils.TARGET_ISCRIZIONI_OLD, params);
					if (!connect)
						runOnUiThread(new UpdateSpinner());
				}
			} else {
				cm.setLogged(false);
				showErrorMessage("Connessione NON attiva!");
			}

			return (null);
		}
	}

	private class UpdateSpinner implements Runnable {

		@Override
		public void run() {
			reset();
			retrieveData();
		}

		private void retrieveData() {
			try {
				if (page_HTML != null) {
					Elements selects = Utils.jsoupSelect(page_HTML,
							"select.TopTabText");

					for (int i = startIndex; i < selects.size(); i++) {
						Elements options = selects.get(i).select("option");
						for (Element option : options) {
							String key = option.text();
							String value = (option.attr("value").equals("-1") ? ""
									: option.attr("value"));
							adapters.get(i).add(key);
							if (!maps.get(i).containsKey(key))
								maps.get(i).put(key, value);
						}
					}
				}
			} catch (Exception e) {
				Utils.appendToLogFile("IscrizioniOLD retrieveData()", e.getMessage());
				if (DEBUG)
					Log.e(TAG, "Error retrieveData(): " + e.getMessage());
			}
		}
	}
}
