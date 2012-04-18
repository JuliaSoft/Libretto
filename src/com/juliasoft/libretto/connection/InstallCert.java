package com.juliasoft.libretto.connection;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.juliasoft.libretto.utils.Utils;
import android.os.Environment;
import android.util.Log;

public class InstallCert {

	public static final String TAG = InstallCert.class.getName();
	private String host;
	private int port;
	private File sd;
	private File trust;

	public InstallCert(String url, int port) {
		this.host = url;
		this.port = port;
		sd = Environment.getExternalStorageDirectory();
		trust = new File(sd, "univr.esse3.cineca.it");
	}

	public KeyStore getUpdatedTrustStore() throws KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException,
			KeyManagementException {

		KeyStore trustStore = Utils.loadTrustFile();

		SSLContext context = SSLContext.getInstance("TLS");
		TrustManagerFactory tmf = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(trustStore);
		X509TrustManager defaultTrustManager = (X509TrustManager) tmf
				.getTrustManagers()[0];
		SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
		context.init(null, new TrustManager[] { tm }, null);
		SSLSocketFactory factory = context.getSocketFactory();
		SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
		socket.setSoTimeout(10000);

		try {
			socket.startHandshake();
			socket.close();
			Log.i(TAG, "Certificate is already trusted");
			return trustStore;
		} catch (SSLException e) {
			Log.i(TAG, "Certificate is not trusted");
		}

		X509Certificate[] chain = tm.chain;
		if (chain == null) {
			Log.i(TAG, "Could not obtain server certificate chain");
			return null;
		}

		System.out.println();
		System.out.println("Server sent " + chain.length + " certificate(s):");
		System.out.println();
		MessageDigest sha1 = MessageDigest.getInstance("SHA1");
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		for (int i = 0; i < chain.length; i++) {
			X509Certificate cert = chain[i];
			System.out.println(" " + (i + 1) + " Subject "
					+ cert.getSubjectDN());
			System.out.println("   Issuer  " + cert.getIssuerDN());
			sha1.update(cert.getEncoded());
			System.out.println("   sha1    " + toHexString(sha1.digest()));
			md5.update(cert.getEncoded());
			System.out.println("   md5     " + toHexString(md5.digest()));
			System.out.println();
		}
		int k = 0;

		X509Certificate cert = chain[k];
		String alias = host + "-" + (k + 1);
		trustStore.setCertificateEntry(alias, cert);

		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(trust),
					8 * 1024);
			trustStore.store(out, "changeit".toCharArray());
			out.close();
		} catch (Exception e) {
			Log.e(TAG, "Errore truststore non salvato!");
		} finally {
			if (out != null) {
				out.close();
			}
		}

		return trustStore;
	}

	private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

	private static String toHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 3);
		for (int b : bytes) {
			b &= 0xff;
			sb.append(HEXDIGITS[b >> 4]);
			sb.append(HEXDIGITS[b & 15]);
			sb.append(' ');
		}
		return sb.toString();
	}

	private static class SavingTrustManager implements X509TrustManager {

		private final X509TrustManager tm;
		private X509Certificate[] chain;

		SavingTrustManager(X509TrustManager tm) {
			this.tm = tm;
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			this.chain = chain;
			tm.checkServerTrusted(chain, authType);
		}
	}
}
