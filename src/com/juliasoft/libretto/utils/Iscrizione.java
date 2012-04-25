package com.juliasoft.libretto.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Iscrizione {

	private final HashMap<String, String> params;
	private final List<String> types;
	private String data;
	private String iscritti;
	private String luogo;
	private String verbalizzazione;
	private String numero;
	private boolean isEnable;
	private boolean isRegister;

	public Iscrizione() {
		params = new HashMap<String, String>();
		types = new ArrayList<String>();
		isEnable = true;
		isRegister = false;
	}

	public void addParam(String key, String value) {
		params.put(key, value);
	}

	public void addType(String type) {
		types.add(type);
	}

	public void clearTypes() {
		types.clear();
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

	public String getParam(String key) {
		return params.get(key);
	}

	public HashMap<String, String> getParams() {
		return params;
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

	public void setRegister(boolean value) {
		isRegister = value;
	}

	public void setData(String value) {
		data = value;
	}

	public void setEnable(boolean value) {
		isEnable = value;
	}

	public void setIscritti(String value) {
		iscritti = value;
	}

	public void setLuogo(String value) {
		luogo = value;
	}

	public void setNumero(String value) {
		numero = value;
	}

	public void setVerbalizzazione(String value) {
		verbalizzazione = value;
	}
}
