package com.juliasoft.libretto.activity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;

import android.R.integer;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.utils.Utils;

public class Medie extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.media);
		init();
	}

	private void init() {
		Intent intent = getIntent();
		String pkg = getPackageName();
		String meditaAritmetica = intent.getStringExtra(pkg + ".aritm");
		String mediaPonderata = intent.getStringExtra(pkg + ".pond");
		String num = intent.getStringExtra(pkg + ".num");
		int crediti = Integer.parseInt(intent.getStringExtra(pkg + ".crediti"));

		ConnectionManager cm = ConnectionManager.getInstance();
		int creditiTotali = 0;
		String page_HTML = cm.connection(ConnectionManager.ESSE3,
				Utils.TARGET_HOME, null);
		Element dd = Utils.jsoupSelect(page_HTML, "div#boxRiepilogoEsami")
				.first().siblingElements().select("dd").get(3);
		if (dd != null)
			try {
				Pattern pattern = Pattern.compile("1[28]+0");
				Matcher matcher = pattern.matcher(dd.text());
				if (matcher.find())
					creditiTotali = Integer.parseInt(matcher.group());
			} catch (Exception e) {
				Utils.appendToLogFile("Medie init()", e.getMessage());
			}

		TextView percent = (TextView) findViewById(R.id.tv_media_percent);
		double x100 = ((double) crediti * 100) / creditiTotali;
		percent.setText(arrotonda(x100, 2) + " %");
		TextView tvMa = (TextView) findViewById(R.id.idMediaA);
		tvMa.setText(meditaAritmetica + " / 30");
		TextView tvMp = (TextView) findViewById(R.id.idMediaP);
		tvMp.setText(mediaPonderata + " / 30");
		TextView tvNum = (TextView) findViewById(R.id.idNumE);
		tvNum.setText(num);
		TextView tvCrediti = (TextView) findViewById(R.id.idNumC);
		tvCrediti.setText(crediti + " / " + creditiTotali);
		ProgressBar pg = (ProgressBar) findViewById(R.id.idProgBarC);
		pg.setMax(creditiTotali);

		final float[] roundedCorners = new float[] { 5, 5, 5, 5, 5, 5, 5, 5 };
		ShapeDrawable pgDrawable = new ShapeDrawable(new RoundRectShape(
				roundedCorners, null, null));
		String color;
		int passo = creditiTotali / 5;
		if (crediti <= passo)
			color = "#FF0000";
		else if (crediti <= passo * 2)
			color = "#FF6600";
		else if (crediti <= passo * 3)
			color = "#FFFF00";
		else if (crediti <= passo * 4)
			color = "#CCFF00";
		else
			color = "#00FF00";

		pgDrawable.getPaint().setColor(Color.parseColor(color));
		ClipDrawable progress = new ClipDrawable(pgDrawable, Gravity.LEFT,
				ClipDrawable.HORIZONTAL);
		pg.setProgressDrawable(progress);
		pg.setBackgroundDrawable(getResources().getDrawable(
				android.R.drawable.progress_horizontal));
		pg.setProgress(crediti);
	}

	public double arrotonda(double numero, int nCifreDecimali) {
		return Math.round(numero * Math.pow(10, nCifreDecimali))
				/ Math.pow(10, nCifreDecimali);
	}
}