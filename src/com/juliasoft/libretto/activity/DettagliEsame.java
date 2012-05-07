package com.juliasoft.libretto.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.juliasoft.libretto.utils.Utils;

public class DettagliEsame extends Activity {

	private static final boolean DEBUG = true;
	private static final String TAG = DettagliEsame.class.getName();
	
	private AlertDialog allertDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dettagli_esame);
		init();
	}

	private void init() {
		Intent intent = getIntent();
		String pkg = getPackageName();

		TextView esame = (TextView) findViewById(R.id.info_esame);
		TextView anno_corso = (TextView) findViewById(R.id.anno);
		TextView aa_freq = (TextView) findViewById(R.id.aa_freq);
		TextView peso_crediti = (TextView) findViewById(R.id.peso);
		TextView data_esame = (TextView) findViewById(R.id.data);
		TextView voto = (TextView) findViewById(R.id.voto);
		TextView ric = (TextView) findViewById(R.id.riconosciuta);
		TextView q_val = (TextView) findViewById(R.id.q_val);
		ImageView img = (ImageView) findViewById(R.id.id_infoEsa_img);
		Button back = (Button) findViewById(R.id.back);
		back.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		esame.setText(intent.getStringExtra(pkg + ".esame"));
		anno_corso.setText(intent.getStringExtra(pkg + ".anno_corso"));
		aa_freq.setText(intent.getStringExtra(pkg + ".aa_freq"));
		peso_crediti.setText(intent.getStringExtra(pkg + ".peso_crediti"));
		data_esame.setText(intent.getStringExtra(pkg + ".data_esame"));
		voto.setText(intent.getStringExtra(pkg + ".voto"));
		ric.setText(intent.getStringExtra(pkg + ".ric"));
		q_val.setText(intent.getStringExtra(pkg + ".q_val"));
		img.setImageBitmap(Utils.downloadBitmap(DettagliEsame.this, intent.getStringExtra(pkg + ".img")));
		initDialog();
	}

	private void initDialog() {
		allertDialog = new AlertDialog
				.Builder(DettagliEsame.this)
				.setTitle("Dettagli esame")
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
}
