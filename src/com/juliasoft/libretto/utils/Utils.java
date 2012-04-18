package com.juliasoft.libretto.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.juliasoft.libretto.connection.ConnectionManager;
import com.juliasoft.libretto.connection.Esse3HttpClient;
import com.juliasoft.libretto.connection.SsolHttpClient;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.util.Log;

public class Utils {

	public static final String TAG = Utils.class.getName();
	/********************************** LINK FACOLT� **********************************/
	public static final String TARGET_LIBRETTO = Esse3HttpClient.AUTH_URI
			+ "auth/studente/Libretto/LibrettoHome.do";
	public static final String TARGET_INFO = Esse3HttpClient.AUTH_URI
			+ "auth/Home.do";
	public static final String TARGET_PIANO_STUDIO = Esse3HttpClient.AUTH_URI
			+ "auth/studente/Piani/PianiHome.do";
	public static final String TARGET_ISCRIZIONI = SsolHttpClient.AUTH_URI
			+ "main?ent=ieappellics";
	public static final String TARGET_ISCRIZIONI_OLD = SsolHttpClient.AUTH_URI
			+ "main?ent=ieappellics";
	/** SD FILE **/
	public static final File SD_CARD = Environment
			.getExternalStorageDirectory();

	public static boolean deleteFile(String fileName) {
		File file = new File(SD_CARD + "/" + fileName);
		return file.delete();
	}

	public static FileOutputStream createXMLFile(String fileName) {
		File newxmlfile = new File(SD_CARD + "/" + fileName);
		try {
			newxmlfile.createNewFile();
		} catch (IOException e) {
			Log.e(TAG, "IOException in createXMLFile() method");
		}
		// we have to bind the new file with a FileOutputStream
		FileOutputStream fileos = null;
		try {
			fileos = new FileOutputStream(newxmlfile);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "FileNotFoundException can't create FileOutputStream");
		}
		return fileos;
	}

	/*
	 * @return boolean return true if the application can access the internet
	 */
	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		URL url;

		if (connectivity.getActiveNetworkInfo() != null
				&& connectivity.getActiveNetworkInfo().isAvailable()
				&& connectivity.getActiveNetworkInfo().isConnected()) {

			try {
				url = new URL("http://www.google.it");
				HttpURLConnection httpConn = (HttpURLConnection) url
						.openConnection();
				httpConn.setConnectTimeout(5000);
				httpConn.connect();
				Log.i(TAG, "ONLINE!");
				return true;
			} catch (Exception e) {
				Log.i(TAG, "OFFLINE!");
				return false;
			}
		} else {
			Log.i(TAG, "OFFLINE!");
			return false;
		}
	}

	public static Elements jsoupSelect(String page_HTML, String query) {
		Document jsoupDoc = Jsoup.parse(page_HTML);
		return jsoupDoc.select(query);
	}

	// Riceve l'input stream della pagina e lo converte in stringa
	public static String inputStreamToString(InputStream is) {
		String line = "";
		BufferedReader in = null;

		StringBuilder sb = new StringBuilder(line);
		String NL = System.getProperty("line.separator");
		try {
			in = new BufferedReader(new InputStreamReader(is), 8 * 1024);
			while ((line = in.readLine()) != null) {
				sb.append(line).append(NL);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					Log.e(TAG, "Error inputStreamToString(..): La connessione non � stata rilasciata!");
				}
			}
		}

		return sb.toString();
	}

	public static FileInputStream loadXMLFile(String name) {
		File file = new File(SD_CARD, name);
		FileInputStream fileis = null;

		try {
			fileis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			Log.i(TAG, "Il file " + name + " non � stato trovato.");
		}

		return fileis;
	}

	public static KeyStore loadTrustFile() throws KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException {
		File file = new File(SD_CARD, "univr.esse3.cineca.it");
		KeyStore trustStore = KeyStore.getInstance("BKS");

		FileInputStream instream = null;
		try {
			instream = new FileInputStream(file);
			trustStore.load(instream, "changeit".toCharArray());
		} catch (Exception e) {
			Log.i(TAG, "Trustore inesistente.");
			trustStore.load(null);
		} finally {
			if (instream != null) {
				try {
					instream.close();
				} catch (IOException e) {
					Log.e(TAG, "Error loadTrustFile(): Chiusura file non riuscita!");
				}
			}
		}
		return trustStore;
	}

	public static boolean isLink(String link) {
		if(link == null || link.equals("")){
			return false;
		}
		Pattern pattern = Pattern.compile("(http|https)\\://(.*?)");
		Matcher matcher = pattern.matcher(link);
		return matcher.matches();
	}

	public static String removeSpecialString(String page_HTML, String regex) {
		// ELIMINO STRINGHE SPECIALI
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(page_HTML);
		while (matcher.find()) {
			page_HTML = matcher.replaceAll("");
		}
		return page_HTML;
	}

	public static String colorToHex(int r, int g, int b) {
		String colorstr = new String("#");
		// Red
		String str = Integer.toHexString(r);
		if (str.length() > 2) {
			str = str.substring(0, 2);
		} else if (str.length() < 2) {
			colorstr += "0" + str;
		} else {
			colorstr += str;
		}
		// Green
		str = Integer.toHexString(g);
		if (str.length() > 2) {
			str = str.substring(0, 2);
		} else if (str.length() < 2) {
			colorstr += "0" + str;
		} else {
			colorstr += str;
		}
		// Blue
		str = Integer.toHexString(b);
		if (str.length() > 2) {
			str = str.substring(0, 2);
		} else if (str.length() < 2) {
			colorstr += "0" + str;
		} else {
			colorstr += str;
		}
		return colorstr;
	}

	/**
	 * @param numero
	 * @param nCifreDecimali
	 * @return
	 */
	public static double arrotonda(double numero, int nCifreDecimali) {
		return Math.round(numero * Math.pow(10, nCifreDecimali))
				/ Math.pow(10, nCifreDecimali);
	}

	public static Bitmap downloadBitmap(String fileUrl) {
		ConnectionManager cm = ConnectionManager.getInstance();
		InputStream is = null;

		if (isLink(fileUrl)) {
			try {
				cm.getEsse3Connection().get(fileUrl);
				is = cm.getEsse3Connection().getEntity().getContent();
				return BitmapFactory.decodeStream(is);
			} catch (Exception e) {
				Log.e(TAG, "Error downloadBitmap(): Download immagine non riuscito!");
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						Log.e(TAG, "Error downloadBitmap(): Chiusura file non riuscita!");
					}
				}
			}
		}
		return null;
	}
}
