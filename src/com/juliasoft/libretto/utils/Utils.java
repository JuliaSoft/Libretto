package com.juliasoft.libretto.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.juliasoft.libretto.activity.DettagliEsame;
import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.connection.Esse3HttpClient;
import com.juliasoft.libretto.connection.SsolHttpClient;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.util.Log;

public class Utils {

	private static final boolean DEBUG = true;
	public static final String TAG = Utils.class.getName();
	/********************************** LINK FACOLTï¿½ **********************************/
	public static final String TARGET_LIBRETTO = Esse3HttpClient.AUTH_URI
			+ "auth/studente/Libretto/LibrettoHome.do";
	public static final String TARGET_HOME = Esse3HttpClient.AUTH_URI
			+ "auth/Home.do";
	public static final String TARGET_PIANO_STUDIO = Esse3HttpClient.AUTH_URI
			+ "auth/studente/Piani/PianiHome.do";
	public static final String TARGET_ISCRIZIONI = SsolHttpClient.AUTH_URI
			+ "main?ent=libretto";
	public static final String TARGET_ISCRIZIONI_OLD = SsolHttpClient.AUTH_URI
			+ "main?ent=ieappellics";

	/*
	 * @return boolean return true if the application can access the internet
	 */
	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (connectivity.getActiveNetworkInfo() != null
				&& connectivity.getActiveNetworkInfo().isAvailable()
				&& connectivity.getActiveNetworkInfo().isConnected()) {

			try {
				URLConnection httpConn = new URL("http://www.google.it")
						.openConnection();
				httpConn.setConnectTimeout(5000);
				httpConn.connect();
				if (DEBUG)
					Log.d(TAG, "ONLINE!");
				return true;
			} catch (Exception e) {
				if (DEBUG)
					Log.d(TAG, "OFFLINE!");
				return false;
			}
		} else {
			if (DEBUG)
				Log.d(TAG, "OFFLINE!");
			return false;
		}
	}

	public static Bitmap downloadBitmap(Context context, String fileUrl) {
		ConnectionManager cm = ConnectionManager.getInstance();

		if (isNetworkAvailable(context)) {
			InputStream is = null;

			if (Utils.isLink(fileUrl)) {
				try {
					cm.getEsse3Connection().get(fileUrl);
					is = cm.getEsse3Connection().getEntity().getContent();
					return BitmapFactory.decodeStream(is);
				} catch (Exception e) {
					if (DEBUG)
						Log.e(TAG, "Error downloadBitmap(): " + e.getMessage());
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (IOException e) {
							if (DEBUG)
								Log.e(TAG,
										"Error downloadBitmap(): Chiusura file non riuscita!");
						}
					}
				}
			}
		} else if (DEBUG)
			Log.e(TAG, "La connessione NON è attiva!");
		return null;
	}

	public static Elements jsoupSelect(String page_HTML, String query) {
		return Jsoup.parse(page_HTML).select(query);
	}

	// Riceve l'input stream della pagina e lo converte in stringa
	public static String inputStreamToString(InputStream is) {
		String line = "";
		BufferedReader in = null;

		StringBuilder sb = new StringBuilder(line);
		String NL = System.getProperty("line.separator");
		try {
			in = new BufferedReader(new InputStreamReader(is), 8 * 1024);
			while ((line = in.readLine()) != null)
				sb.append(line).append(NL);
		} catch (IOException e) {
			if (DEBUG)
				Log.d(TAG,
						"Error inputStreamToString(): Reader is closed or some other I/O error occurs.");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					if (DEBUG)
						Log.d(TAG,
								"Error inputStreamToString(): The connection has not been released!");
				}
			}
		}

		return sb.toString();
	}

	public static boolean isLink(String link) {
		if (link == null || link.equals(""))
			return false;

		Pattern pattern = Pattern.compile("(http|https)\\://(.*?)");
		Matcher matcher = pattern.matcher(link);
		return matcher.matches();
	}

	public static String removeSpecialString(String page_HTML, String regex) {
		// ELIMINO STRINGHE SPECIALI
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(page_HTML);
		while (matcher.find())
			page_HTML = matcher.replaceAll("");

		return page_HTML;
	}
}