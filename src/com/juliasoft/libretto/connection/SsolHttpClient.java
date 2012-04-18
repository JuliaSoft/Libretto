package com.juliasoft.libretto.connection;

import java.security.KeyStore;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.HttpConnectionParams;

import android.util.Log;

public class SsolHttpClient extends DefaultHttpClient {

	public static final String TAG = SsolHttpClient.class.getName();
	public static final String DOMAIN = "www.ssol.univr.it";
	public static final String AUTH_URI = "https://" + DOMAIN + "/";
	public static final int SICURE_PORT = 443;
	public static final int DEFAULT_PORT = 80;

	private ClientConnectionManager ccm = null;

	private SSLSocketFactory createFactory() {

		// Log.d(TAG, "Creating ssl socket");

		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore
					.getDefaultType());
			trustStore.load(null, null);
			// Pass the keystore to the SSLSocketFactory. The factory is
			// responsible
			// for the verification of the server certificate.
			SSLSocketFactory factory = new MySSLSocketFactory(trustStore);
			factory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			// final SSLSocketFactory factory = new
			// SSLSocketFactory(ConnectionManager.getTrustStore(DOMAIN, PORT));
			// Hostname verification from certificate
			factory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			return factory;
		} catch (Exception e) {
			Log.e(TAG,
					"Caught exception when trying to create ssl socket factory. Reason: "
							+ e.getMessage());
		}

		return null;
	}

	@Override
	protected ClientConnectionManager createClientConnectionManager() {

		if (ccm == null) {
			// Log.d(TAG, "Creating client connection manager");
			final SchemeRegistry registry = new SchemeRegistry();

			// Log.d(TAG, "Adding https scheme for port " + SICURE_PORT);
			registry.register(new Scheme("https", this.createFactory(),
					SICURE_PORT));
			registry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), DEFAULT_PORT));
			HttpConnectionParams.setSoTimeout(getParams(), 5000);
			HttpConnectionParams.setConnectionTimeout(getParams(), 5000);
			ccm = new SingleClientConnManager(getParams(), registry);
		}
		return ccm;
	}
}
