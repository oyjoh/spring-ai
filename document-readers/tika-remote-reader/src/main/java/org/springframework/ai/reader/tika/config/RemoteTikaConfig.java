package org.springframework.ai.reader.tika.config;


import java.net.URI;

public class RemoteTikaConfig {
	private URI tikaEndpoint;

	public RemoteTikaConfig setTikaEndpoint(URI tikaEndpoint) {
		this.tikaEndpoint = tikaEndpoint;
		return this;
	}

	public RemoteTikaConfig setTikaEndpoint(String tikaEndpoint) {
		this.tikaEndpoint = URI.create(tikaEndpoint);
		return this;
	}

	public URI getTikaEndpoint() {
		return tikaEndpoint;
	}
}