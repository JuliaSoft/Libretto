package com.juliasoft.libretto.activity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.connection.Esse3HttpClient;
import com.juliasoft.libretto.utils.Esame;
import com.juliasoft.libretto.utils.Utils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlSerializer;

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
import android.util.Xml;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class Libretto extends ListActivity {

	public static final String TAG = Libretto.class.getName();

	private static final int MENU_MEDIA = R.id.mitem01;
	private static final int MENU_EXPORT_XML = R.id.mitem02;
	private static final int MENU_UPDATE = R.id.mitem03;
	private static final int MENU_CLEAR_XML = R.id.mitem04;
	private static final int METHOD_DRAW_SELECTOR_ON_TOP = 0;
	private static final int CONTEXT_MENU_DELETE_ITEM = 1;
	private static final int CONTEXT_MENU_EDIT = 2;
	private static final int EXPORT_XML_MESSAGE = 3;
	private static final int CLEAR_XML_MESSAGE = 4;
	private static final int CONNECTION_ERROR = 5;
	private static final int LOGIN_ERROR = 6;
	private static final int DIALOG_MESSAGE = 7;
	private static final int SHOW_ACTIVITY = 8;

	private static final String LIBRETTO_XML_FILE = "Libretto.xml";

	private Set<Esame> esami;
	private int tot_crediti_sost;
	private int tot_esami_sost;
	private int mMethod;
	private int selected_item;
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
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		Esame esame = (Esame) adapter.getItem(info.position);
		menu.setHeaderTitle(esame.getNome());
		menu.add(Menu.NONE, CONTEXT_MENU_DELETE_ITEM, Menu.NONE, "Delete");
		menu.add(Menu.NONE, CONTEXT_MENU_EDIT, Menu.NONE, "Edit");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		if (adapter.getItemViewType(info.position) == SeparatedListAdapter.TYPE_SEPARATOR) {
			return false;
		}

		switch (item.getItemId()) {
		case CONTEXT_MENU_DELETE_ITEM:
			Esame esame = (Esame) adapter.getItem(info.position);
			esami.remove(esame);
			adapter.removeItem(info.position);
			return (true);
		case CONTEXT_MENU_EDIT:
			selected_item = info.position;
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View layout = inflater.inflate(R.layout.dialog_edit_esame,
					null);
			setEditDialog(layout);
			AlertDialog ad = new AlertDialog.Builder(this)
					.setView(layout)
					.setTitle("Edit")
					.setIcon(R.drawable.edit)
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									String name = ((EditText) layout
											.findViewById(R.id.et_edit_name))
											.getText().toString();
									DatePicker dp = (DatePicker) layout
											.findViewById(R.id.dp_edit_date);
									int day = dp.getDayOfMonth();
									int month = dp.getMonth();
									int year = dp.getYear();
									String aaf = ((Spinner) layout
											.findViewById(R.id.sp_edit_aaf))
											.getSelectedItem().toString();
									String voto = ((Spinner) layout
											.findViewById(R.id.sp_edit_voto))
											.getSelectedItem().toString();
									String crediti = ((Spinner) layout
											.findViewById(R.id.sp_edit_crediti))
											.getSelectedItem().toString();
									Esame e = (Esame) adapter
											.getItem(selected_item);
									e.setNome(name);
									e.setData_esame(day + "/" + month + "/"
											+ year);
									e.setAa_freq(aaf);
									e.setVoto(voto);
									e.setPeso_crediti(crediti);

									adapter.replaceItem(e, selected_item);
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
			ad.show();
			return (true);
		}
		return (super.onOptionsItemSelected(item));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		if (adapter.getItemViewType(position) == SeparatedListAdapter.TYPE_SEPARATOR) {
			return;
		}

		final Esame esame = (Esame) adapter.getItem(position);
		Intent myIntent = new Intent(Libretto.this, DettagliEsame.class);

		String pkg = getPackageName();
		myIntent.putExtra(pkg + ".esame", esame.getNome());
		myIntent.putExtra(pkg + ".anno_corso", esame.getAnno_corso());
		myIntent.putExtra(pkg + ".aa_freq", esame.getAa_freq());
		myIntent.putExtra(pkg + ".peso_crediti", esame.getPeso_crediti());
		myIntent.putExtra(pkg + ".data_esame", esame.getData_esame());
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

	private boolean loadXML() {
		Log.i(TAG, "Load XML");
		FileInputStream fileis = Utils.loadXMLFile(LIBRETTO_XML_FILE);
		if (fileis == null) {
			return false;
		}

		String xml = Utils.inputStreamToString(fileis);
		Elements anni = Utils.jsoupSelect(xml, "libretto>anno");

		if (!anni.isEmpty()) {
			for (Element anno : anni) {
				Esame e = new Esame();
				e.setNome(anno.attr("nome"));
				adapter.addSeparatorItem(e);
				for (Element esame : anno.select("esame")) {
					e = new Esame();
					e.setNome(esame.attr("nome"));
					e.setAnno_corso(esame.attr("annocorso"));
					e.setAa_freq(esame.attr("annofreq"));
					e.setPeso_crediti(esame.attr("crediti"));
					e.setData_esame(esame.attr("data"));
					e.setVoto(esame.attr("voto"));
					e.setRic(esame.attr("ric"));
					e.setQ_val(esame.attr("qval"));
					e.setStato_gif(esame.attr("image"));

					adapter.addItem(e);

					esami.add(e);
				}
			}

		} else {

			Elements esami = Utils.jsoupSelect(xml, "libretto>esame");

			for (Element esame : esami) {
				Esame e = new Esame();
				e.setNome(esame.attr("nome"));
				e.setAnno_corso(esame.attr("annocorso"));
				e.setAa_freq(esame.attr("annofreq"));
				e.setPeso_crediti(esame.attr("crediti"));
				e.setData_esame(esame.attr("data"));
				e.setVoto(esame.attr("voto"));
				e.setRic(esame.attr("ric"));
				e.setQ_val(esame.attr("qval"));
				e.setStato_gif(esame.attr("image"));
				adapter.addItem(e);
				this.esami.add(e);
			}
		}

		Log.i(TAG, "Load XML completato");
		return true;
	}

	private void exportToXML() {

		FileOutputStream fileos = Utils.createXMLFile(LIBRETTO_XML_FILE);

		if (fileos == null) {
			showMessage(EXPORT_XML_MESSAGE, "Errore durante la creazione del file!");
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
			serializer.startTag(null, "LIBRETTO");

			int pos = 0;
			for (Esame esame : adapter.esami) {
				switch (adapter.getItemViewType(pos)) {
				case SeparatedListAdapter.TYPE_ITEM:
					serializer.startTag(null, "ESAME");
					serializer.attribute(null, "nome", esame.getNome());
					serializer.attribute(null, "annocorso",
							esame.getAnno_corso());
					serializer.attribute(null, "annofreq", esame.getAa_freq());
					serializer.attribute(null, "crediti",
							esame.getPeso_crediti());
					serializer.attribute(null, "data", esame.getData_esame());
					serializer.attribute(null, "voto", esame.getVoto());
					serializer.attribute(null, "ric", esame.getRic());
					serializer.attribute(null, "qval", esame.getQ_val());
					serializer.attribute(null, "image", esame.getStato_gif());
					serializer.endTag(null, "ESAME");
					break;
				case SeparatedListAdapter.TYPE_SEPARATOR:
					if (serializer.getName().equals("ANNO")) {
						serializer.endTag(null, "ANNO");
					}
					serializer.startTag(null, "ANNO");
					serializer.attribute(null, "nome", esame.getNome());

					break;
				default:
					break;
				}
				pos++;
			}
			if (serializer.getName().equals("ANNO")) {
				serializer.endTag(null, "ANNO");
			}

			serializer.endTag(null, "LIBRETTO");
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
			e.printStackTrace();
		}
	}

	private void showMessage(int type, String msg) {
		builder.setMessage(msg);
		librettoHandler.sendEmptyMessage(type);
	}

	private void reset() {
		adapter.reset();
		adapter = new SeparatedListAdapter(Libretto.this,
				R.layout.libretto_item);
		pianoStudio.clear();
		esami.clear();
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
		builder.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

		changeMethod(METHOD_DRAW_SELECTOR_ON_TOP);

		new LibrettoTask().execute(page_HTML);
	}

	private void retrieveData(String page_HTML) {
		if (page_HTML == null) {
			return;
		}

		setPianoStudio();

		Elements trs = Utils.jsoupSelect(page_HTML, "table.detail_table")
				.select("tr:not(:has(th))");

		for (Element tr : trs) {
			Esame esa = new Esame();
			Elements tds = tr.children();
			try {
				String e = tds.get(2).text().split(" - ", 2)[1];

				esa.setNome(e);
				esa.setAnno_corso(tds.get(1).text());
				esa.setStato_gif(Esse3HttpClient.AUTH_URI
						+ tds.get(8).select("img").first().attr("src"));
				esa.setAa_freq(tds.get(9).text());
				esa.setPeso_crediti(tds.get(10).text());
				esa.setData_esame(tds.get(11).text());
				esa.setVoto(tds.get(12).text());
				esa.setRic(tds.get(14).text());
				esa.setQ_val(tds.get(15).text());
				if (pianoStudio.isEmpty())
					adapter.addItem(esa);
				esami.add(esa);
			} catch (Exception e) {
				Log.e(TAG, "Retrive data: " + e.getMessage());
			}
		}

		if (pianoStudio.isEmpty())
			return;

		for (String s : pianoStudio) {
			if (s.contains("Anno")) {
				Esame e = new Esame();
				e.setNome(s);
				adapter.addSeparatorItem(e);
			} else
				for (Esame e: esami)
					if (e.getNome().equals(s))
						adapter.addItem(e);
		}

		Esame e = new Esame();
		e.setNome("Attività formative a scelta dello studente");
		adapter.addSeparatorItem(e);
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
							Element td = tr.select("td").get(1);
							pianoStudio.add(td.text());
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

	private class SeparatedListAdapter extends ArrayAdapter<Object> {

		public static final int TYPE_ITEM = 0;
		public static final int TYPE_SEPARATOR = 1;

		private static final int TYPE_MAX_COUNT = TYPE_SEPARATOR + 1;

		private TreeSet<Integer> mSeparatorsSet;
		private ArrayList<Esame> esami;

		public SeparatedListAdapter(Context context, int textViewResourceId) {
			// TODO Auto-generated constructor stub
			super(context, textViewResourceId);
			esami = new ArrayList<Esame>();
			mSeparatorsSet = new TreeSet<Integer>();
		}

		public void reset() {
			esami.clear();
			mSeparatorsSet.clear();
			notifyDataSetChanged();
		}

		public void addItem(Esame esame) {
			esami.add(esame);
			notifyDataSetChanged();
		}

		public void addSeparatorItem(Esame esame) {
			addItem(esame);
			// save separator position
			mSeparatorsSet.add(esami.size() - 1);
		}

		@Override
		public int getViewTypeCount() {
			return TYPE_MAX_COUNT;
		}

		@Override
		public int getCount() {
			return esami.size();
		}

		@Override
		public Object getItem(int position) {
			return esami.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		public void removeItem(int position) {
			esami.remove(position);
			ArrayList<Integer> toRemove = new ArrayList<Integer>();
			ArrayList<Integer> toAdd = new ArrayList<Integer>();
			Iterator<Integer> iter = mSeparatorsSet.iterator();
			while (iter.hasNext()) {
				int pos = iter.next();
				if (pos > position) {
					toRemove.add(pos);
					toAdd.add(pos - 1);
				}
			}
			mSeparatorsSet.removeAll(toRemove);
			mSeparatorsSet.addAll(toAdd);

			notifyDataSetChanged();
		}

		public void replaceItem(Esame esame, int position) {
			esami.set(position, esame);
			notifyDataSetChanged();
		}

		@Override
		public int getItemViewType(int position) {
			return mSeparatorsSet.contains(position) ? TYPE_SEPARATOR
					: TYPE_ITEM;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			int type = getItemViewType(position);

			if (convertView == null) {
				switch (type) {
				case TYPE_ITEM:
					LayoutInflater inflater = getLayoutInflater();
					row = inflater.inflate(R.layout.libretto_item, parent,
							false);
					break;
				case TYPE_SEPARATOR:
					inflater = getLayoutInflater();
					row = inflater.inflate(R.layout.libretto_separator, parent,
							false);
					break;
				}
			}
			switch (type) {
			case TYPE_ITEM:

				final Esame esame = (Esame) esami.get(position);

				ImageView v = (ImageView) row.findViewById(R.id.bar);
				TextView label = (TextView) row.findViewById(R.id.esame);
				label.setText(esame.toString());

				TextView voto = (TextView) row.findViewById(R.id.li_voto);
				voto.setText(esame.getVoto());
				if (esame.getVoto() != null && esame.getVoto().length() > 0) {
					v.setBackgroundResource(R.layout.green_bar);
				} else {
					v.setBackgroundResource(R.layout.red_bar);
				}
				break;
			case TYPE_SEPARATOR:

				Esame e = (Esame) esami.get(position);
				TextView vi = (TextView) row.findViewById(R.id.separator);
				vi.setText(e.getNome());
				break;
			}

			return row;
		}
	}

	private double mediaAritmetica() {
		double somma = 0;
		for (Esame e: esami) {
			if (e.getData_esame() == null || e.getData_esame().equals(""))
				continue;
			try {

				somma += Integer.parseInt(e.getVoto());
			} catch (Exception ex) {
				if (e.getVoto().equals("30L")) {
					somma += 30;
				}
			}
		}

		try {
			return Utils.arrotonda(somma / numeroEsami(), 2);
		} catch (Exception e) {
			return 0;
		}
	}

	private double mediaPonderata() {
		int count = 0;
		double somma = 0;
		int crediti = 0;
		for (Esame e: esami) {
			try {
				crediti = Integer.parseInt(e.getPeso_crediti());
				somma += Integer.parseInt(e.getVoto()) * crediti;
				count += crediti;
			} catch (Exception ex) {
				if (e.getVoto().equals("30L")) {
					somma += 30 * crediti;
					count += crediti;
				}
			}
		}
		try {
			return Utils.arrotonda(somma / count, 2);
		} catch (Exception e) {
			return 0;
		}
	}

	private int numeroEsami() {
		tot_esami_sost = 0;
		tot_crediti_sost = 0;
		int no_media = 0;
		for (Esame e: esami) {
			try {
				Integer.parseInt(e.getVoto());
				tot_esami_sost++;
				if (e.getData_esame() != null)
					tot_crediti_sost += Integer.parseInt(e.getPeso_crediti());
			} catch (Exception ex) {
				if (e.getVoto().equals("30L")) {
					tot_esami_sost++;
					tot_crediti_sost += Integer.parseInt(e.getPeso_crediti());
				}
				if (e.getVoto().equals("APPR") || e.getVoto().equals("IDO")) {
					tot_esami_sost++;
					tot_crediti_sost += Integer.parseInt(e.getPeso_crediti());
					no_media++;
				}
			}
		}
		return tot_esami_sost - no_media;
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
		mInflater.inflate(R.menu.lib_menu, menu);
		return true;
	}

	private void setEditDialog(View layout) {
		Esame e = (Esame) adapter.getItem(selected_item);

		EditText et = (EditText) layout.findViewById(R.id.et_edit_name);
		et.setText(e.toString());

		Spinner sp_aaf = (Spinner) layout.findViewById(R.id.sp_edit_aaf);
		Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		String aaf = e.getAa_freq();
		int selected = 0;
		for (int i = 0; i < 10; i++) {
			String item = (year - 1) + "/" + year;
			if (item.equals(aaf)) {
				selected = i;
				System.out.println("pos: " + selected + " -- " + item);
			}
			adapter.add(item);
			year--;
		}
		sp_aaf.setAdapter(adapter);
		sp_aaf.setSelection(selected);

		Spinner sp_voto = (Spinner) layout.findViewById(R.id.sp_edit_voto);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		String voto = e.getVoto();
		selected = 0;
		for (int i = 18; i < 31; i++) {
			String item = String.valueOf(i);
			adapter.add(item);
			if (voto.equals(item))
				selected = i - 18;
		}
		sp_voto.setAdapter(adapter);
		sp_voto.setSelection(selected);

		Spinner sp_crediti = (Spinner) layout.findViewById(R.id.sp_edit_crediti);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		String crediti = e.getPeso_crediti();
		selected = 0;
		for (int i = 1; i < 31; i++) {
			String item = String.valueOf(i);
			adapter.add(item);
			if (crediti.equals(item)) {
				selected = i - 1;
				System.out.println("pos: " + selected + " -- " + item);
			}
		}
		sp_crediti.setAdapter(adapter);
		sp_crediti.setSelection(selected);

		DatePicker dp = (DatePicker) layout.findViewById(R.id.dp_edit_date);
		String dmy[] = e.getData_esame().split("/");
		int day = 0;
		int month = 0;
		if (dmy.length == 3) {
			try {
				day = Integer.parseInt(dmy[0]);
				month = Integer.parseInt(dmy[1]);
				year = Integer.parseInt(dmy[2]);
				dp.init(year, month, day, null);
			} catch (Exception ex) {
				// TODO: handle exception
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_MESSAGE:
			return builder;
		default:
			return super.onCreateDialog(id);
		}
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
		case MENU_EXPORT_XML:
			exportToXML();
			return true;
		case MENU_CLEAR_XML:
			boolean success = Utils.deleteFile(LIBRETTO_XML_FILE);
			if (success)
				showMessage(CLEAR_XML_MESSAGE, "Dati cancellati.");
			return success;
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

				intent.putExtra(pkg + ".aritm",
						String.valueOf(mediaAritmetica()));
				intent.putExtra(pkg + ".pond", String.valueOf(mediaPonderata()));
				intent.putExtra(pkg + ".num", String.valueOf(numeroEsami()));
				intent.putExtra(pkg + ".crediti",
						String.valueOf(tot_crediti_sost));

				startActivity(intent);
				break;
			case EXPORT_XML_MESSAGE:
			case CLEAR_XML_MESSAGE:
			case CONNECTION_ERROR:
			case LOGIN_ERROR:
				showDialog(DIALOG_MESSAGE);
				break;
			}
		}
	};

	public class LibrettoTask extends AsyncTask<String, Void, Void> {

		private ProgressDialog dialog;

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

		@Override
		protected void onPostExecute(Void success) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
			setListAdapter(adapter);
			registerForContextMenu(getListView());
		}

		@Override
		protected Void doInBackground(String... params) {
			if (params != null && params.length > 0) {
				if (!loadXML())
					retrieveData(params[0]);
			} else if (!Utils.isNetworkAvailable(Libretto.this)) {
				cm.setLogged(false);
				showMessage(CONNECTION_ERROR, "Connessione NON attiva!");
			} else
				retrieveData(cm.connection(ConnectionManager.ESSE3,
						Utils.TARGET_LIBRETTO));
			return null;
		}
	}

}
