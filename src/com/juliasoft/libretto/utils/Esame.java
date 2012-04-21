package com.juliasoft.libretto.utils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.juliasoft.libretto.connection.Esse3HttpClient;

public class Esame {

	private String nome;
	private String annoCorso;
	private String aaFrequenza;
	private String numeroCrediti;
	private String data;
	private String voto;
	private String ric;
	private String q_val;
	private String stato_gif;

	public Esame() {}

	public Esame(Element element) {
		nome = element.attr("nome");
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
		nome = tds.get(2).text().split(" - ", 2)[1];
		annoCorso = tds.get(1).text();
		stato_gif = Esse3HttpClient.AUTH_URI + tds.get(8).select("img").first().attr("src");
		aaFrequenza = tds.get(9).text();
		numeroCrediti = tds.get(10).text();
		data = tds.get(11).text();
		voto = tds.get(12).text();
		ric = tds.get(14).text();
		q_val = tds.get(15).text();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Esame && ((Esame) other).getNome().equals(nome);
	}

	@Override
	public int hashCode() {
		return nome.hashCode();
	}

	public String getAa_freq() {
		return aaFrequenza;
	}

	public String getAnno_corso() {
		return annoCorso;
	}

	public String getData_esame() {
		return data;
	}

	public String getNome() {
		return nome;
	}

	public String getPeso_crediti() {
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

	public void setAa_freq(String aa_freq) {
		this.aaFrequenza = aa_freq;
	}

	public void setAnno_corso(String anno_corso) {
		this.annoCorso = anno_corso;
	}

	public void setData_esame(String data_esame) {
		this.data = data_esame;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public void setPeso_crediti(String peso_crediti) {
		this.numeroCrediti = peso_crediti;
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
		return nome;
	}
}