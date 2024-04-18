package org.springframework.ai.reader.tika;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.tika.config.RemoteTikaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;

public class TikaRemoteDocumentReader implements DocumentReader {

	/**
	 * Metadata key representing the source of the document.
	 */
	public static final String METADATA_SOURCE = "source";

	/**
	 * The resource pointing to the document.
	 */
	private final Resource resource;

	/**
	 * Formatter for the extracted text.
	 */
	private final ExtractedTextFormatter textFormatter;

	/**
	 * Rest client for connecting to the remote tika server
	 */
	private final RestClient restClient;


	private RemoteTikaConfig config;

	public TikaRemoteDocumentReader(Resource resource, RemoteTikaConfig config) {
		this(resource, ExtractedTextFormatter.defaults(), config);
	}

	public TikaRemoteDocumentReader(String resourceUrl, RemoteTikaConfig config) {
		this(new DefaultResourceLoader().getResource(resourceUrl), ExtractedTextFormatter.defaults(), config);
	}

	public TikaRemoteDocumentReader(Resource resource, ExtractedTextFormatter textFormatter, RemoteTikaConfig config) {
		this.resource = resource;
		this.textFormatter = textFormatter;
		this.config = config;

		this.restClient = RestClient.builder().baseUrl(config.getTikaEndpoint().toString()).build();
	}

	@Override
	public List<Document> get() {
		var result = putRequest();
		return List.of(toDocument(result));
	}

	private String putRequest() {
		try (var inputStream = this.resource.getInputStream()) {
			return restClient
					.put()
					.uri("/tika")
					.accept(MediaType.TEXT_PLAIN)
					.body(inputStream.readAllBytes())
					.retrieve()
					.body(String.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * Converts the given text to a {@link Document}.
	 * @param docText Text to be converted
	 * @return Converted document
	 */
	private Document toDocument(String docText) {
		docText = Objects.requireNonNullElse(docText, "");
		docText = this.textFormatter.format(docText);
		Document doc = new Document(docText);
		doc.getMetadata().put(METADATA_SOURCE, resourceName());
		return doc;
	}

	/**
	 * Returns the name of the resource. If the filename is not present, it returns the
	 * URI of the resource.
	 * @return Name or URI of the resource
	 */
	private String resourceName() {
		try {
			var resourceName = this.resource.getFilename();
			if (!StringUtils.hasText(resourceName)) {
				resourceName = this.resource.getURI().toString();
			}
			return resourceName;
		}
		catch (IOException e) {
			return String.format("Invalid source URI: %s", e.getMessage());
		}
	}

}
