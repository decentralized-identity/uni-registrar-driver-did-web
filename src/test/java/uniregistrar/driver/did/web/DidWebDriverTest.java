package uniregistrar.driver.did.web;

import foundation.identity.did.DIDDocument;
import foundation.identity.did.Service;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uniregistrar.RegistrationException;
import uniregistrar.driver.did.web.util.ErrorMessages;
import uniregistrar.request.CreateRequest;
import uniregistrar.request.DeactivateRequest;
import uniregistrar.request.UpdateRequest;
import uniregistrar.state.CreateState;
import uniregistrar.state.UpdateState;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DidWebDriverTest {
	private static final String basePath = "src/test/dids";
	private static final String baseUrl = "https://localhost";
	private static final String testId = "did:web:localhost:testDid42";
	private static Map<String, Object> props;
	private static final String DID_DOC_OP = "setDidDocument";
	private static DidWebDriver driver;
	private static DIDDocument testDoc;
	private static Path absoluteBasePath;

	@BeforeAll
	static void init() throws IOException {
		String workingPath = Path.of("").toAbsolutePath().toString();
		absoluteBasePath = Path.of(workingPath, basePath);


		Files.createDirectories(absoluteBasePath);


		props = new HashMap<>();
		props.put("baseUrl", baseUrl);
		props.put("basePath", absoluteBasePath.toString());
		props.put("generatedFolder", "generated");

		driver = new DidWebDriver(props);

		testDoc = DIDDocument.fromJson("{\"@context\":\"https://www.w3.org/2019/did/v1\"}");

	}

	@BeforeEach
	void cleanDids() throws IOException {
		if (Files.exists(absoluteBasePath)) {
			FileUtils.cleanDirectory(absoluteBasePath.toFile());
		}
	}

	@Test
	@DisplayName("register: with empty document")
	void registerWithEmptyDocTest() throws RegistrationException {

		CreateRequest request = new CreateRequest();
		request.setDidDocument(new DIDDocument());

		CreateState state = driver.create(request);
		assertNotNull(state);

		DIDDocument rd = (DIDDocument) state.getDidState().get("didDocument");
		assertNotNull(rd.getId());
	}

	@Test
	@DisplayName("register: without doc throws error")
	void registerWithoutDocTest() {
		assertThrows(RegistrationException.class,
					 () -> driver.create(new CreateRequest()),
					 ErrorMessages.DID_DOC_IS_NULL);


	}

	@Test
	@DisplayName("register: with given document, no identifier")
	void registerWithGivenDocTest() throws RegistrationException {

		CreateRequest request = new CreateRequest();
		request.setDidDocument(testDoc);


		CreateState state = driver.create(request);
		assertNotNull(state);

		DIDDocument rd = (DIDDocument) state.getDidState().get("didDocument");
		assertNotNull(rd.getId());
		assertNotNull(rd.getContexts());
	}

	@Test
	@DisplayName("register: with existing identifier")
	void registerWithExistingIdTest() throws RegistrationException {

		CreateRequest request = new CreateRequest();
		testDoc.setJsonObjectKeyValue("id", testId);
		request.setDidDocument(testDoc);

		CreateState state = driver.create(request);
		assertNotNull(state);

		DIDDocument rd = (DIDDocument) state.getDidState().get("didDocument");
		assertEquals(testId, rd.getId().toString());
		assertThrows(RegistrationException.class,
					 () -> driver.create(request),
					 ErrorMessages.DID_ALREADY_EXISTS);
	}

	@Test
	@DisplayName("update: after registering with given id")
	void update() throws RegistrationException {

		DIDDocument createdDoc = registerWithGivenIdTest();

		Service service = Service.builder()
								 .id(URI.create("randId"))
								 .serviceEndpoint("randEp")
								 .build();

		DIDDocument updateDoc = DIDDocument.builder()
										   .id(createdDoc.getId())
										   .service(service)
										   .build();

		UpdateRequest request = new UpdateRequest();
		request.setDid(createdDoc.getId().toString());
		request.setDidDocument(List.of(updateDoc));
		request.setDidDocumentOperation(List.of(DID_DOC_OP));
		UpdateState state = driver.update(request);
		DIDDocument rd = (DIDDocument) state.getDidState().get("didDocument");

		assertEquals(testId, rd.getId().toString());
		assertNotNull(rd.getServices());

	}

	@Test
	@DisplayName("update: without doc throws error")
	void updateWithoutDocTest() {
		assertThrows(RegistrationException.class,
					 () -> driver.update(new UpdateRequest()),
					 ErrorMessages.DID_DOC_IS_NULL);
	}

	@Test
	@DisplayName("update: with multiple docs throws error")
	void updateWithMultipleDocsTest() {
		UpdateRequest request = new UpdateRequest();
		testDoc.setJsonObjectKeyValue("id", testId);
		request.setDidDocument(List.of(testDoc, testDoc));

		assertThrows(RegistrationException.class,
				() -> driver.update(new UpdateRequest()),
				ErrorMessages.MULTIPLE_DID_DOCS_IN_REQUEST);
	}

	@Test
	@DisplayName("update: with multiple didDocumentOperation throws error")
	void updateWithMultipleDocOpTest() {
		UpdateRequest request = new UpdateRequest();
		testDoc.setJsonObjectKeyValue("id", testId);
		request.setDidDocument(List.of(testDoc));
		request.setDidDocumentOperation(List.of(DID_DOC_OP,DID_DOC_OP));

		assertThrows(RegistrationException.class,
				() -> driver.update(new UpdateRequest()),
				ErrorMessages.DID_DOC_OP_IS_INVALID);
	}

	@Test
	@DisplayName("update: with invalid didDocumentOperation throws error")
	void updateWithInvalidDocOpTest() {
		UpdateRequest request = new UpdateRequest();
		testDoc.setJsonObjectKeyValue("id", testId);
		request.setDidDocument(List.of(testDoc));
		request.setDidDocumentOperation(List.of("yo"));

		assertThrows(RegistrationException.class,
				() -> driver.update(new UpdateRequest()),
				ErrorMessages.DID_DOC_OP_IS_INVALID);
	}

	@Test
	@DisplayName("update: without DID throws error")
	void updateWithoutDidTest() {

		UpdateRequest request = new UpdateRequest();
		request.setDidDocument(List.of(testDoc));

		assertThrows(RegistrationException.class,
					 () -> driver.update(new UpdateRequest()),
					 ErrorMessages.DID_IS_NULL);

	}

	@Test
	@DisplayName("update: with non-registered DID throws error")
	void updateWithNonRegisteredDidTest() {

		UpdateRequest request = new UpdateRequest();
		testDoc.setJsonObjectKeyValue("id", testId);
		request.setDidDocument(List.of(testDoc));

		assertThrows(RegistrationException.class,
					 () -> driver.update(request),
					 ErrorMessages.DID_DOESNT_EXIST);
	}


	@Test
	@DisplayName("deactivate: without identifier throws error")
	void deactivateWithoutIdTest() {

		assertThrows(RegistrationException.class,
					 () -> driver.deactivate(new DeactivateRequest()),
					 ErrorMessages.DID_IS_NULL);
	}

	@Test
	@DisplayName("deactivate: with non-registered DID throws error")
	void deactivateWithNonRegisteredDidTest() {

		DeactivateRequest request = new DeactivateRequest();
		request.setDid(testId);

		assertThrows(RegistrationException.class,
					 () -> driver.deactivate(request),
					 ErrorMessages.DID_DOESNT_EXIST);
	}

	private DIDDocument registerWithGivenIdTest() throws RegistrationException {

		CreateRequest request = new CreateRequest();
		testDoc.setJsonObjectKeyValue("id", testId);
		request.setDidDocument(testDoc);

		CreateState state = driver.create(request);
		assertNotNull(state);

		DIDDocument rd = (DIDDocument) state.getDidState().get("didDocument");
		assertEquals(testId, rd.getId().toString());

		return rd;
	}


}
