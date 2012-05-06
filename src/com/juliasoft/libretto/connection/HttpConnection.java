package com.juliasoft.libretto.connection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.util.Log;

public class HttpConnection implements Runnable {

	private static final boolean DEBUG = true;
	private static final String TAG = HttpConnection.class.getName();

	public static final int GET = 0;
	public static final int POST = 1;

	public static final String CLIENT_PROTOCOL_EXCEPTION = "Errore dovuto al protocollo HTTP";
	public static final String IO_EXCEPTION = "Connessione interrotta";
	public static final String CONNECT_EXCEPTION = "Errore durante la connessione";
	public static final String HTTP_BAD_METHOD_EXCEPTION = "Metodo sconosciuto; usare GET o POST";
	public static final String HTTP_INTERNAL_ERROR_EXCEPTION = "Errore interno al server";
	public static final String HTTP_NOT_FOUND_EXCEPTION = "Pagina non trovata";
	public static final String HTTP_UNAUTHORIZED_EXCEPTION = "Il Nome Utente o la Password inserita non sono corretti";
	public static final String HTTP_UNAVAILABLE_EXCEPTION = "Server momentaneamente non disponibile";
	public static final String HTTP_DEFAULT_EXCEPTION = "Errore durante la connessione";

	private String url;
	private int method;

	private Map<String, String> params;
	private Map<String, String> headers;

	private final DefaultHttpClient httpClient;
	private HttpResponse response;

	public HttpConnection(DefaultHttpClient httpClient) {
		this.httpClient = httpClient;
	}

	public void create(int method, String url, Map<String, String> headers,
			Map<String, String> params) {
		this.method = method;
		this.url = url;
		this.headers = headers;
		this.params = params;
		response = null;
		run();
	}

	public void get(String url) {
		create(GET, url, null, null);
	}

	public void post(String url, Map<String, String> params) {
		create(POST, url, null, params);
	}

	@Override
	public void run() {
		try {
			switch (method) {
			case POST:
				HttpPost post = new HttpPost(url);
				setHeaders(post);
				setParams(post);
				response = httpClient.execute(post);
				break;
			case GET:
				HttpGet get = new HttpGet(url);
				setHeaders(get);
				response = httpClient.execute(get);
				break;
			default:
				throw new AssertionError("");
			}
		} catch (ClientProtocolException e) {
			if(DEBUG)
				Log.e(TAG, CLIENT_PROTOCOL_EXCEPTION + ": " + e.getMessage());
		} catch (IOException e) {
			if(DEBUG)
				Log.e(TAG, IO_EXCEPTION + ": " + e.getMessage());
		} catch (Exception e) {
			if(DEBUG)
				Log.e(TAG, "Connection error: " + e.getMessage());
		}
	}

	private void setHeaders(HttpUriRequest uriRq) {
		if (headers != null && !headers.isEmpty())
			for (Map.Entry<String, String> entry : headers.entrySet())
				uriRq.addHeader(entry.getKey(), entry.getValue());
	}

	private void setParams(HttpPost post) throws UnsupportedEncodingException {
		if (params != null && !params.isEmpty()) {
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();

			for (Map.Entry<String, String> entry : params.entrySet())
				if (entry.getValue() != null && entry.getValue() != "")
					postParams.add(new BasicNameValuePair(entry.getKey(), entry
							.getValue()));

			post.setEntity(new UrlEncodedFormEntity(postParams, HTTP.UTF_8));
		}
	}

	public List<Cookie> getCookies() {
		return httpClient.getCookieStore().getCookies();
	}

	public void consumeContent() {
		if (response != null)
			try {
				response.getEntity().consumeContent();
			} catch (IOException e) {
				Log.e(TAG, "Error consumeContent()");
			}
	}

	public void reset() {
		if (httpClient != null)
			httpClient.getConnectionManager().shutdown();
		response = null;
	}

	public int getStatusCode() {
		return response == null ? -1 : response.getStatusLine().getStatusCode();
	}

	public HttpEntity getEntity() throws LoginException, ConnectException {
		if (response == null)
			throw new ConnectException(CONNECT_EXCEPTION);

		int status_code = getStatusCode();
		switch (status_code) {
		case HttpURLConnection.HTTP_OK:
			return response.getEntity();
		case HttpURLConnection.HTTP_BAD_METHOD:
			consumeContent();
			throw new ConnectException(HTTP_BAD_METHOD_EXCEPTION);
		case HttpURLConnection.HTTP_INTERNAL_ERROR:
			consumeContent();
			throw new ConnectException(HTTP_INTERNAL_ERROR_EXCEPTION);
		case HttpURLConnection.HTTP_NOT_FOUND:
			consumeContent();
			throw new ConnectException(HTTP_NOT_FOUND_EXCEPTION);
		case HttpURLConnection.HTTP_UNAUTHORIZED:
			consumeContent();
			throw new LoginException(HTTP_UNAUTHORIZED_EXCEPTION);
		case HttpURLConnection.HTTP_UNAVAILABLE:
			consumeContent();
			throw new ConnectException(HTTP_UNAVAILABLE_EXCEPTION);
		default:
			consumeContent();
			throw new ConnectException(HTTP_DEFAULT_EXCEPTION
					+ "\nStatus code: " + status_code);
		}
	}
}