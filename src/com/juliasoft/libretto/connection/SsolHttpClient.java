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

	private static final boolean DEBUG = true;
	private static final String TAG = SsolHttpClient.class.getName();
	
	public static final String DOMAIN = "www.ssol.univr.it";
	public static final String AUTH_URI = "https://" + DOMAIN + "/";
	public static final int SICURE_PORT = 443;
	public static final int DEFAULT_PORT = 80;

	private ClientConnectionManager ccm = null;

	private SSLSocketFactory newSslSocketFactory() {
		SSLSocketFactory factory = SSLSocketFactory.getSocketFactory();
		
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);
			factory = new MySSLSocketFactory(trustStore);
		} catch (Exception e) {
			if(DEBUG)
				Log.e(TAG, "Caught exception when trying to create ssl socket factory. Reason: " + e.getMessage());
		}

		factory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		
		return factory;
	}

	@Override
	protected ClientConnectionManager createClientConnectionManager() {
		if (ccm == null) {
			final SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("https", newSslSocketFactory(), SICURE_PORT));
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), DEFAULT_PORT));			
			HttpConnectionParams.setSoTimeout(getParams(), 10000);			
			ccm = new SingleClientConnManager(getParams(), registry);
		}
		
		return ccm;
	}
}
