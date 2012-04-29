package com.juliasoft.libretto.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.connection.SsolHttpClient;
import com.juliasoft.libretto.utils.Iscrizione;
import com.juliasoft.libretto.utils.Utils;

public class IscrizioneAppello extends ExpandableListActivity {

	private static final String TAG = IscrizioneAppello.class.getName();

	private static final int INIT = 0;
	private static final int SUCCESS = 1;
	private static final int FAIL = 2;
	private static final int QUEST = 3;

	private ConnectionManager cm;
	private ArrayList<String> groupData;
	private ArrayList<List<Object>> childData;
	private String url;
	private String page_HTML;
	private String page_URL;
	private boolean loadQuestionario;

	private TextView corso;
	private TextView esame;
	private ArrayAdapter<String> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.iscriz_appelli);
		init();
	}

	private void init() {
		Intent intent = getIntent();
		String pkg = getPackageName();
		page_HTML = intent.getStringExtra(pkg + ".iscriz");

		cm = ConnectionManager.getInstance();
		page_URL = intent.getStringExtra(pkg + ".url");

		groupData = new ArrayList<String>();
		childData = new ArrayList<List<Object>>();

		corso = (TextView) findViewById(R.id.tv_iscr_corso);
		esame = (TextView) findViewById(R.id.tv_iscr_esame);

		retrieveData();
	}

	private void retrieveData() {
		if (page_HTML == null) {
			return;
		}

		try {
			Element fieldset = Utils.jsoupSelect(page_HTML, "fieldset").first();

			Element legend = fieldset.select("legend").first();
			if (legend != null) {
				try {
					String s[] = legend.text().split(" - ");
					corso.setText(s[0]);
					esame.setText(s[1]);
				} catch (NullPointerException e) {
					Log.e(TAG, "Retrive data: split error");
				}
			}

			Elements groups = fieldset.select("p.provaDiesame");

			for (Element group : groups) {
				String text = "";
				group = group.nextElementSibling();
				Elements strongs = group.select("strong.ContentTitolo");
				for (Iterator<Element> i = strongs.iterator(); i.hasNext();) {
					text += i.next().text() + "-";
				}
				group.children().remove();
				text += group.text().replace(":", "");
				groupData.add(text);
			}

			Elements forms = fieldset.select("form");
			for (int i = 0; i < forms.size(); i++) {
				Iscrizione esame = new Iscrizione();
				List<Object> children = new ArrayList<Object>();
				childData.add(children);
				children.add(esame);

				Element form = forms.get(i);
				url = form.attr("action");
				Elements inputs = form.select("input");

				for (Element input : inputs)
					esame.addParam(input.attr("name"), input.attr("value"));

				Elements trs = form.select("table").select("tr");
				String action = esame.getParam("azione");

				if (action.equals("INSERT")) {
					esame.setRegister(false);
					esame.clearTypes();
					Elements options = form.select("select>option");
					for (Element option : options)
						esame.addType(option.text());

					Elements tds = trs.get(0).select("td.Content_Chiaro");
					esame.setData(tds.get(0).text());
					String iscrtt = tds.get(1).text();
					if (tds.get(1).select("input[type=submit]").isEmpty()) {
						esame.setEnable(false);
						iscrtt = iscrtt.replace("Iscrizioni chiuse", "");
					}
					esame.setIscritti(iscrtt);

					tds = trs.get(2).select("td.Content_Chiaro");
					esame.setLuogo(tds.text());

					tds = trs.get(3).select("td.Content_Chiaro");
					esame.setVerbalizzazione(tds.text());
				}

				// Se sono gi� iscritto all'esame
				if (action.equals("DELETE")) {
					esame.setRegister(true);
					Elements tds = trs.get(0).select("td.Content_Chiaro");
					esame.setNumero(tds.get(0).text());
					String iscrtt = tds.get(1).text();
					if (tds.get(1).select("input[type=submit]").isEmpty()) {
						esame.setEnable(false);
						iscrtt = iscrtt.replace("Iscrizioni chiuse", "");
					}
					esame.setIscritti(iscrtt);
					tds = trs.get(1).select("td.Content_Chiaro");
					esame.setData(tds.text());

					tds = trs.get(2).select("td.Content_Chiaro");
					esame.clearTypes();
					esame.addType(tds.text());

					tds = trs.get(3).select("td.Content_Chiaro");
					esame.setLuogo(tds.text());

					tds = trs.get(4).select("td.Content_Chiaro");
					esame.setVerbalizzazione(tds.text());
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Retrive data: " + e.getMessage());
			e.printStackTrace();
		}
		appelloHandler.sendEmptyMessage(INIT);
	}

	private void refreshPage() {
		groupData.clear();
		childData.clear();
		page_HTML = cm.connection(ConnectionManager.SSOL, page_URL, null);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1)
			switch (resultCode) {
			case Activity.RESULT_OK:
				appelloHandler.sendEmptyMessage(SUCCESS);
				break;
			case Activity.RESULT_CANCELED:
				Toast cancel = Toast.makeText(this, "Iscrizione NON avvenuta!", Toast.LENGTH_LONG);
				cancel.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
				cancel.show();
				break;
			default:
				showDialog(FAIL);
				break;
			}
	}

	private Handler appelloHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case INIT:
				MyExpandableListAdapter el = new MyExpandableListAdapter(
						IscrizioneAppello.this, groupData, childData);
				setListAdapter(el);
				break;
			case QUEST:
				loadQuestionario = true;
				Intent intent = new Intent(getApplicationContext(),
						Questionario.class);
				String pkg = getPackageName();
				// setto i dati ricavati dal login
				intent.putExtra(pkg + ".page", page_HTML);
				startActivityForResult(intent, 1);
				break;
			case SUCCESS:
				try {
					refreshPage();
					retrieveData();
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
				}
				break;
			case FAIL:
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

			if (obj instanceof Iscrizione) {
				LayoutInflater infalInflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.iscr_appello_item,
						null);

				final Iscrizione appello = (Iscrizione) obj;

				TextView tv = (TextView) convertView
						.findViewById(R.id.tv_iscr_data);
				tv.setText("" + appello.getData());

				tv = (TextView) convertView.findViewById(R.id.tv_iscr_luogo);
				tv.setText("" + appello.getLuogo());

				tv = (TextView) convertView.findViewById(R.id.tv_iscr_verb);
				tv.setText(Html.fromHtml(appello.getVerb()));

				tv = (TextView) convertView.findViewById(R.id.tv_iscr_iscritti);
				tv.setText("" + appello.getTotIscritti());

				tv = (TextView) convertView.findViewById(R.id.tv_iscr_num);
				tv.setText("" + appello.getNumero());

				LinearLayout ll = (LinearLayout) convertView
						.findViewById(R.id.ll_iscr_butt);
				tv = (TextView) convertView.findViewById(R.id.tv_iscr_closed);

				final Spinner spinner = (Spinner) convertView
						.findViewById(R.id.sp_iscr);
				adapter = new ArrayAdapter<String>(IscrizioneAppello.this,
						android.R.layout.simple_spinner_item,
						appello.getTypes());
				spinner.setAdapter(adapter);
				if (appello.isRegister())
					spinner.setEnabled(false);
				final ToggleButton b = (ToggleButton) convertView
						.findViewById(R.id.tb_iscr_ok);

				if (appello.isRegister())
					b.setChecked(true);
				else
					b.setChecked(false);

				if (appello.isEnable()) {
					tv.setVisibility(View.INVISIBLE);
					b.setOnClickListener(new OnClickListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onClick(View v) {
							b.setChecked(false);
							appello.addParam("tipo_iscrizione", spinner
									.getSelectedItem().toString());

							new IscrizioneAsyncTask().execute(appello
									.getParams());
						}
					});
				} else {
					ll.setVisibility(View.INVISIBLE);
					tv.setText("Iscrizioni chiuse");
				}
			}

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
				convertView = infalInflater.inflate(R.layout.app_iscrr_item,
						null);
			}

			String s[] = group.split("-");
			if (s.length > 0) {
				TextView tv = (TextView) convertView
						.findViewById(R.id.tv_app_typ);
				tv.setText(s[0]);
				if (s.length > 1) {
					tv = (TextView) convertView.findViewById(R.id.tv_app_data);
					tv.setText(s[1]);

					if (s.length > 2) {
						tv = (TextView) convertView
								.findViewById(R.id.tv_app_ora);
						tv.setText(s[2]);

						if (s.length > 3) {
							tv = (TextView) convertView
									.findViewById(R.id.tv_app_note);
							tv.setText(s[3]);
						}
					}
				}
			}
			return convertView;
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

	public class IscrizioneAsyncTask extends
			AsyncTask<HashMap<String, String>, Boolean, Boolean> {

		private ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(IscrizioneAppello.this,
					"Please wait...", "Loading data ...", true);
		}

		@Override
		protected void onPostExecute(Boolean isSuccess) {
			if (isSuccess)
				appelloHandler.sendEmptyMessage(SUCCESS);
			else if (!loadQuestionario)
				appelloHandler.sendEmptyMessage(FAIL);
			progressDialog.dismiss();
		}

		@Override
		protected Boolean doInBackground(HashMap<String, String>... params) {
			if (params != null && params.length > 0)
				return iscrizione(SsolHttpClient.AUTH_URI + url, params[0]);

			return false;
		}

		private boolean iscrizione(String link, HashMap<String, String> params) {
			if (Utils.isLink(link)) {
				loadQuestionario = false;
				page_HTML = cm.connection(ConnectionManager.SSOL, link, params);
				if (page_HTML.contains("Questionario"))
					appelloHandler.sendEmptyMessage(QUEST);
				else {
					refreshPage();
					return true;
				}
			}

			return false;
		}

	}
}
