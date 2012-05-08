package com.juliasoft.libretto.utils;

import org.jsoup.select.Elements;

import com.juliasoft.libretto.connection.Esse3HttpClient;

public class Esame extends Row {

	private  String id;
	private  String annoCorso;
	private  String aaFrequenza;
	private  String numeroCrediti;
	private  String data;
	private  String voto;
	private  String ric;
	private  String q_val;
	private  String stato_gif;

	public Esame(Elements tds) {
		super(tds.get(2).text().split(" - ", 2)[1]);
		try {
			id = tds.get(2).text().split(" - ", 2)[0];
			annoCorso = tds.get(1).text();
			stato_gif = Esse3HttpClient.AUTH_URI
					+ tds.get(8).select("img").first().attr("src");
			aaFrequenza = tds.get(9).text();
			numeroCrediti = tds.get(10).text();
			data = tds.get(11).text();
			voto = tds.get(12).text();
			ric = tds.get(14).text();
			q_val = tds.get(15).text();
		} catch (Exception e) {
			Utils.appendToLogFile("Esame Esame()", e.getMessage());
		}
	}

	public String getId() {
		return id;
	}

	public String getAa_freq() {
		return aaFrequenza;
	}

	public String getAnno_corso() {
		return annoCorso;
	}

	public String getData() {
		return data;
	}

	public String getCrediti() {
		return numeroCrediti;
	}

	public String getQ_val() {
		return q_val;
	}

	public String getRic() {
		return ric;
	}

	public String getStato_gif() {
		return stato_gif;
	}

	public String getVoto() {
		return voto;
	}
}