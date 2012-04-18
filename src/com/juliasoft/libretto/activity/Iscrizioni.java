package com.juliasoft.libretto.activity;

import java.util.ArrayList;
import java.util.List;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.connection.SsolHttpClient;
import com.juliasoft.libretto.utils.Appello;
import com.juliasoft.libretto.utils.Utils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.R.color;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.TextView;

public class Iscrizioni extends ExpandableListActivity {

	@SuppressWarnings("unused")
	private static final String TAG = Iscrizioni.class.getName();
	private static final int INIT = 0;
	private static final int METHOD_DRAW_SELECTOR_ON_TOP = 1;

	private ConnectionManager cm;
	private ArrayList<String> groupData;
	private ArrayList<List<Object>> childData;
	private String page_data;
	private String url;
	private int mMethod;
	private String type;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.iscrizioni_new);
		init();
	}

	private void changeMethod(int method) {
		if (mMethod != method) {
			switch (method) {
			case METHOD_DRAW_SELECTOR_ON_TOP:
				mMethod = METHOD_DRAW_SELECTOR_ON_TOP;
				getExpandableListView().setSelector(
						R.drawable.list_selector_on_top);
				getExpandableListView().setDrawSelectorOnTop(true);
				break;
			default:
				// Do nothing : this value is not handled
				break;
			}
			getExpandableListView().invalidateViews();
		}
	}

	private void init() {
		Intent intent = getIntent();
		String pkg = getPackageName();
		String page_HTML = intent.getStringExtra(pkg + ".iscriz");
		type = "" + intent.getStringExtra(pkg + ".type");

		cm = ConnectionManager.getInstance();
		groupData = new ArrayList<String>();
		childData = new ArrayList<List<Object>>();

		changeMethod(METHOD_DRAW_SELECTOR_ON_TOP);

		new MyAsyncTask(false).execute(page_HTML);
	}

	private void retriveData(String page_HTML) {
		if (page_HTML == null) {
			return;
		}

		if (type.equals("OLD")) {
			String esame = Utils
					.jsoupSelect(page_HTML,
							"select.TopTabText[name=id_insegn]>option[selected=selected]")
					.text();
			groupData.add(esame.toUpperCase());
			List<Object> children = new ArrayList<Object>();
			childData.add(children);

			page_HTML = Utils.removeSpecialString(page_HTML, "&nbsp;");

			// mi ricavo tutti gli appelli dell'esame
			Elements fss = Utils.jsoupSelect(page_HTML, "fieldset");

			// ciclo degli appelli per l'esame
			for (Element fs : fss) {
				Appello app = new Appello();
				children.add(app);
				String docenti = "-";
				String test = fs.select("p").text();
				String split[] = test
						.replace("Verbalizzante", ":Verbalizzante").split(":");
				if (split.length > 1) {
					docenti = "<b>" + split[0] + ": </b>" + split[1];
					if (split.length > 3)
						docenti += "<br><b>" + split[2] + ": </b>" + split[3];
				}

				// informazioni dell'appello
				app.setDocenti(docenti);

				Elements tds = fs.select("td.Content_Chiaro");
				Elements input = tds.get(4).children();
				tds.remove(4);
				String tipo = "", modulo = "", data = "", ora = "";

				for (int i = 0; i < tds.size(); i += 4) {

					if (tds.get(i).text().equals("Solo verbalizzazione")) {
						tipo += "Verb." + "\n";
					} else {
						tipo += tds.get(i).text() + "\n";
					}
					modulo += tds.get(i + 1).text() + "\n";
					data += tds.get(i + 2).text() + "\n";
					ora += tds.get(i + 3).text() + "\n";
				}

				app.setTipo(tipo);
				app.setModulo(modulo);
				app.setData(data);
				app.setOra(ora);

				String link = "";
				if (input.hasAttr("onclick")) {
					link = SsolHttpClient.AUTH_URI
							+ input.attr("onclick").split("'")[1];
				}

				// link per effettuare l'iscrizione
				if (Utils.isLink(link)) {
					app.setLink(link);
				} else {
					app.setLink(input.text());
				}
			}
			return;
		}

		Elements trs = Utils.jsoupSelect(page_HTML, "table.Border>tbody>tr");

		// ciclo degli esami da sostenere
		for (Element tr : trs) {
			Elements tds = tr.children();

			groupData.add(tds.get(1).text());
			List<Object> children = new ArrayList<Object>();
			childData.add(children);

			Elements input = tds.get(4).children();
			if (input == null) {
				children.add(null);
				continue;
			}

			String link = "";
			if (input.hasAttr("onclick")) {
				link = SsolHttpClient.AUTH_URI
						+ input.attr("onclick").split("'")[1];
			}

			// se l'iscrizione all'esame corrente è aperta proseguo
			if (Utils.isLink(link)) {
				try {
					page_HTML = cm.connection(ConnectionManager.SSOL, link);
				} catch (Exception e) {
					children.add(input.text());
					continue;
				}

				page_HTML = Utils.removeSpecialString(page_HTML, "&nbsp;");

				// mi ricavo tutti gli appelli dell'esame
				Elements fss = Utils.jsoupSelect(page_HTML, "fieldset");

				// ciclo degli appelli per l'esame
				for (Element fs : fss) {
					Appello app = new Appello();
					children.add(app);
					String docenti = "-";
					String test = fs.select("p").text();
					String split[] = test.replace("Verbalizzante",
							":Verbalizzante").split(":");
					if (split.length > 1) {
						docenti = "<b>" + split[0] + ": </b>" + split[1];
						if (split.length > 3)
							docenti += "<br><b>" + split[2] + ": </b>"
									+ split[3];
					}

					// informazioni dell'appello
					app.setDocenti(docenti);

					tds = fs.select("td.Content_Chiaro");
					input = tds.get(4).children();
					tds.remove(4);
					String tipo = "", modulo = "", data = "", ora = "";

					for (int i = 0; i < tds.size(); i += 4) {

						if (tds.get(i).text().equals("Solo verbalizzazione")) {
							tipo += "Verb." + "\n";
						} else {
							tipo += tds.get(i).text() + "\n";
						}
						modulo += tds.get(i + 1).text() + "\n";
						data += tds.get(i + 2).text() + "\n";
						ora += tds.get(i + 3).text() + "\n";
					}

					app.setTipo(tipo);
					app.setModulo(modulo);
					app.setData(data);
					app.setOra(ora);

					link = "";
					if (input.hasAttr("onclick")) {
						link = SsolHttpClient.AUTH_URI
								+ input.attr("onclick").split("'")[1];
					}

					// link per effettuare l'iscrizione
					if (Utils.isLink(link)) {
						app.setLink(link);
					} else {
						app.setLink(input.text());
					}
				}
			}
		}
	}

	// Handler serve per aggiornare la grafica
	private Handler iscrizioniHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case INIT:
				MyExpandableListAdapter e = new MyExpandableListAdapter(
						Iscrizioni.this, groupData, childData);
				setListAdapter(e);
				break;
			default:
				break;
			}
		}
	};

	public class MyExpandableListAdapter extends BaseExpandableListAdapter {

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		private Context context;

		List<String> groupData = new ArrayList<String>();
		List<List<Object>> childData = new ArrayList<List<Object>>();

		public MyExpandableListAdapter(Context context,
				ArrayList<String> groups, ArrayList<List<Object>> children) {
			this.context = context;
			this.groupData = groups;
			this.childData = children;
		}

		/**
		 * A general add method, that allows you to add a appello to this list
		 * 
		 * Depending on if the category opf the appello is present or not, the
		 * corresponding item will either be added to an existing group if it
		 * exists, else the group will be created and then the item will be
		 * added
		 * 
		 * @param appello
		 */
		public void addItem(Appello appello) {
			if (!groupData.contains(appello.getEsame())) {
				groupData.add(appello.getEsame());
			}
			int index = groupData.indexOf(appello.getEsame());
			if (childData.size() < index + 1) {
				childData.add(new ArrayList<Object>());
			}
			childData.get(index).add(appello);
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return childData.get(groupPosition).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		// Return a child view. You can load your custom layout here.

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {

			Object obj = getChild(groupPosition, childPosition);

			if (obj instanceof String) {
				LayoutInflater infalInflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.iscriz_item, null);

				TextView tv = (TextView) convertView.findViewById(R.id.tv_iscr);
				tv.setText((CharSequence) obj);

				return convertView;
			}

			if (obj instanceof Appello) {
				LayoutInflater infalInflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(
						R.layout.dettagli_appelli_item, null);

				Appello appello = (Appello) obj;

				TextView tv = (TextView) convertView.findViewById(R.id.tipo_id);
				tv.setText("" + appello.getTipo());

				tv = (TextView) convertView.findViewById(R.id.modulo_id);
				tv.setText("" + appello.getModulo());
				tv = (TextView) convertView.findViewById(R.id.data_id);
				tv.setText("" + appello.getData());
				tv = (TextView) convertView.findViewById(R.id.ora_id);
				tv.setText("" + appello.getOra());
				tv = (TextView) convertView.findViewById(R.id.doc_id);
				tv.setText(Html.fromHtml(appello.getDocenti()));
				Button b = (Button) convertView.findViewById(R.id.prosegui_id);
				final String link = appello.getLink();

				if (Utils.isLink(link)) {

					b.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							new MyAsyncTask(true).execute(link);
						}
					});
				} else {
					b.setVisibility(View.INVISIBLE);
					tv = (TextView) convertView.findViewById(R.id.tv_idInfoApp);
					tv.setText(link);
					tv.setTextColor(color.black);
				}
			}
			convertView
					.setBackgroundResource(R.drawable.list_item_background_special);

			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return childData.get(groupPosition).size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return groupData.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return groupData.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		// Return a group view. You can load your custom layout here.
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			String group = (String) getGroup(groupPosition);
			if (convertView == null) {
				LayoutInflater infalInflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater
						.inflate(R.layout.appelli_item, null);
			}

			TextView tv = (TextView) convertView.findViewById(R.id.tvGroup);
			tv.setPadding(45, 0, 0, 0);
			tv.setTextColor(Color.BLACK);
			tv.setText(group);
			return convertView;
		}

		public TextView getGenericView() {
			// Layout parameters for the ExpandableListView
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, 100);

			TextView textView = new TextView(Iscrizioni.this);

			textView.setLayoutParams(lp);
			// Center the text vertically
			textView.setTextColor(Color.BLACK);
			textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
			textView.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
			textView.setMaxLines(2);
			textView.setMinHeight(45);
			textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			// Set the text starting position
			textView.setPadding(55, 0, 0, 0);

			return textView;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int arg0, int arg1) {
			return true;
		}
	}

	public class MyAsyncTask extends AsyncTask<String, Void, Void> {

		private ProgressDialog progressDialog;
		private boolean connect;

		public MyAsyncTask(boolean connect) {
			this.connect = connect;
		}

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(Iscrizioni.this,
					"Please wait...", "Loading data ...", true);
		}

		@Override
		protected void onPostExecute(Void result) {
			if (connect) {
				Intent intent = new Intent(getApplicationContext(),
						IscrizioneAppello.class);
				String pkg = getPackageName();
				// setto i dati ricavati dal login
				intent.putExtra(pkg + ".iscriz", page_data);
				intent.putExtra(pkg + ".url", url);
				startActivity(intent);
			} else {
				iscrizioniHandler.sendEmptyMessage(INIT);
			}
			progressDialog.dismiss();
		}

		@Override
		protected Void doInBackground(String... params) {
			if (params != null && params.length > 0) {
				if (connect) {
					url = params[0];
					page_data = cm.connection(ConnectionManager.SSOL, url);
				} else {
					retriveData(params[0]);
				}
			}
			return null;
		}
	}
}
