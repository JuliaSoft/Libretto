package com.juliasoft.libretto.activity;

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
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.connection.SsolHttpClient;

public class Questionario extends Activity {

	private static final String TAG = Questionario.class.getName();
	private static final int SUCCESS = 0;
	private static final int LOAD = 1;
	private static final int LOAD_DLG = 2;

	private ConnectionManager cm;
	private String questionario_HTML;
	public boolean update = true;
	public boolean loadJS = false;
	public boolean javascriptInterfaceBroken = false;

	private final Activity progressBar = this;
	private WebView webView;
	private ViewSwitcher vs;
	private ProgressDialog dialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.questionario);
		init();
	}

	private void init() {
		cm = ConnectionManager.getInstance();

		// Determine if JavaScript interface is broken.
		// For now, until we have further clarification from the Android team,
		// use version number
		if ("2.3".equals(Build.VERSION.RELEASE.substring(0, 3))) {
			javascriptInterfaceBroken = true;
		}

		webView = (WebView) findViewById(R.id.sito);
		vs = ((ViewSwitcher) findViewById(R.id.view_switcher));
		vs.showNext(); // Visualizzo il browser

		dialog = new ProgressDialog(Questionario.this);
		dialog.setCancelable(true);
		dialog.setMessage("Loading ...");

		setUpWebView();
		loadSSOLCookies();
		Intent intent = getIntent();
		questionario_HTML = intent.getStringExtra(getPackageName() + ".page");
		loadQuestionario();
	}

	private void setUpWebView() {
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
		webSettings.setSupportMultipleWindows(false);
		webSettings.setSupportZoom(true);
		webSettings.setDefaultTextEncodingName("utf-8");

		webView.setWebViewClient(new MyWebViewClient());
		webView.requestFocus(View.FOCUS_DOWN);

		// Add javascript interface only if it's not broken
		if (!javascriptInterfaceBroken)
			webView.addJavascriptInterface(new QuestionarioJSInterface(),
					"droid");

		webView.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int progress) {
				progressBar.setTitle("Loading...");
				progressBar.setProgress(progress * 100);
			}

			@Override
			public boolean onJsConfirm(WebView view, final String url,
					String message, final JsResult result) {
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
								}).create().show();

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
	}

	private void loadSSOLCookies() {
		List<Cookie> cookies = cm.getSSOLCookies();
		if (!cookies.isEmpty()) {
			CookieSyncManager.createInstance(this);
			CookieManager cookieManager = CookieManager.getInstance();
			for (Cookie sessionInfo : cookies) {
				String cookieString = sessionInfo.getName() + "="
						+ sessionInfo.getValue() + "; domain="
						+ sessionInfo.getDomain();
				cookieManager.setCookie(SsolHttpClient.AUTH_URI, cookieString);
				CookieSyncManager.getInstance().sync();
			}
		}
	}

	private void loadQuestionario() {
		if (questionario_HTML != null)
			webView.loadDataWithBaseURL(SsolHttpClient.AUTH_URI,
					questionario_HTML, "text/html", "UTF-8", null);
	}

	private Handler questionarioHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LOAD:
				if (!dialog.isShowing())
					showDialog(LOAD_DLG);
				loadQuestionario();
				break;
			case SUCCESS:
				reset();
				setResult(Activity.RESULT_OK, null);
				finish();
				break;
			default:
				break;
			}
		}
	};

	private void reset() {
		webView.stopLoading();
		webView.clearCache(true);
		webView.clearHistory();
		webView.freeMemory();
		dialog.dismiss();
		dialog.cancel();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case LOAD_DLG:
			return dialog;
		default:
			return super.onCreateDialog(id);
		}
	}

	@Override
	public void onBackPressed() {
		reset();
		setResult(Activity.RESULT_CANCELED, null);
		super.onBackPressed();
	}

	class MyWebViewClient extends WebViewClient {

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			System.out.println("Connect to: " + url);
			if (!loadJS)
				vs.showPrevious(); // Nascondo il browser

			super.onPageStarted(view, url, favicon);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			if (update && !javascriptInterfaceBroken) {
				dialog.setMessage("load questionnaire ...");
				loadJS = true;
				webView.loadUrl("javascript:window.droid.questJS(document.getElementsByTagName('html')[0].innerHTML);");
			} else {
				dialog.setMessage("success ...");
				vs.showNext(); // Visualizzo il browser
				loadJS = false;
				update = true;
				dialog.dismiss();
			}
		}

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			dialog.dismiss();
			Toast.makeText(Questionario.this, "Oh no! " + description,
					Toast.LENGTH_SHORT).show();
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url_poi) {
			dialog.setMessage("should override url Loading ...");
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
				matcher.appendReplacement(sb,
						"<body><script type=\"text/javascript\">");

			matcher.appendTail(sb);
			questionario_HTML = sb.toString();
		}
	}
}
