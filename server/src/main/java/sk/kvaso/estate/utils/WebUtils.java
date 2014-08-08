package sk.kvaso.estate.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;

public class WebUtils {
	public static byte[] downloadURI(final String uri) throws IOException, URISyntaxException {
		System.out.println("Downloading [" + uri + "]");
		return IOUtils.toByteArray(new URI(uri));
	}
}
