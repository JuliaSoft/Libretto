package com.juliasoft.libretto.utils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.juliasoft.libretto.connection.SsolHttpClient;

public class Appello {
	private final String data;
	private final String ora;
	private final String tipo;
	private final String modulo;
	private final String docenti;
	private final String link;

	public Appello(Element fs) {
		String docenti = "-";
		String test = fs.select("p").text();
		String split[] = test.replace("Verbalizzante", ":Verbalizzante").split(":");
		if (split.length > 1) {
			docenti = "<b>" + split[0] + ": </b>" + split[1];
			if (split.length > 3)
				docenti += "<br><b>" + split[2] + ": </b>" + split[3];
		}
		this.docenti = docenti;

		Elements tds = fs.select("td.Content_Chiaro");
		Elements input = tds.get(4).children();
		tds.remove(4);
		String tipo = "", modulo = "", data = "", ora = "";

		for (int i = 0; i < tds.size(); i += 4) {
			if (tds.get(i).text().equals("Solo verbalizzazione"))
				tipo += "Verb." + "\n";
			else
				tipo += tds.get(i).text() + "\n";

			modulo += tds.get(i + 1).text() + "\n";
			data += tds.get(i + 2).text() + "\n";
			ora += tds.get(i + 3).text() + "\n";
		}

		this.tipo = tipo;
		this.modulo = modulo;
		this.data = data;
		this.ora = ora;

		String link;
		if (input.hasAttr("onclick"))
			link = SsolHttpClient.AUTH_URI + input.attr("onclick").split("'")[1];
		else
			link = "";

		// link per effettuare l'iscrizione
		if (!Utils.isLink(link))
			link = input.text();

		this.link = link;
	}

	public String getData() {
		return data;
	}

	public String getDocenti() {
		return docenti;
	}

	public String getLink() {
		return link;
	}

	public String getModulo() {
		return modulo;
	}

	public String getOra() {
		return ora;
	}

	public String getTipo() {
		return tipo;
	}
}