package com.juliasoft.libretto.activity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.utils.Esame;
import com.juliasoft.libretto.utils.Row;
import com.juliasoft.libretto.utils.Separator;
import com.juliasoft.libretto.utils.Utils;

public class Libretto extends ListActivity {

	private static final boolean DEBUG = true;
	private static final String TAG = Libretto.class.getName();

	private static final int SHOW_ACTIVITY = 0;
	private static final int ERROR_MESSAGE = 1;

	private static final int ERROR_DIALOG_ID = 2;
	private static final int PROGRESS_DIALOG_ID = 3;

	private static final int MENU_MEDIA = R.id.libretto_menu_statistiche;
	private static final int MENU_UPDATE = R.id.libretto_menu_update;

	private LibrettoTask librettoTask;
	private Set<Esame> esami;
	private ArrayList<String> pianoStudio;

	private SeparatedListAdapter adapter;
	private AlertDialog allertDialog;
	private ProgressDialog progressDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		init();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.lib_menu, menu);
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
		case MENU_MEDIA:
			librettoHandler.sendEmptyMessage(SHOW_ACTIVITY);
			return true;
		case MENU_UPDATE:
			doUpdate();
			return true;
		default:
			return false;
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (adapter.getItemViewType(position) == SeparatedListAdapter.TYPE_SEPARATOR)
			return;

		Row row = adapter.getItem(position);
		if (!(row instanceof Esame))
			return;

		Esame esame = (Esame) row;

		Intent detailsIntent = new Intent(Libretto.this, DettagliEsame.class);

		String pkg = getPackageName();
		detailsIntent.putExtra(pkg + ".esame", esame.getNome());
		detailsIntent.putExtra(pkg + ".anno_corso", esame.getAnno_corso());
		detailsIntent.putExtra(pkg + ".aa_freq", esame.getAa_freq());
		detailsIntent.putExtra(pkg + ".peso_crediti", esame.getCrediti());
		detailsIntent.putExtra(pkg + ".data_esame", esame.getData());
		detailsIntent.putExtra(pkg + ".voto", esame.getVoto());
		detailsIntent.putExtra(pkg + ".ric", esame.getRic());
		detailsIntent.putExtra(pkg + ".q_val", esame.getQ_val());
		detailsIntent.putExtra(pkg + ".img", esame.getStato_gif());
		startActivity(detailsIntent);
	}

	private Handler librettoHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case SHOW_ACTIVITY:
				Intent statisticsIntent = new Intent(getApplicationContext(),
						Medie.class);
				String pkg = getPackageName();

				statisticsIntent.putExtra(pkg + ".aritm",
						String.valueOf(mediaAritmetica()));
				statisticsIntent.putExtra(pkg + ".pond",
						String.valueOf(mediaPonderata()));
				statisticsIntent.putExtra(pkg + ".num",
						String.valueOf(numeroEsami()));
				statisticsIntent.putExtra(pkg + ".crediti",
						String.valueOf(creditiSostenuti()));

				startActivity(statisticsIntent);
				break;
			case ERROR_MESSAGE:
				showDialog(ERROR_DIALOG_ID);
				break;
			}
		}
	};

	private void init() {
		adapter = new SeparatedListAdapter(Libretto.this,
				R.layout.libretto_item);
		esami = new HashSet<Esame>();
		pianoStudio = new ArrayList<String>();

		initDialog();

		getListView().setSelector(R.drawable.list_selector_on_top);
		getListView().setDrawSelectorOnTop(true);
		getListView().invalidateViews();

		doUpdate();
	}

	private void initDialog() {
		progressDialog = new ProgressDialog(Libretto.this);
		progressDialog.setTitle("Please wait...");
		progressDialog.setMessage("Loading data ...");
		progressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						librettoTask.cancel(true);
					}
				});

		allertDialog = new AlertDialog.Builder(Libretto.this)
				.setTitle("Libretto")
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

	private void doUpdate() {
		showDialog(PROGRESS_DIALOG_ID);
		librettoTask = (LibrettoTask) new LibrettoTask().execute();
	}

	private void retrieveData(String page_HTML) {
		try {
			if (page_HTML != null) {

				Elements trs = Utils.jsoupSelect(page_HTML,
						"table.detail_table").select("tr:not(:has(th))");

				for (Element tr : trs)
					try {
						esami.add(new Esame(tr.children()));
					} catch (Exception e) {
						if (DEBUG)
							Log.e(TAG, "Retrieve data: " + e.getMessage());
					}

				if (!pianoStudio.isEmpty()) {
					for (String id : pianoStudio)
						if (id.contains("Anno"))
							adapter.addSeparatorItem(new Separator(id));
						else
							for (Esame e : esami)
								if (e.getId().equals(id))
									adapter.addItem(e);

					adapter.addSeparatorItem(new Separator(
							"Attività formative a scelta dello studente"));
					for (Esame e : esami)
						if (!pianoStudio.contains(e.getId()))
							adapter.addItem(e);
				} else
					for (Esame e : esami)
						adapter.addItem(e);
			}
		} catch (Exception e) {
			Utils.appendToLogFile("Libretto retrieveData()", e.getMessage());
		}
	}

	private void onTaskCompleted() {
		setListAdapter(adapter);
		registerForContextMenu(getListView());
		removeDialog(PROGRESS_DIALOG_ID);
		librettoTask = null;
		if (DEBUG)
			Log.i(TAG, "Task complete.");
	}

	private void showErrorMessage(String msg) {
		allertDialog.setMessage(msg);
		librettoHandler.sendEmptyMessage(ERROR_MESSAGE);
	}

	private int numeroEsami() {
		int result = 0;

		for (Esame e : esami)
			if (!e.getVoto().equals("APPR") && !e.getVoto().equals("IDO")
					&& !e.getVoto().equals(""))
				result++;

		return result;
	}

	private int creditiSostenuti() {
		int result = 0;
		for (Esame e : esami) {
			String voto = e.getVoto();
			int crediti = Integer.parseInt(e.getCrediti());
			try {
				Integer.parseInt(voto);
				result += crediti;
			} catch (NumberFormatException ex) {
				if (voto.equals("30L") || voto.equals("APPR")
						|| voto.equals("IDO"))
					result += crediti;
			}
		}

		return result;
	}

	private double mediaAritmetica() {
		int somma = 0;

		for (Esame e : esami) {
			String voto = e.getVoto();
			if (voto.equals("30L"))
				somma += 30;
			else
				try {
					somma += Integer.parseInt(voto);
				} catch (NumberFormatException ex) {
				}
		}
		try {
			return arrotonda((double) somma / numeroEsami(), 2);
		} catch (Exception e) {
			return 0.0;
		}
	}

	private double mediaPonderata() {
		int count = 0, somma = 0;

		for (Esame e : esami) {
			String voto = e.getVoto();

			try {
				int crediti = voto.equals("30L") ? 30 : Integer.parseInt(e
						.getCrediti());
				somma += Integer.parseInt(voto) * crediti;
				count += crediti;
			} catch (NumberFormatException ex) {
			}
		}

		try {
			return arrotonda((double) somma / count, 2);
		} catch (Exception e) {
			return 0.0;
		}
	}

	private double arrotonda(double numero, int nCifreDecimali) {
		return Math.round(numero * Math.pow(10, nCifreDecimali))
				/ Math.pow(10, nCifreDecimali);
	}

	private class SeparatedListAdapter extends ArrayAdapter<Row> {

		public static final int TYPE_ITEM = 0;
		public static final int TYPE_SEPARATOR = 1;
		public static final int TYPE_MAX_COUNT = 2;

		private final TreeSet<Integer> separatorsSet;
		private final ArrayList<Row> rows;

		public SeparatedListAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			rows = new ArrayList<Row>();
			separatorsSet = new TreeSet<Integer>();
		}

		public void reset() {
			rows.clear();
			separatorsSet.clear();
			notifyDataSetChanged();
		}

		public void addItem(Row esame) {
			rows.add(esame);
			notifyDataSetChanged();
		}

		public void addSeparatorItem(Separator separator) {
			rows.add(separator);
			separatorsSet.add(rows.size() - 1);
			notifyDataSetChanged();
		}

		@Override
		public int getViewTypeCount() {
			return TYPE_MAX_COUNT;
		}

		@Override
		public int getCount() {
			return rows.size();
		}

		@Override
		public Row getItem(int position) {
			return rows.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemViewType(int position) {
			return separatorsSet.contains(position) ? TYPE_SEPARATOR
					: TYPE_ITEM;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			int type = getItemViewType(position);

			if (row == null) {
				switch (type) {
				case TYPE_ITEM:
					row = getLayoutInflater().inflate(R.layout.libretto_item,
							parent, false);
					break;
				case TYPE_SEPARATOR:
					row = getLayoutInflater().inflate(
							R.layout.libretto_separator, parent, false);
					break;
				}
			}

			switch (type) {
			case TYPE_ITEM:
				Esame esame = (Esame) rows.get(position);

				ImageView v = (ImageView) row
						.findViewById(R.id.libretto_item_bar);
				TextView label = (TextView) row
						.findViewById(R.id.libretto_item_esame);
				label.setText(esame.toString());

				TextView voto = (TextView) row
						.findViewById(R.id.libretto_item_voto);
				voto.setText(esame.getVoto());

				if (esame.getVoto() != null && esame.getVoto().length() > 0)
					v.setBackgroundResource(R.layout.green_bar);
				else
					v.setBackgroundResource(R.layout.red_bar);

				break;
			case TYPE_SEPARATOR:
				Separator separator = (Separator) rows.get(position);
				TextView vi = (TextView) row.findViewById(R.id.separator);
				vi.setText(separator.getNome());
				break;
			}

			return row;
		}
	}

	public class LibrettoTask extends AsyncTask<Void, Void, Void> {

		private ConnectionManager cm;

		public LibrettoTask() {
			cm = ConnectionManager.getInstance();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			doReset();
		}

		@Override
		protected void onPostExecute(Void success) {
			super.onPostExecute(success);
			onTaskCompleted();
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (Utils.isNetworkAvailable(Libretto.this)) {
				if (!isCancelled())
					initStudyPlan();
				if (!isCancelled())
					retrieveData(cm.connection(ConnectionManager.ESSE3,
							Utils.TARGET_LIBRETTO, null));
			} else {
				cm.setLogged(false);
				showErrorMessage("Connessione NON attiva!");
			}
			return (null);
		}

		private void initStudyPlan() {
			try {
				String page_HTML = cm.connection(ConnectionManager.ESSE3,
						Utils.TARGET_PIANO_STUDIO, null);
				Elements tables = Utils.jsoupSelect(page_HTML,
						"table.detail_table");

				for (int i = 0; i < tables.size() - 1; i++) {
					pianoStudio.add("Attività didattiche - Anno di corso "
							+ (i + 1));
					Elements trs = tables.get(i).select("tr:not(:has(th))");
					for (Element tr : trs)
						pianoStudio.add(tr.select("td").get(0).text());
				}
			} catch (Exception e) {
				Utils.appendToLogFile("Libretto initStudyPlan()", e.getMessage());
				if (DEBUG)
					Log.e(TAG, "Piano studi: " + e.getMessage());
				pianoStudio.clear();
			}
		}

		private void doReset() {
			adapter.reset();
			adapter = new SeparatedListAdapter(Libretto.this,
					R.layout.libretto_item);
			pianoStudio.clear();
			esami.clear();
		}
	}
}