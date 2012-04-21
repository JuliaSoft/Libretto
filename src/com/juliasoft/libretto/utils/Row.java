package com.juliasoft.libretto.utils;

public abstract class Row {
	private final String name;

	protected Row(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	public String getNome() {
		return name;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Esame && ((Esame) other).getNome().equals(name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
