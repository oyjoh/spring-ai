package org.springframework.ai.reader.tika;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.reader.tika.config.RemoteTikaConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TikaRemoteDocumentReaderTests {

	public static GenericContainer<?> tika = new GenericContainer<>(DockerImageName
			.parse("apache/tika:2.9.2.0-full"))
			.waitingFor(Wait.forHttp("/"))
			.withExposedPorts(9998);

	@BeforeAll
	static void beforeAll() {
		tika.setPortBindings(List.of("9998:9998"));
		tika.start();
	}

	@AfterAll
	static void afterAll() {
		tika.stop();
	}

	@ParameterizedTest
	@CsvSource({
			"classpath:/word-sample.docx,word-sample.docx,Two kinds of links are possible, those that refer to an external website",
			"classpath:/word-sample.doc,word-sample.doc,The limited permissions granted above are perpetual and will not be revoked by OASIS",
			"classpath:/sample2.pdf,sample2.pdf,Consult doc/pdftex/manual.pdf from your tetex distribution for more",
			"classpath:/sample.ppt,sample.ppt,Sed ipsum tortor, fringilla a consectetur eget, cursus posuere sem.",
			"classpath:/sample.pptx,sample.pptx,Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
			"https://docs.spring.io/spring-ai/reference/,https://docs.spring.io/spring-ai/reference/,project aims to streamline the development of applications"})
	public void testDocx(String resourceUri, String resourceName, String contentSnipped) {

		var config = new RemoteTikaConfig().setTikaEndpoint("http://localhost:9998");
		var docs = new TikaRemoteDocumentReader(resourceUri, config).get();
		assertThat(docs).hasSize(1);

		var doc = docs.get(0);

		assertThat(doc.getMetadata()).containsKeys(TikaRemoteDocumentReader.METADATA_SOURCE);
		assertThat(doc.getMetadata().get(TikaRemoteDocumentReader.METADATA_SOURCE)).isEqualTo(resourceName);
		assertThat(doc.getContent()).contains(contentSnipped);
	}
}
