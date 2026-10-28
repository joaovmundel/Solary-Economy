package com.redeskyller.bukkit.solaryeconomy.update;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Asks GitHub for the latest release once and logs the result. Runs on an async task: it uses
 * timeouts so a dead network cannot hang a worker, never throws, and reports a failure as a single
 * warning line (a stack trace for "no internet" helps nobody). Nothing is downloaded or installed.
 */
public final class UpdateChecker implements Runnable {

	private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/sredition/Solary-Economy/releases/latest";
	private static final int TIMEOUT_MS = 5000;
	private static final int MAX_RESPONSE_BYTES = 1024 * 1024;
	private static final Pattern TAG_NAME = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern HTML_URL = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");

	private final String currentVersion;
	private final Logger logger;

	public UpdateChecker(String currentVersion, Logger logger) {
		this.currentVersion = currentVersion;
		this.logger = logger;
	}

	@Override
	public void run() {
		try {
			String json = fetch();
			Matcher tag = TAG_NAME.matcher(json);
			if (!tag.find()) {
				this.logger.warning("Verificação de atualização: resposta inesperada do GitHub.");
				return;
			}

			String latest = tag.group(1);
			if (isNewer(latest, this.currentVersion)) {
				Matcher link = HTML_URL.matcher(json);
				this.logger.warning("Há uma versão nova do Solary-Economy: " + latest + " (a sua é "
						+ this.currentVersion + ")." + (link.find() ? " Baixe em: " + link.group(1) : ""));
			} else {
				this.logger.info("Você está na versão mais recente do plugin (" + this.currentVersion + ").");
			}
		} catch (IOException | RuntimeException exception) {
			this.logger.warning("Não foi possível verificar atualizações: " + exception);
		}
	}

	private String fetch() throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_RELEASE_URL).openConnection();
		try {
			connection.setConnectTimeout(TIMEOUT_MS);
			connection.setReadTimeout(TIMEOUT_MS);
			connection.setRequestProperty("User-Agent", "Solary-Economy/" + this.currentVersion);
			connection.setRequestProperty("Accept", "application/vnd.github+json");

			int status = connection.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK)
				throw new IOException("GitHub respondeu HTTP " + status);

			try (InputStream stream = connection.getInputStream()) {
				ByteArrayOutputStream body = new ByteArrayOutputStream();
				byte[] chunk = new byte[4096];
				int read;
				while ((read = stream.read(chunk)) != -1) {
					body.write(chunk, 0, read);
					if (body.size() > MAX_RESPONSE_BYTES)
						throw new IOException("resposta grande demais");
				}
				return new String(body.toByteArray(), Charset.forName("UTF-8"));
			}
		} finally {
			connection.disconnect();
		}
	}

	/** Compares dotted numeric versions ("1.10.0" &gt; "1.9"), ignoring a leading "v" and any "-suffix". */
	static boolean isNewer(String candidate, String current) {
		int[] a = parse(candidate);
		int[] b = parse(current);
		if (a == null || b == null)
			return false;

		for (int i = 0; i < Math.max(a.length, b.length); i++) {
			int left = i < a.length ? a[i] : 0;
			int right = i < b.length ? b[i] : 0;
			if (left != right)
				return left > right;
		}
		return false;
	}

	private static int[] parse(String version) {
		String core = version.trim();
		if (core.startsWith("v") || core.startsWith("V"))
			core = core.substring(1);
		int dash = core.indexOf('-');
		if (dash >= 0)
			core = core.substring(0, dash);

		String[] parts = core.split("\\.");
		int[] numbers = new int[parts.length];
		try {
			for (int i = 0; i < parts.length; i++)
				numbers[i] = Integer.parseInt(parts[i]);
		} catch (NumberFormatException exception) {
			return null;
		}
		return numbers;
	}
}
