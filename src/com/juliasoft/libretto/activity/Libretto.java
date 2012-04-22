package com.juliasoft.libretto.activity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.utils.Esame;
import com.juliasoft.libretto.utils.Row;
import com.juliasoft.libretto.utils.Separator;
import com.juliasoft.libretto.utils.Utils;

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

public class Libretto extends ListActivity {

	public static final String TAG = Libretto.class.getName();

	private static final int MENU_MEDIA = R.id.mitem01;
	private static final int MENU_UPDATE = R.id.mitem03;
	private static final int METHOD_DRAW_SELECTOR_ON_TOP = 0;
	private static final int CONNECTION_ERROR = 5;
	private static final int LOGIN_ERROR = 6;
	private static final int DIALOG_MESSAGE = 7;
	private static final int SHOW_ACTIVITY = 8;

	private Set<Esame> esami;
	private int mMethod;
	private ArrayList<String> pianoStudio = new ArrayList<String>();

	private SeparatedListAdapter adapter;
	private AlertDialog builder;
	private ConnectionManager cm;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		init();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (adapter.getItemViewType(position) == SeparatedListAdapter.TYPE_SEPARATOR)
			return;

		Row row = adapter.getItem(position);
		if (!(row instanceof Esame))
			return;

		Esame esame = (Esame) row;

		Intent myIntent = new Intent(Libretto.this, DettagliEsame.class);

