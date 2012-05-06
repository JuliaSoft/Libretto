package com.juliasoft.libretto.activity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.cookie.Cookie;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.connection.SsolHttpClient;
import com.juliasoft.libretto.utils.Utils;

public class Questionario extends Activity {

	private static final boolean DEBUG = true;
	private static final String TAG = Questionario.class.getName();
	
	private static final int SUCCESS = 0;
	private static final int LOAD = 1;
	private static final int ERROR_MESSAGE = 2;
	
	private static final int PROGRESS_DIALOG_ID = 3;
	private static final int ERROR_DIALOG_ID = 4;

	private String questionario_HTML;
	private boolean update = true;
	private boolean loadJS = false;
	private boolean javascriptInterfaceBroken = false;

	private WebView webView;
	private FrameLayout webContainer;
	private ViewSwitcher viewSwitcher;

	private AlertDialog allertDialog;
	private ProgressDialog progressDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (DEBUG)
			Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.questionario);
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

	@Override
	protected void onPause() {
		if (DEBUG)
			Log.d(TAG, "onPause()");
		super.onPause();
		this.callHiddenWebViewMethod("onPause");
		webView.pauseTimers();
		CookieSyncManager.getInstance().stopSync();
	}

	@Override
	protected void onResume() {
		if (DEBUG)
			Log.d(TAG, "onResume()");
		super.onResume();
		this.callHiddenWebViewMethod("onResume");
		webView.resumeTimers();
		CookieSyncManager.getInstance().startSync();
	}

	@Override
	protected void onDestroy() {
		if (DEBUG)
			Log.d(TAG, "onDestroy()");
		super.onDestroy();
		webContainer.removeAllViews();
		webView.freeMemory();
		webView.destroy();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		setResult(Activity.RESULT_CANCELED, null);
		finish();
	}

	private Handler questionarioHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LOAD:
				if (!progressDialog.isShowing())
					showDialog(PROGRESS_DIALOG_ID);
				loadQuestionario();
				break;
			case SUCCESS:
				setResult(Activity.RESULT_OK, null);
				finish();
				break;
			default:
				break;
			}
		}
	};

	private void init() {
		// Determine if JavaScript interface is broken.
		// For now, until we have further clarification from the Android team,
		// use version number
		if ("2.3".equals(Build.VERSION.RELEASE.substring(0, 3))) {
			javascriptInterfaceBroken = true;
		}

		Intent intent = getIntent();
		questionario_HTML = intent.getStringExtra(getPackageName() + ".page");

		viewSwitcher = ((ViewSwitcher) findViewById(R.id.view_switcher));
		viewSwitcher.showNext();

		initDialog();
		initWebView();
		loadSSOLCookies();
		loadQuestionario();
	}

	private void initDialog() {
		progressDialog = new ProgressDialog(Questionario.this);
		progressDialog.setCancelable(true);
		progressDialog.setMessage("Loading ...");

		allertDialog = new AlertDialog
				.Builder(Questionario.this)
				.setTitle("Questionario")
				.setIcon(android.R.drawable.ic_dialog_alert)
				.create();
		allertDialog.setButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {				
				dialog.dismiss();
				finish();
			}
		});

		allertDialog.getWindow().getAttributes().dimAmount = 0.5f;
		allertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
	}

	private void initWebView() {
		webContainer = (FrameLayout) findViewById(R.id.web_container);
		webView = new WebView(Questionario.this);
		webView.setWebViewClient(new MyWebViewClient());
		webView.requestFocus(View.FOCUS_DOWN);

		// Add javascript interface only if it's not broken
		if (!javascriptInterfaceBroken)
			webView.addJavascriptInterface(new QuestionarioJSInterface(), "droid");

		webView.setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onJsConfirm(WebView view,
									   final String url,
									   String message, 
									   final JsResult result) {
				
				new AlertDialog.Builder(Questionario.this)
						.setTitle("Questionario")
						.setMessage(message)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										result.confirm();
									}
								})
						.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										result.cancel();
									}
								})
						.create()
						.show();

				return true;
			}
		});

		webView.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_UP:
					if (!v.hasFocus())
						v.requestFocus();
					break;
				}
				return false;
			}
		});

		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSupportZoom(true);
		webSettings.setDefaultTextEncodingName("utf-8");
		webContainer.addView(webView);
	}

	private void loadSSOLCookies() {
		List<Cookie> cookies = ConnectionManager.getInstance().getSSOLCookies();
		if (!cookies.isEmpty()) {
			CookieSyncManager.createInstance(this);
			CookieManager cookieManager = CookieManager.getInstance();
			for (Cookie sessionInfo : cookies) {
				String cookieString = sessionInfo.getName() 
						+ "=" + sessionInfo.getValue() 
						+ "; domain=" + sessionInfo.getDomain();
				cookieManager.setCookie(SsolHttpClient.AUTH_URI, cookieString);
				CookieSyncManager.getInstance().sync();
			}
		}
	}

	private void loadQuestionario() {
		if (questionario_HTML != null)
			webView.loadDataWithBaseURL(SsolHttpClient.AUTH_URI, questionario_HTML, "text/html", "UTF-8", null);
	}

	private void callHiddenWebViewMethod(String name) {
		if (webView != null) {
			try {
				Method method = WebView.class.getMethod(name);
				method.invoke(webView);
			} catch (NoSuchMethodException e) {
				if (DEBUG)
					Log.e(TAG, "No such method " + name + ": " + e.getMessage());
			} catch (IllegalAccessException e) {
				if (DEBUG)
					Log.e(TAG, "Illegal Access " + name + ": " + e.getMessage());
			} catch (InvocationTargetException e) {
				if (DEBUG)
					Log.e(TAG, "Invocation Target Exception " + name + ": " + e.getMessage());
			}
		}
	}

	private void showErrorMessage(String msg) {
		allertDialog.setMessage(msg);
		questionarioHandler.sendEmptyMessage(ERROR_MESSAGE);
	}

	class MyWebViewClient extends WebViewClient {

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			if (Utils.isNetworkAvailable(Questionario.this)) {
				if (!loadJS)
					viewSwitcher.showPrevious(); 

				super.onPageStarted(view, url, favicon);
			} else {
				ConnectionManager.getInstance().setLogged(false);
				showErrorMessage("Connessione NON attiva!");
				setResult(Activity.RESULT_CANCELED, null);
				finish();
			}
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			if (update && !javascriptInterfaceBroken) {
				progressDialog.setMessage("load questionnaire ...");
				loadJS = true;
				webView. loadUrl("javascript:window.droid.questJS(document.getElementsByTagName('html')[0].innerHTML);");
			} else {
				progressDialog.setMessage("success ...");
				viewSwitcher.showNext();
				loadJS = false;
				update = true;
				progressDialog.dismiss();
			}
		}

		@Override
		public void onReceivedError(WebView view, 
									int errorCode,
									String description,
									String failingUrl) {
			
			progressDialog.dismiss();
			Toast.makeText(Questionario.this, "Oh no! " + description, Toast.LENGTH_SHORT).show();
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url_poi) {
			progressDialog.setMessage("Should override url Loading ...");
			view.loadUrl(url_poi);
			return true;
		}
	}

	class QuestionarioJSInterface {

		public void questJS(String data) {
			questionario_HTML = "<html>" + data + "</html>";
			if (isRegistered(data))
				questionarioHandler.sendEmptyMessage(SUCCESS);
			if (update)
				if (isCleanable(data)) {
					update = false;
					clean(data);
					questionarioHandler.sendEmptyMessage(LOAD);
				}
		}

		private boolean isRegistered(String data) {
			return data.contains("Esito:iscritto all'appello");
		}

		private boolean isCleanable(String data) {
			return data.contains("<script type=\"text/javascript\">");
		}

		private void clean(String data) {
			StringBuffer sb = new StringBuffer();
			Pattern pattern = Pattern.compile(
					"^<body>(.*?)<script type=\"text/javascript\">$",
					Pattern.MULTILINE | Pattern.DOTALL);
			Matcher matcher = pattern.matcher(questionario_HTML);

			if (matcher.find())
				matcher.appendReplacement(sb, "<body><script type=\"text/javascript\">");

			matcher.appendTail(sb);
			questionario_HTML = sb.toString();
		}
	}
}
