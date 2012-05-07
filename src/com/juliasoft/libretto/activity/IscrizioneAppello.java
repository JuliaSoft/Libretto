package com.juliasoft.libretto.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.utils.Iscrizione;
import com.juliasoft.libretto.utils.Utils;

public class IscrizioneAppello extends ExpandableListActivity {

	private static final boolean DEBUG = true;
	private static final String TAG = IscrizioneAppello.class.getName();

	private static final int INIT = 0;
	private static final int QUEST = 1;
	private static final int ERROR_MESSAGE = 2;

	private static final int ERROR_DIALOG_ID = 3;
	private static final int PROGRESS_DIALOG_ID = 4;

	private ArrayList<String> groupData;
	private ArrayList<List<Object>> childData;
	private String iscrizione_URL;
	private String page_HTML;
	private String page_URL;
	private boolean loadQuestionario;

	private TextView corso;
	private TextView esame;
	private ArrayAdapter<String> adapter;
	private AlertDialog allertDialog;
	private ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.iscrizioni_appello);
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

	private void init() {
		Intent intent = getIntent();
		String pkg = getPackageName();
		page_HTML = intent.getStringExtra(pkg + ".page_HTML");
		page_URL = intent.getStringExtra(pkg + ".page_URL");
		
		groupData = new ArrayList<String>();
		childData = new ArrayList<List<Object>>();

		corso = (TextView) findViewById(R.id.tv_iscr_corso);
		esame = (TextView) findViewById(R.id.tv_iscr_esame);

		initDialog();
		retrieveData();
	}

	private void initDialog() {
		progressDialog = new ProgressDialog(IscrizioneAppello.this);
		progressDialog.setTitle("Please wait...");
		progressDialog.setMessage("Loading data ...");
		progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						dialog.dismiss();
					}
				});

		allertDialog = new AlertDialog
				.Builder(IscrizioneAppello.this)
				.setTitle("Libretto")
				.setIcon(android.R.drawable.ic_dialog_alert)
				.create();
		allertDialog.setButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}

		});

		WindowManager.LayoutParams lp = allertDialog.getWindow().getAttributes();
		lp.dimAmount = 0.5f;

		allertDialog.getWindow().setAttributes(lp);
		allertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
	}

	private void retrieveData() {
		try {
			if (page_HTML == null) {
				return;
			}

			Element fieldset = Utils.jsoupSelect(page_HTML, "fieldset").first();
			if (fieldset != null) {
				Element legend = fieldset.select("legend").first();
				if (legend != null) {
					try {
						String s[] = legend.text().split(" - ");
						corso.setText(s[0]);
						esame.setText(s[1]);
					} catch (Exception e) {
						if (DEBUG)
							Log.e(TAG, "Error retriveData(): " + e.getMessage());
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
					Element form = forms.get(i);
					List<Object> children = new ArrayList<Object>();
					childData.add(children);
					children.add(new Iscrizione(form));
				}
			}
		} catch (Exception e) {
			if (DEBUG)
				Log.e(TAG, "Retrive data: " + e.getMessage());
		}
		appelloHandler.sendEmptyMessage(INIT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		refresh();
	}

	private void doUpdate(HashMap<String, String> params) {
		showDialog(PROGRESS_DIALOG_ID);
		new AppelloTask().execute(params);
	}

	private void onTaskCompleted() {
		if(loadQuestionario)
			appelloHandler.sendEmptyMessage(QUEST);
		else
			refresh();
		removeDialog(PROGRESS_DIALOG_ID);
		if (DEBUG)
			Log.i(TAG, "Task complete.");
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
				Intent intent = new Intent(getApplicationContext(),
						Questionario.class);
				String pkg = getPackageName();
				intent.putExtra(pkg + ".page", page_HTML);
				startActivityForResult(intent, 1);
				break;
			case ERROR_MESSAGE:
				showDialog(ERROR_DIALOG_ID);
				break;
			default:
				break;
			}
		}
	};

	private void showErrorMessage(String msg) {
		allertDialog.setMessage(msg);
		appelloHandler.sendEmptyMessage(ERROR_MESSAGE);
	}

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

						@Override
						public void onClick(View v) {
							b.setChecked(false);
							appello.addParams("tipo_iscrizione", spinner
									.getSelectedItem().toString());
							iscrizione_URL = appello.getUrl();
							doUpdate(appello.getParams());
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

	private void refresh(){
		groupData.clear();
		childData.clear();
		loadQuestionario = false;
		page_HTML = ConnectionManager.getInstance().connection(ConnectionManager.SSOL, page_URL, null);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				retrieveData();
			}
		});
	}
	
	private class AppelloTask extends
			AsyncTask<HashMap<String, String>, Void, Void> {

		private ConnectionManager cm;

		public AppelloTask() {
			cm = ConnectionManager.getInstance();
		}

		@Override
		protected void onPostExecute(Void success) {
			super.onPostExecute(success);
			onTaskCompleted();
		}

		@Override
		protected Void doInBackground(HashMap<String, String>... params) {
			if (Utils.isNetworkAvailable(IscrizioneAppello.this)) {
				if (params != null && params.length > 0)
					iscrizione(params[0]);
			} else {
				cm.setLogged(false);
				showErrorMessage("Connessione NON attiva!");
			}
			return (null);
		}

		private void iscrizione(HashMap<String, String> params) {
			if (Utils.isLink(iscrizione_URL) && !isCancelled()) {
				loadQuestionario = false;
				page_HTML = cm.connection(ConnectionManager.SSOL, iscrizione_URL, params);
				if (page_HTML != null && page_HTML.contains("Questionario")) 
					loadQuestionario = true;
			}
		}
	}
}