		String pkg = getPackageName();
		myIntent.putExtra(pkg + ".esame", esame.getNome());
		myIntent.putExtra(pkg + ".anno_corso", esame.getAnno_corso());
		myIntent.putExtra(pkg + ".aa_freq", esame.getAa_freq());
		myIntent.putExtra(pkg + ".peso_crediti", esame.getCrediti());
		myIntent.putExtra(pkg + ".data_esame", esame.getData());
		myIntent.putExtra(pkg + ".voto", esame.getVoto());
		myIntent.putExtra(pkg + ".ric", esame.getRic());
		myIntent.putExtra(pkg + ".q_val", esame.getQ_val());
		myIntent.putExtra(pkg + ".img", esame.getStato_gif());
		startActivityForResult(myIntent, 1);
	}

	private void changeMethod(int method) {
		if (mMethod != method) {
			switch (method) {
			case METHOD_DRAW_SELECTOR_ON_TOP:
				mMethod = METHOD_DRAW_SELECTOR_ON_TOP;
				getListView().setSelector(R.drawable.list_selector_on_top);
				getListView().setDrawSelectorOnTop(true);
				break;
			default:
				// Do nothing : this value is not handled
				break;
			}
			getListView().invalidateViews();
		}
	}

	private void showMessage(int type, String msg) {
		builder.setMessage(msg);
		librettoHandler.sendEmptyMessage(type);
	}

	private void init() {
		Intent intent = getIntent();
		String pkg = getPackageName();
		String page_HTML = intent.getStringExtra(pkg + ".lib");

		cm = ConnectionManager.getInstance();

		adapter = new SeparatedListAdapter(Libretto.this, R.layout.libretto_item);
		esami = new HashSet<Esame>();
		pianoStudio = new ArrayList<String>();

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

		changeMethod(METHOD_DRAW_SELECTOR_ON_TOP);

		new LibrettoTask().execute(page_HTML);
	}

	private void retrieveData(String page_HTML) {
		if (page_HTML == null)
			return;

		setPianoStudio();

		Elements trs = Utils.jsoupSelect(page_HTML, "table.detail_table")
				.select("tr:not(:has(th))");

		for (Element tr : trs) {
			try {
				Esame esa = new Esame(tr.children());
				if (pianoStudio.isEmpty())
					adapter.addItem(esa);
				esami.add(esa);
			} catch (Exception e) {
				Log.e(TAG, "Retrieve data: " + e.getMessage());
			}
		}

		if (pianoStudio.isEmpty())
			return;

		for (String s : pianoStudio) {
			if (s.contains("Anno"))
				adapter.addSeparatorItem(new Separator(s));
			else
				for (Esame e: esami)
					if (e.getNome().equals(s))
						adapter.addItem(e);
		}

		adapter.addSeparatorItem(new Separator("Attività formative a scelta dello studente"));
		for (Esame ee: esami)
			if (!pianoStudio.contains(ee.getNome()))
				adapter.addItem(ee);
	}

	private void setPianoStudio() {
		if (cm.isLogged()) {
			try {
				String page_HTML = cm.connection(ConnectionManager.ESSE3,
						Utils.TARGET_PIANO_STUDIO);
				Elements tables = Utils.jsoupSelect(page_HTML, "table.detail_table");

				for (int i = 0; i < tables.size() - 1; i++) {
					pianoStudio.add("Attività didattiche - Anno di corso " + (i + 1));

					Element table = tables.get(i);
					Elements trs = table.select("tr");
					for (Element tr : trs) {
						try {
							pianoStudio.add(tr.select("td").get(1).text());
						} catch (Exception e) {
							// TODO: handle exception
						}
					}
				}
			} catch (Exception e) {
				Log.e(TAG, "Piano studi: " + e.getMessage());
			}
		}
	}

	private class SeparatedListAdapter extends ArrayAdapter<Row> {

		public static final int TYPE_ITEM = 0;
		public static final int TYPE_SEPARATOR = 1;

		private static final int TYPE_MAX_COUNT = TYPE_SEPARATOR + 1;

		private final TreeSet<Integer> mSeparatorsSet;
		private final ArrayList<Row> rows;

		public SeparatedListAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			rows = new ArrayList<Row>();
			mSeparatorsSet = new TreeSet<Integer>();
		}

		public void reset() {
			rows.clear();
			mSeparatorsSet.clear();
			notifyDataSetChanged();
		}

		public void addItem(Row esame) {
			rows.add(esame);
			notifyDataSetChanged();
		}

		public void addSeparatorItem(Separator separator) {
			addItem(separator);
			// save separator position
			mSeparatorsSet.add(rows.size() - 1);
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
			return mSeparatorsSet.contains(position) ? TYPE_SEPARATOR : TYPE_ITEM;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			int type = getItemViewType(position);

			if (row == null) {
				switch (type) {
				case TYPE_ITEM:
					row = getLayoutInflater().inflate(R.layout.libretto_item, parent, false);
					break;
				case TYPE_SEPARATOR:
					row = getLayoutInflater().inflate(R.layout.libretto_separator, parent, false);
					break;
				}
			}
			switch (type) {
			case TYPE_ITEM: {
				Esame esame = (Esame) rows.get(position);

				ImageView v = (ImageView) row.findViewById(R.id.bar);
				TextView label = (TextView) row.findViewById(R.id.esame);
				label.setText(esame.toString());

				TextView voto = (TextView) row.findViewById(R.id.li_voto);
				voto.setText(esame.getVoto());
				if (esame.getVoto() != null && esame.getVoto().length() > 0)
					v.setBackgroundResource(R.layout.green_bar);
				else
					v.setBackgroundResource(R.layout.red_bar);

				break;
			}
			case TYPE_SEPARATOR: {
				Esame esame = (Esame) rows.get(position);
				TextView vi = (TextView) row.findViewById(R.id.separator);
				vi.setText(esame.getNome());
				break;
			}
			}

			return row;
		}
	}

	/**
	 * @param numero
	 * @param nCifreDecimali
	 * @return
	 */
	private double arrotonda(double numero, int nCifreDecimali) {
		return Math.round(numero * Math.pow(10, nCifreDecimali)) / Math.pow(10, nCifreDecimali);
	}

	private double mediaAritmetica() {
		int somma = 0;

		for (Esame e: esami)
			if (e.getVoto().equals("30L"))
				somma += 30;
			else
				try {
					somma += Integer.parseInt(e.getVoto());
				}
				catch (NumberFormatException ex) {}

		try {
			return arrotonda((double) somma / numeroEsami(), 2);
		} catch (Exception e) {
			return 0.0;
		}
	}

	private double mediaPonderata() {
		int count = 0, somma = 0;

		for (Esame e: esami) {
			String voto = e.getVoto();

			try {
				int crediti = voto.equals("30L") ? 30 : Integer.parseInt(e.getCrediti());
				somma += Integer.parseInt(e.getVoto()) * crediti;
				count += crediti;
			}
			catch (NumberFormatException ex) {}
		}

		try {
			return arrotonda((double) somma / count, 2);
		} catch (Exception e) {
			return 0.0;
		}
	}

	private int numeroEsami() {
		int result = 0;

		for (Esame e: esami)
			if (!e.getVoto().equals("APPR") && !e.getVoto().equals("IDO"))
				result++;

		return result;
	}

	private int creditiSostenuti() {
		int result = 0;
		for (Esame e: esami) {
			String voto = e.getVoto();

			try {
				Integer.parseInt(e.getVoto());
				if (e.getData() != null)
					result += Integer.parseInt(e.getCrediti());
			} catch (Exception ex) {
				if (voto.equals("30L") || e.getVoto().equals("APPR") || e.getVoto().equals("IDO"))
					result += Integer.parseInt(e.getCrediti());
			}
		}

		return result;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.lib_menu, menu);
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_MESSAGE)
			return builder;
		else
			return super.onCreateDialog(id);
	}

	/**
	 * 
	 * @param item
	 * @return
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_MEDIA:
			librettoHandler.sendEmptyMessage(SHOW_ACTIVITY);
			return true;
		case MENU_UPDATE:
			new LibrettoTask().execute();
			return true;
		default:
			return false;
		}
	}

	// Handler serve per aggiornare la grafica
	private Handler librettoHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case SHOW_ACTIVITY:
				Intent intent = new Intent(getApplicationContext(), Medie.class);
				String pkg = getPackageName();

				intent.putExtra(pkg + ".aritm", String.valueOf(mediaAritmetica()));
				intent.putExtra(pkg + ".pond", String.valueOf(mediaPonderata()));
				intent.putExtra(pkg + ".num", String.valueOf(numeroEsami()));
				intent.putExtra(pkg + ".crediti", String.valueOf(creditiSostenuti()));

				startActivity(intent);
				break;
			case CONNECTION_ERROR:
			case LOGIN_ERROR:
				showDialog(DIALOG_MESSAGE);
				break;
			}
		}
	};

	public class LibrettoTask extends AsyncTask<String, Void, Void> {

		private final ProgressDialog dialog;

		public LibrettoTask() {
			dialog = new ProgressDialog(Libretto.this);
			dialog.setTitle("Please wait...");
			dialog.setCancelable(true);
		}

		@Override
		protected void onPreExecute() {
			dialog.setMessage("Loading data ...");
			dialog.show();
			reset();
		}

		private void reset() {
			adapter.reset();
			adapter = new SeparatedListAdapter(Libretto.this, R.layout.libretto_item);
			pianoStudio.clear();
			esami.clear();
		}

		@Override
		protected void onPostExecute(Void success) {
			if (dialog.isShowing())
				dialog.dismiss();
			setListAdapter(adapter);
			registerForContextMenu(getListView());
		}

		@Override
		protected Void doInBackground(String... params) {
			if (params != null && params.length > 0)
				retrieveData(params[0]);
			else if (!Utils.isNetworkAvailable(Libretto.this)) {
				cm.setLogged(false);
				showMessage(CONNECTION_ERROR, "Connessione NON attiva!");
			}
			else
				retrieveData(cm.connection(ConnectionManager.ESSE3, Utils.TARGET_LIBRETTO));

			return null;
		}
	}
}