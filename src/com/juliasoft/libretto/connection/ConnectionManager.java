package com.juliasoft.libretto.connection;

import java.io.IOException;
import java.net.ConnectException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.apache.http.cookie.Cookie;

import android.util.Log;

import com.juliasoft.libretto.utils.Utils;

public class ConnectionManager {

	public static final String TAG = ConnectionManager.class.getName();

	public static final int SSOL = 0;
	public static final int ESSE3 = 1;

	private static ConnectionManager instance;
	private static KeyStore trustStore;
	private static Map<Integer, List<Cookie>> cookies;

	private String username;
	private String password;

	private HttpConnection ssolConn;
	private HttpConnection esse3Conn;

	private boolean isLogged;
	private String ssol_login_HTML;

	private ConnectionManager() {
		isLogged = false;
		username = "";
		password = "";
		cookies = new HashMap<Integer, List<Cookie>>();
	}

	public static ConnectionManager getInstance() {
		if (instance == null) {
			instance = new ConnectionManager();
			Log.i("INFO", "Start new ClientManager!");
		}

		return instance;
	}

	public static KeyStore getTrustStore(String url, int port)
			throws KeyManagementException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException {
		if (trustStore == null)
			trustStore = new InstallCert(url, port).getUpdatedTrustStore();

		return trustStore;
	}

	public void authenticate() throws ConnectException, LoginException {
		Log.i(TAG, "Login user: " + username);
		isLogged = false;

		// Login presso univr.esse3.cineca.it
		esse3Conn = new HttpConnection(new Esse3HttpClient(username, password));
		esse3Conn.get(Esse3HttpClient.AUTH_URI);
		esse3Conn.getEntity();
		esse3Conn.consumeContent();

		// Login presso www.ssol.univr.it
		ssolConn = new HttpConnection(new SsolHttpClient());
		Map<String, String> params = new HashMap<String, String>();
		params.put("username", username);
		params.put("password", password);
		ssolConn.post(SsolHttpClient.AUTH_URI + "main?ent=login", params);
		ssolConn.getEntity();

		try {
			ssol_login_HTML = Utils.inputStreamToString(ssolConn.getEntity().getContent());
		}
		catch (IOException e) {}

		if (ssol_login_HTML.contains("Content_Chiaro Warning"))
			throw new LoginException(HttpConnection.HTTP_UNAUTHORIZED_EXCEPTION);

		isLogged = true;
		Log.i(TAG, "Login OK");
		// After Login save cookies
		cookies.put(ESSE3, esse3Conn.getCookies());
		cookies.put(SSOL, ssolConn.getCookies());
		Log.i(TAG, "Login OK");
	}

	public List<Cookie> getSSOLCookies() {
		return cookies.get(SSOL);
	}

	public List<Cookie> getESSE3Cookies() {
		return cookies.get(ESSE3);
	}

	public String getSSOLLoginHTML() {
		return ssol_login_HTML;
	}

	public String connection(int type, String url,
			HashMap<String, String> params) {
		Log.i(TAG, "Connessione URL: " + url);

		try {
			if (!isLogged)
				authenticate();

			switch (type) {
			case ESSE3:
				return getHTML(esse3Conn, url);
			case SSOL:
				return getHTML(ssolConn, url);
			}
		}
		catch (Exception e) {
			isLogged = false;
			Log.e(TAG, "Error connection(..): " + e.getMessage());
		}

		return null;
	}

	private String getHTML(HttpConnection connection, String url) throws ConnectException, IllegalStateException, LoginException, IOException {
		if (Utils.isLink(url))
			connection.get(url);

		return Utils.inputStreamToString(connection.getEntity().getContent());
	}

	public void reset() {
		isLogged = false;

		if (ssolConn != null)
			ssolConn.reset();

		if (esse3Conn != null)
			esse3Conn.reset();

		instance = null;
	}

	public HttpConnection getEsse3Connection() {
		return esse3Conn;
	}

	public HttpConnection getSsolConnection() {
		return ssolConn;
	}

	public void setLogged(boolean value) {
		isLogged = value;
	}

	public void setCredentials(String user, String pass) {
		this.username = user;
		this.password = pass;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public boolean isLogged() {
		return isLogged;
	}
}
