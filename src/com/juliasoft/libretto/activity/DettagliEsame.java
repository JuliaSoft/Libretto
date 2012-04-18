package com.juliasoft.libretto.activity;

import java.net.ConnectException;

import javax.security.auth.login.LoginException;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.utils.Utils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class DettagliEsame extends Activity {
	private static final int CONNECTION_ERROR = 1;
	private static final int LOGIN_ERROR = 2;
	private TextView esame;
	private TextView anno_corso;
	private TextView aa_freq;
	private TextView peso_crediti;
	private TextView data_esame;
	private TextView voto;
	private TextView ric;
	private TextView q_val;
	private ImageView img;
	private Button back;
	private AlertDialog builder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.dettaggli_esame);
		init();

	}

	private void init() {
		Intent intent = getIntent();
		String pkg = getPackageName();
		String _esame = intent.getStringExtra(pkg + ".esame");
		String _anno_corso = intent.getStringExtra(pkg + ".anno_corso"); // i
																			// dati
		String _aa_freq = intent.getStringExtra(pkg + ".aa_freq");
		String _peso_crediti = intent.getStringExtra(pkg + ".peso_crediti");
		String _data_esame = intent.getStringExtra(pkg + ".data_esame");
		String _voto = intent.getStringExtra(pkg + ".voto");
		String _ric = intent.getStringExtra(pkg + ".ric");
		String _q_val = intent.getStringExtra(pkg + ".q_val");
		String _img = intent.getStringExtra(pkg + ".img");

		esame = (TextView) findViewById(R.id.info_esame);
		anno_corso = (TextView) findViewById(R.id.anno);
		aa_freq = (TextView) findViewById(R.id.aa_freq);
		peso_crediti = (TextView) findViewById(R.id.peso);
		data_esame = (TextView) findViewById(R.id.data);
		voto = (TextView) findViewById(R.id.voto);
		ric = (TextView) findViewById(R.id.riconosciuta);
		q_val = (TextView) findViewById(R.id.q_val);
		img = (ImageView) findViewById(R.id.id_infoEsa_img);
		back = (Button) findViewById(R.id.back);

		builder = new AlertDialog.Builder(this).setTitle("Dettagli esame")
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

		back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
		esame.setText(_esame);
		anno_corso.setText(_anno_corso);
		aa_freq.setText(_aa_freq);
		peso_crediti.setText(_peso_crediti);
		data_esame.setText(_data_esame);
		voto.setText(_voto);
		ric.setText(_ric);
		q_val.setText(_q_val);
		ConnectionManager cm = ConnectionManager.getInstance();
		if (Utils.isNetworkAvailable(getApplicationContext())) {
			if (!cm.isLogged()) {
				try {
					cm.authenticate();
				} catch (ConnectException e) {
					showMessage(CONNECTION_ERROR, e.getMessage());
				} catch (LoginException e) {
					showMessage(LOGIN_ERROR, e.getMessage());
				}
			}
			if (cm.isLogged())
				img.setImageBitmap(Utils.downloadBitmap(_img));
		}
	}

	private void showMessage(int type, String msg) {
		builder.setMessage(msg);
		showDialog(type);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONNECTION_ERROR:
		case LOGIN_ERROR:
			return builder;
		default:
			return super.onCreateDialog(id);
		}
	}
}
