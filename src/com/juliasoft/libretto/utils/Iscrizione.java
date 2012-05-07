package com.juliasoft.libretto.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.juliasoft.libretto.connection.SsolHttpClient;

public class Iscrizione {

	private HashMap<String, String> params;
	private List<String> types;
	private String data;
	private String iscritti;
	private String luogo;
	private String verbalizzazione;
	private String numero;
	private String url;
	private boolean isEnable;
	private boolean isRegister;

	public Iscrizione(Element form) {
		params = new HashMap<String, String>();
		types = new ArrayList<String>();
		isEnable = true;
		url = SsolHttpClient.AUTH_URI + form.attr("action");
		
		Elements inputs = form.select("input");

		for (Element input : inputs)
			params.put(input.attr("name"), input.attr("value"));

		Elements trs = form.select("table").select("tr");
		String action = params.get("azione");

		if (action.equals("INSERT")) {
			isRegister = false;
			//types.clear();
			Elements options = form.select("select>option");
			for (Element option : options)
				types.add(option.text());

			Elements tds = trs.get(0).select("td.Content_Chiaro");
			data = tds.get(0).text();
			
			iscritti = tds.get(1).text();
			if (tds.get(1).select("input[type=submit]").isEmpty()) {
				isEnable = false;
				iscritti = iscritti.replace("Iscrizioni chiuse", "");
			}

			tds = trs.get(2).select("td.Content_Chiaro");
			luogo = tds.text();

			tds = trs.get(3).select("td.Content_Chiaro");
			verbalizzazione = tds.text();
		}

		// Se sono gi� iscritto all'esame
		if (action.equals("DELETE")) {
			isRegister = true;
			Elements tds = trs.get(0).select("td.Content_Chiaro");
			numero = tds.get(0).text();
			
			iscritti = tds.get(1).text();
			if (tds.get(1).select("input[type=submit]").isEmpty()) {
				isEnable = false;
				iscritti = iscritti.replace("Iscrizioni chiuse", "");
			}
		
			tds = trs.get(1).select("td.Content_Chiaro");
			data = tds.text();

			tds = trs.get(2).select("td.Content_Chiaro");
			types.add(tds.text());

			tds = trs.get(3).select("td.Content_Chiaro");
			luogo = tds.text();

			tds = trs.get(4).select("td.Content_Chiaro");
			verbalizzazione = tds.text();
		}		
	}
	
	public String getData() {
		return data;
	}

	public String getLuogo() {
		return luogo;
	}

	public String getNumero() {
		return numero;
	}
	
	public String getUrl() {
		return url;
	}

	public String getParam(String key) {
		return params.get(key);
	}

	public HashMap<String, String> getParams() {
		return params;
	}
	
	public void addParams(String key, String value){
		params.put(key, value);
	}

	public String getTotIscritti() {
		return iscritti;
	}

	public List<String> getTypes() {
		return types;
	}

	public String getVerb() {
		return verbalizzazione;
	}

	public boolean isEnable() {
		return isEnable;
	}

	public boolean isRegister() {
		return isRegister;
	}
}
