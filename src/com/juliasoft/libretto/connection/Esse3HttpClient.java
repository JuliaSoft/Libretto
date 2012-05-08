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

import com.juliasoft.libretto.utils.Utils;

import android.util.Log;

public class Esse3HttpClient extends DefaultHttpClient {

	private static final boolean DEBUG = true;
	private static final String TAG = Esse3HttpClient.class.getName();

	public static final String DOMAIN = "univr.esse3.cineca.it";
	public static final String AUTH_URI = "https://" + DOMAIN + "/";
	public static final int SICURE_PORT = 443;

	private ClientConnectionManager ccm;
	private CredentialsProvider cp;

	private final String user;
	private final String pass;

	public Esse3HttpClient(String user, String pass) {
		this.user = user;
		this.pass = pass;
	}

	private SSLSocketFactory newSslSocketFactory() {
		SSLSocketFactory factory;
		try {
			factory = new SSLSocketFactory(ConnectionManager.getTrustStore(DOMAIN, SICURE_PORT));			
		} catch (final Exception e) {
			factory = SSLSocketFactory.getSocketFactory();
			Utils.appendToLogFile("Esse3HttpClient newSslSocketFactory()", e.getMessage());
			if (DEBUG)
				Log.e(TAG, "Caught exception when trying to create ssl socket factory. Reason: " + e.getMessage());
		}
		
		factory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		
		return factory;
	}

	@Override
	protected ClientConnectionManager createClientConnectionManager() {
		if (ccm == null) {
			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("https", newSslSocketFactory(), SICURE_PORT));
			HttpConnectionParams.setSoTimeout(getParams(), 10000);
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
