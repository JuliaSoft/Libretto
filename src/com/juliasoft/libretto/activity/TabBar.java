package com.juliasoft.libretto.activity;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class TabBar extends TabActivity implements OnClickListener {

	public static final String TAG = TabActivity.class.getName();

	private Button ok;
	private WindowManager.LayoutParams lp;
	private AlertDialog dialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_bar);
		init();
	}

	private void init() {
		// INIT GUI
		ok = (Button) findViewById(R.id.id_ok_info);
		ok.setOnClickListener(this);
		dialog = new AlertDialog.Builder(TabBar.this).setTitle("Informazioni")
				.setMessage(infoMessage())
				.setIcon(R.drawable.info)
				.setPositiveButton("Close", null).create();

		lp = dialog.getWindow().getAttributes();
		lp.dimAmount = 0.5f;

		dialog.getWindow().setAttributes(lp);
		dialog.getWindow()
				.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

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
				.setIndicator(null,
						res.getDrawable(R.drawable.informazioni))
				.setContent(informazioni);
		tabHost.addTab(infoTab);

		// LIBRETTO
		TabSpec librettoTab = tabHost
				.newTabSpec("Libretto")
				.setIndicator(null,
						res.getDrawable(R.drawable.libretto))
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
				.setIndicator(null,
						res.getDrawable(R.drawable.iscrizioni))
				.setContent(iscrizioni);
		tabHost.addTab(iscrizTab);
	}

	private String infoMessage() {
		return "Quest'applicazione si basa sull'utilizzo del protocollo HTTP per "
				+ "effettuare richieste ai server dell'Universit� di Verona, "
				+ "mantenendo un certo livello di sicurezza per mezzo "
				+ "dell'utilizzo di socket SSL.\n"
				+ "Successivamente il recupero delle informazioni utili dalla "
				+ "pagina HTML avviene tramite un parser basato sull'uso di Jsoup";
	}

	@Override
	public void onClick(View v) {
		if (v == ok) {
			dialog.show();
		}
	}
}