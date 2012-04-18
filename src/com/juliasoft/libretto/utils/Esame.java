package com.juliasoft.libretto.utils;

public class Esame {

	private String esame;
	private String anno_corso;
	private String aa_freq;
	private String peso_crediti;
	private String data_esame;
	private String voto;
	private String ric;
	private String q_val;
	private String stato_gif;

	public String getAa_freq() {
		return aa_freq;
	}

	public String getAnno_corso() {
		return anno_corso;
	}

	public String getData_esame() {
		return data_esame;
	}

	public String getEsame() {
		return esame;
	}

	public String getPeso_crediti() {
		return peso_crediti;
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

	public void setAa_freq(String aa_freq) {
		this.aa_freq = aa_freq;
	}

	public void setAnno_corso(String anno_corso) {
		this.anno_corso = anno_corso;
	}

	public void setData_esame(String data_esame) {
		this.data_esame = data_esame;
	}

	public void setEsame(String esame) {
		this.esame = esame;
	}

	public void setPeso_crediti(String peso_crediti) {
		this.peso_crediti = peso_crediti;
	}

	public void setQ_val(String q_val) {
		this.q_val = q_val;
	}

	public void setRic(String ric) {
		this.ric = ric;
	}

	public void setStato_gif(String stato_gif) {
		this.stato_gif = stato_gif;
	}

	public void setVoto(String voto) {
		this.voto = voto;
	}

	@Override
	public String toString() {
		return esame;
	}
}