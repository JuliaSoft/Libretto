package com.juliasoft.libretto.activity;

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

public class Medie extends Activity {

	private String media_aritm;
	private String media_pond;
	private String num;
	private int crediti;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.media);
		init();
	}

	private void init() {
		Intent intent = getIntent();
		String pkg = getPackageName();
		media_aritm = intent.getStringExtra(pkg + ".aritm");
		media_pond = intent.getStringExtra(pkg + ".pond");
		num = intent.getStringExtra(pkg + ".num");
		crediti = Integer.parseInt(intent.getStringExtra(pkg + ".crediti"));

		TextView percent = (TextView) findViewById(R.id.tv_media_percent);
		int x100 = (int) Math.round(((double) crediti * 100) / 180);
		percent.setText(x100 + " %");
		TextView tvMa = (TextView) findViewById(R.id.idMediaA);
		tvMa.setText(media_aritm + " / 30");
		TextView tvMp = (TextView) findViewById(R.id.idMediaP);
		tvMp.setText(media_pond + " / 30");
		TextView tvNum = (TextView) findViewById(R.id.idNumE);
		tvNum.setText(num);
		TextView tvCrediti = (TextView) findViewById(R.id.idNumC);
		tvCrediti.setText(crediti + " / 180");
		ProgressBar pg = (ProgressBar) findViewById(R.id.idProgBarC);
		pg.setMax(180);

		final float[] roundedCorners = new float[] { 5, 5, 5, 5, 5, 5, 5, 5 };
		ShapeDrawable pgDrawable = new ShapeDrawable(new RoundRectShape(
				roundedCorners, null, null));
		String color = "";
		if (crediti <= 40)
			color = "#FF0000";
		else if (crediti <= 80)
			color = "#FF6600";
		else if (crediti <= 120)
			color = "#FFFF00";
		else if (crediti <= 160)
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
}