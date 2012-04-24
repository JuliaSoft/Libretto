package com.juliasoft.libretto.connection;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.HttpConnectionParams;

import android.util.Log;

public class Esse3HttpClient extends DefaultHttpClient {

	public static final String TAG = Esse3HttpClient.class.getName();
	public static final String DOMAIN = "univr.esse3.cineca.it";
	public static final String AUTH_URI = "https://" + DOMAIN + "/";
	public static final int PORT = 443;

	private ClientConnectionManager ccm;
	private CredentialsProvider cp;

	private final String user;
	private final String pass;

	public Esse3HttpClient(String user, String pass) {
		this.user = user;
		this.pass = pass;
	}

	private SSLSocketFactory newSslSocketFactory() {
		try {
			// Pass the keystore to the SSLSocketFactory.
			// The factory is responsible for the verification
			// of the server certificate.
			SSLSocketFactory factory = new SSLSocketFactory(ConnectionManager.getTrustStore(DOMAIN, PORT));
			// Hostname verification from certificate
			factory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			return factory;
		} catch (final Exception e) {
			Log.e(TAG, "Caught exception when trying to create ssl socket factory. Reason: " + e.getMessage());

			return null;
		}
	}

	@Override
	protected ClientConnectionManager createClientConnectionManager() {
		if (ccm == null) {
			SchemeRegistry registry = new SchemeRegistry();

			// Register our SSLSocketFactory (with our Keystore) for port 443
			registry.register(new Scheme("https", this.newSslSocketFactory(), PORT));
			HttpConnectionParams.setSoTimeout(getParams(), 5000);
			HttpConnectionParams.setConnectionTimeout(getParams(), 5000);
			ccm = new SingleClientConnManager(getParams(), registry);
		}
		return ccm;
	}

	@Override
	protected CredentialsProvider createCredentialsProvider() {
		if (cp == null) {
			cp = new BasicCredentialsProvider();
			cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, pass));
		}
		return cp;
	}
}
