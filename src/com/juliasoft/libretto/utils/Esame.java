package com.juliasoft.libretto.utils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.juliasoft.libretto.connection.Esse3HttpClient;

public class Esame extends Row {

	private final String annoCorso;
	private final String aaFrequenza;
	private final String numeroCrediti;
	private final String data;
	private final String voto;
	private final String ric;
	private final String q_val;
	private final String stato_gif;

	public Esame(Element element) {
		super (element.attr("nome"));

		annoCorso = element.attr("annocorso");
		aaFrequenza = element.attr("annofreq");
		numeroCrediti = element.attr("crediti");
		data = element.attr("data");
		voto = element.attr("voto");
		ric = element.attr("ric");
		q_val = element.attr("qval");
		stato_gif = element.attr("image");
	}

	public Esame(Elements tds) {
		super(tds.get(2).text().split(" - ", 2)[1]);

		annoCorso = tds.get(1).text();
		stato_gif = Esse3HttpClient.AUTH_URI + tds.get(8).select("img").first().attr("src");
		aaFrequenza = tds.get(9).text();
		numeroCrediti = tds.get(10).text();
		data = tds.get(11).text();
		voto = tds.get(12).text();
		ric = tds.get(14).text();
		q_val = tds.get(15).text();
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