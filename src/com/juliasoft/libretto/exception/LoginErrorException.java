package com.juliasoft.libretto.exception;

public class LoginErrorException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String msg;

	public LoginErrorException(String msg) {
		this.msg = msg;
	}

	@Override
	public String getMessage() {
		// TODO Auto-generated method stub
		return msg;
	}

}
