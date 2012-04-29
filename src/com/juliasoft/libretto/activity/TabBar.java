package com.juliasoft.libretto.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class TabBar extends TabActivity {

	public static final String TAG = TabActivity.class.getName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_bar);
		init();
	}

	private void init() {
		setResult(Activity.RESULT_OK);
		initInfoButton();

		Resources res = getResources();
		String pkg = getPackageName();
		Intent intent = getIntent();

		String lib = intent.getStringExtra(pkg + ".lib");
		String info = intent.getStringExtra(pkg + ".info");

		Intent libretto = new Intent().setClass(this, Libretto.class);
		libretto.putExtra(pkg + ".lib", lib);

		TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);

		// INFORMAZIONI
		Intent informazioni = new Intent().setClass(this, Info.class);
		informazioni.putExtra(pkg + ".info", info);

		TabSpec infoTab = tabHost
				.newTabSpec("Info")
				.setIndicator(null, res.getDrawable(R.drawable.informazioni))
				.setContent(informazioni);
		tabHost.addTab(infoTab);

		// LIBRETTO
		TabSpec librettoTab = tabHost
				.newTabSpec("Libretto")
				.setIndicator(null, res.getDrawable(R.drawable.libretto))
				.setContent(libretto);
		tabHost.addTab(librettoTab);

		// ISCRIZIONI
		Intent iscrizioni = new Intent();

		String iscriz = intent.getStringExtra(pkg + ".iscriz");
		if (iscriz == null) {
			// OLD
			iscriz = intent.getStringExtra(pkg + ".iscriz_old");
			iscrizioni.setClass(this, IscrizioniOld.class);
			iscrizioni.putExtra(pkg + ".type", "OLD");
		} else {
			// NEW
			iscrizioni.putExtra(pkg + ".type", "NEW");
			iscrizioni.setClass(this, Iscrizioni.class);
		}

		iscrizioni.putExtra(pkg + ".iscriz", iscriz);

		TabSpec iscrizTab = tabHost
				.newTabSpec("Iscrizioni")
				.setIndicator(null, res.getDrawable(R.drawable.iscrizioni))
				.setContent(iscrizioni);
		tabHost.addTab(iscrizTab);
	}

	private void initInfoButton() {
		String message = "Quest'applicazione accede ai server dell'Università di Verona "
				+ "utilizzando HTML e, per sicurezza, dei socket SSL.\n"
				+ "Non essendo note delle API per il sistema ESSE3, il recupero delle informazioni dalla "
				+ "pagina HTML avviene tramite un parser basato sulla libreria Jsoup (http://jsoup.org), "
				+ "il che potrà comprometterne in futuro il funzionamento. "
				+ "Il logo dell'applicazione è stato gentilmente fornito da Logogratis (http://www.logogratis.net). "
				+ "Questo software è stato sviluppato da Davide Vallicella per Julia Srl (http://www.juliasoft.com).";

		TextView text = new TextView(this);
		SpannableString s = new SpannableString(message);
		Linkify.addLinks(s, Linkify.WEB_URLS);
		text.setText(s);
		text.setMovementMethod(LinkMovementMethod.getInstance());

		final AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Informazioni")
				.setIcon(R.drawable.info)
				.setPositiveButton("Close", null)
				.setView(text).create();
		dialog.getWindow().getAttributes().dimAmount = 0.5f;
		dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

		Button ok = (Button) findViewById(R.id.id_ok_info);
		ok.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.show();
			}
		});
	}
}