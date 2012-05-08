package com.juliasoft.libretto.activity;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.R.color;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.connection.SsolHttpClient;
import com.juliasoft.libretto.utils.Appello;
import com.juliasoft.libretto.utils.Utils;

public class Iscrizioni extends ExpandableListActivity {

	private static final boolean DEBUG = true;
	private static final String TAG = Iscrizioni.class.getName();

	private static final int INIT = 0;
	private static final int SUCCESS = 1;
	private static final int ERROR_MESSAGE = 2;
	private static final int ERROR_DIALOG_ID = 3;
	private static final int PROGRESS_DIALOG_ID = 4;

	private IscrizioniTask iscrizioniTask;
	private ArrayList<String> groupData;
	private ArrayList<List<Object>> childData;
	private String page_URL;
	private String page_HTML;
	private String type;
	private AlertDialog allertDialog;
	private ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.iscrizioni_new);
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
				Intent intent = new Intent(getApplicationContext(), IscrizioneAppello.class);
				intent.putExtra(getPackageName() + ".page_HTML", page_HTML);
				intent.putExtra(getPackageName() + ".page_URL", page_URL);
				startActivity(intent);
				break;
			case INIT:
				IscrizioniAdapter e = new IscrizioniAdapter(Iscrizioni.this, groupData, childData);
				setListAdapter(e);
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
		Intent intent = getIntent();
		type = intent.getStringExtra(getPackageName() + ".parent") != null ? "OLD" : "NEW";
		page_HTML = intent.getStringExtra(getPackageName() + ".page_HTML");

		groupData = new ArrayList<String>();
		childData = new ArrayList<List<Object>>();

		getExpandableListView().setSelector(R.drawable.list_selector_on_top);
		getExpandableListView().setDrawSelectorOnTop(true);
		getExpandableListView().invalidateViews();

		initDialog();
		doUpdate(false);
	}

	private void initDialog() {
		progressDialog = new ProgressDialog(Iscrizioni.this);
		progressDialog.setTitle("Please wait...");
		progressDialog.setMessage("Loading data ...");
		progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					
			@Override
			public void onCancel(DialogInterface dialog) {
				iscrizioniTask.cancel(true);
			}
		});

		allertDialog = new AlertDialog
				.Builder(Iscrizioni.this)
				.setTitle("Iscrizioni")
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

	private void doUpdate(boolean connect) {
		showDialog(PROGRESS_DIALOG_ID);
		iscrizioniTask = (IscrizioniTask) new IscrizioniTask(connect).execute();
	}

	private void showErrorMessage(String msg) {
		allertDialog.setMessage(msg);
		iscrizioniHandler.sendEmptyMessage(ERROR_MESSAGE);
	}

	private void onTaskCompleted(boolean connect) {
		if (connect)
			iscrizioniHandler.sendEmptyMessage(SUCCESS);
		else
			iscrizioniHandler.sendEmptyMessage(INIT);

		removeDialog(PROGRESS_DIALOG_ID);
		iscrizioniTask = null;
		if (DEBUG)
			Log.i(TAG, "Task complete.");
	}

	public class IscrizioniAdapter extends BaseExpandableListAdapter {

		private Context context;
		private List<String> groupData;
		private List<List<Object>> childData;

		public IscrizioniAdapter(Context context,
								 ArrayList<String> groups,
								 ArrayList<List<Object>> children) {
			
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

		@Override
		public View getChildView(int groupPosition, 
								 int childPosition,
								 boolean isLastChild, 
								 View convertView, 
								 ViewGroup parent) {

			Object obj = getChild(groupPosition, childPosition);

			LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			if (obj instanceof String) {
				convertView = infalInflater.inflate(R.layout.iscriz_item, null);

				TextView tv = (TextView) convertView.findViewById(R.id.iscrizioni_closed_item);
				tv.setText((CharSequence) obj);

			} else if (obj instanceof Appello) {
				convertView = infalInflater.inflate(R.layout.dettagli_appelli_item, null);

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
				String link = appello.getLink();

				if (Utils.isLink(link)) {
					page_URL = link;
					b.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							doUpdate(true);
						}
					});
				} else {
					b.setVisibility(View.INVISIBLE);
					tv = (TextView) convertView.findViewById(R.id.tv_idInfoApp);
					tv.setText(link);
					tv.setTextColor(color.black);
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

		@Override
		public View getGroupView(int groupPosition,
								 boolean isExpanded,
								 View convertView, 
								 ViewGroup parent) {
			
			String group = (String) getGroup(groupPosition);
			if (convertView == null) {
				LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.appelli_item, null);
			}

			TextView tv = (TextView) convertView.findViewById(R.id.tvGroup);
			tv.setPadding(45, 0, 0, 0);
			tv.setTextColor(Color.BLACK);
			tv.setText(group);
			
			return convertView;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
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

	public class IscrizioniTask extends AsyncTask<Void, Void, Void> {

		private ConnectionManager cm;
		private final boolean connect;

		public IscrizioniTask(boolean connect) {
			cm = ConnectionManager.getInstance();
			this.connect = connect;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			onTaskCompleted(connect);
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (Utils.isNetworkAvailable(Iscrizioni.this)) {
				if (connect && !isCancelled())
					page_HTML = cm.connection(ConnectionManager.SSOL, page_URL, null);
				else if (!isCancelled())
					retrieveData();
			} else {
				cm.setLogged(false);
				showErrorMessage("Connessione NON attiva!");
			}
			return (null);
		}

		private void retrieveData() {
			try {
				if ("OLD".equals(type)) {
					if (page_HTML == null)
						return;

					String esame = Utils.jsoupSelect(page_HTML, "select.TopTabText[name=id_insegn]>option[selected=selected]").text();
					groupData.add(esame.toUpperCase());
					List<Object> children = new ArrayList<Object>();
					childData.add(children);
					retrieveAppelli(children);
				} else {
					page_HTML = cm.connection(ConnectionManager.SSOL, Utils.TARGET_ISCRIZIONI, null);

					if (page_HTML == null)
						return;

					Elements trs = Utils.jsoupSelect(page_HTML, "table.Border>tbody>tr");

					for (Element tr : trs) {
						Elements tds = tr.children();

						groupData.add(tds.get(1).text());
						List<Object> children = new ArrayList<Object>();
						childData.add(children);

						Elements td = tds.get(4).children();
						if (td.hasAttr("onclick")) {
							String link = SsolHttpClient.AUTH_URI + td.attr("onclick").split("'")[1];
							if (Utils.isLink(link)) {
								page_HTML = cm.connection(ConnectionManager.SSOL, link, null);

								if (page_HTML != null)
									retrieveAppelli(children);
							}
						} else if (!td.isEmpty())
							children.add(td.text());
					}
				}
			} catch (Exception e) {
				Utils.appendToLogFile("Iscrizioni retrieveData()", e.getMessage());
				showErrorMessage("Si è verificato un errore durante il parsing.");
				if (DEBUG)
					Log.e(TAG, e.getMessage());
			}
			page_HTML = "";
		}

		private void retrieveAppelli(List<Object> children) {
			page_HTML = Utils.removeSpecialString(page_HTML, "&nbsp;");

			for (Element fs : Utils.jsoupSelect(page_HTML, "fieldset"))
				children.add(new Appello(fs));

		}
	}
}
