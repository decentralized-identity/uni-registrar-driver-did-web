package uniregistrar.driver.did.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.identity.did.DIDDocument;
import foundation.identity.did.Service;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uniregistrar.RegistrationException;
import uniregistrar.driver.did.web.util.ErrorMessages;
import uniregistrar.openapi.model.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DidWebDriverTest {

	private static final ObjectMapper objectMapper = new ObjectMapper();

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
	void registerWithEmptyDocTest() throws RegistrationException, JsonProcessingException {

		CreateRequest request = new CreateRequest();
		request.setDidDocument(new DidDocument());

		CreateState state = driver.create(request);
		assertNotNull(state);

		DIDDocument rd = DIDDocument.fromJson(objectMapper.writeValueAsString(state.getDidState().getDidDocument()));
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
	void registerWithGivenDocTest() throws RegistrationException, JsonProcessingException {

		DidDocument testDoc = new DidDocument();
		testDoc.putAdditionalProperty("@context", "https://www.w3.org/2019/did/v1");

		CreateRequest request = new CreateRequest();
		request.setDidDocument(testDoc);

		CreateState state = driver.create(request);
		assertNotNull(state);

		DIDDocument rd = DIDDocument.fromJson(objectMapper.writeValueAsString(state.getDidState().getDidDocument()));
		assertNotNull(rd.getId());
		assertNotNull(rd.getContexts());
	}

	@Test
	@DisplayName("register: with existing identifier")
	void registerWithExistingIdTest() throws RegistrationException, JsonProcessingException {

		DidDocument testDoc = new DidDocument();
		testDoc.putAdditionalProperty("@context", "https://www.w3.org/2019/did/v1");
		testDoc.setId(testId);

		CreateRequest request = new CreateRequest();
		request.setDidDocument(testDoc);

		CreateState state = driver.create(request);
		assertNotNull(state);

		DIDDocument rd = DIDDocument.fromJson(objectMapper.writeValueAsString(state.getDidState().getDidDocument()));
		assertEquals(testId, rd.getId().toString());
		assertThrows(RegistrationException.class,
					 () -> driver.create(request),
					 ErrorMessages.DID_ALREADY_EXISTS);
	}

	@Test
	@DisplayName("update: after registering with given id")
	void update() throws RegistrationException, JsonProcessingException {

		DIDDocument createdDoc = registerWithGivenIdTest();

		Service service = Service.builder()
								 .id(URI.create("randId"))
								 .serviceEndpoint(URI.create("randEp"))
								 .build();

		DIDDocument updateDoc = DIDDocument.builder()
										   .id(createdDoc.getId())
										   .service(service)
										   .build();

		UpdateRequest request = new UpdateRequest();
		request.setDid(createdDoc.getId().toString());
		request.setDidDocument(List.of(objectMapper.convertValue(updateDoc, DidDocument.class)));
		request.setDidDocumentOperation(List.of(DID_DOC_OP));
		UpdateState state = driver.update(request);
		DIDDocument rd = DIDDocument.fromJson(objectMapper.writeValueAsString(state.getDidState().getDidDocument()));

		assertEquals(testId, rd.getId().toString());
		assertNotNull(rd.getServices());

	}

	@Test
	@DisplayName("update: without doc throws error")
	void updateWithoutDocTest() {
		Exception ex = assertThrows(RegistrationException.class, () -> driver.update(new UpdateRequest()));
		assertEquals(ErrorMessages.DID_DOC_IS_NULL, ex.getMessage());
	}

	@Test
	@DisplayName("update: without DID throws error")
	void updateWithoutDid() {

		DidDocument testDoc = new DidDocument();
		testDoc.putAdditionalProperty("@context", "https://www.w3.org/2019/did/v1");
		testDoc.setId(testId);

		UpdateRequest request = new UpdateRequest();
		request.setDidDocument(List.of(testDoc, testDoc));

		Exception ex = assertThrows(RegistrationException.class, () -> driver.update(request));
		assertEquals(ErrorMessages.DID_DOC_OP_IS_NULL, ex.getMessage());
	}

	@Test
	@DisplayName("update: with multiple didDocumentOperation throws error")
	void updateWithMultipleDocOpTest() throws RegistrationException {

		DidDocument testDoc = new DidDocument();
		testDoc.putAdditionalProperty("@context", "https://www.w3.org/2019/did/v1");

		CreateRequest request = new CreateRequest();
		request.setDidDocument(testDoc);

		CreateState createState = driver.create(request);
		String testId = createState.getDidState().getDid();

		DidDocument testDoc2 = new DidDocument();
		testDoc2.putAdditionalProperty("@context", "https://www.w3.org/2019/did/v1");
		testDoc2.setId(testId);

		UpdateRequest request2 = new UpdateRequest();
		request2.setDid(testId);
		request2.setDidDocument(List.of(testDoc2));
		request2.setDidDocumentOperation(List.of(DID_DOC_OP,DID_DOC_OP));

		Exception ex = assertThrows(RegistrationException.class, () -> driver.update(request2));
		assertEquals(ErrorMessages.DID_DOC_IS_NULL, ex.getMessage());
	}

	@Test
	@DisplayName("update: with invalid didDocumentOperation throws error")
	void updateWithInvalidDocOpTest() {

		DidDocument testDoc = new DidDocument();
		testDoc.putAdditionalProperty("@context", "https://www.w3.org/2019/did/v1");
		testDoc.setId(testId);

		UpdateRequest request = new UpdateRequest();
		request.setDidDocument(List.of(testDoc));
		request.setDidDocumentOperation(List.of("yo"));

		Exception ex = assertThrows(RegistrationException.class, () -> driver.update(request));
		assertEquals(ErrorMessages.DID_DOC_OP_IS_INVALID, ex.getMessage());
	}

	@Test
	@DisplayName("update: without DID throws error")
	void updateWithoutDidTest() {

		UpdateRequest request = new UpdateRequest();
		request.setDidDocument(List.of(objectMapper.convertValue(testDoc, DidDocument.class)));

		Exception ex = assertThrows(RegistrationException.class, () -> driver.update(request));
		assertEquals(ErrorMessages.DID_IS_NULL, ex.getMessage());
	}

	@Test
	@DisplayName("update: with non-registered DID throws error")
	void updateWithNonRegisteredDidTest() {

		DidDocument testDoc = new DidDocument();
		testDoc.putAdditionalProperty("@context", "https://www.w3.org/2019/did/v1");
		testDoc.setId(testId);

		UpdateRequest request = new UpdateRequest();
		request.setDid(testId);
		request.setDidDocument(List.of(testDoc));

		Exception ex = assertThrows(RegistrationException.class, () -> driver.update(request));
		assertEquals(ErrorMessages.DID_DOESNT_EXIST, ex.getMessage());
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

	private DIDDocument registerWithGivenIdTest() throws RegistrationException, JsonProcessingException {

		DidDocument testDoc = new DidDocument();
		testDoc.putAdditionalProperty("@context", "https://www.w3.org/2019/did/v1");
		testDoc.setId(testId);

		CreateRequest request = new CreateRequest();
		request.setDidDocument(testDoc);

		CreateState state = driver.create(request);
		assertNotNull(state);

		DIDDocument rd = DIDDocument.fromJson(objectMapper.writeValueAsString(state.getDidState().getDidDocument()));
		assertEquals(testId, rd.getId().toString());

		return rd;
	}


}
