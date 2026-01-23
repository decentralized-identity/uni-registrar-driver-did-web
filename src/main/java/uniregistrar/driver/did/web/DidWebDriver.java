package uniregistrar.driver.did.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import foundation.identity.did.DIDDocument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modelmapper.ModelMapper;
import uniregistrar.RegistrationException;
import uniregistrar.driver.AbstractDriver;
import uniregistrar.driver.did.web.util.ErrorMessages;
import uniregistrar.openapi.model.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class DidWebDriver extends AbstractDriver {

	private static final Logger log = LogManager.getLogger(DidWebDriver.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static final String METHOD_PREFIX = "did:web:";
	public static final String FILE_NAME = "/did.json";
	private String generatedFolder;
	private List<URL> baseUrls;
	private Path basePath;
	private Map<String, Object> properties;

	public DidWebDriver() {
		this(getPropertiesFromEnvironment());
	}

	public DidWebDriver(Map<String, Object> properties) {
		setProperties(properties);

		log.debug("Driver properties set with: {}", () -> properties.keySet().stream()
																	.map(key -> key + "=" + properties.get(key))
																	.collect(Collectors.joining(", ", "{", "}")));

/*		if (basePath == null) throw new IllegalArgumentException(ErrorMessages.BASE_PATH_NOT_DEFINED);
		if (baseUrls == null) throw new IllegalArgumentException(ErrorMessages.BASE_URL_NOT_DEFINED);
		for (URL baseUrl : baseUrls) {
			if (!"https".equals(baseUrl.getProtocol())) throw new IllegalArgumentException(ErrorMessages.WRONG_URL_PROTOCOL + ": (" + baseUrl + ")");
		}

		if (!Files.exists(basePath)) {
			log.warn(ErrorMessages.BASE_PATH_DOES_NOT_EXIST + " Base path: " + basePath);
			log.info("Trying to generate base path directories!");
			try {
				Files.createDirectories(basePath);
			} catch (IOException e) {
				throw new RuntimeException("Cannot create directories for base path: " + basePath, e);
			}
		}
		if (!Files.isDirectory(basePath)) throw new IllegalArgumentException(ErrorMessages.BASE_PATH_NOT_DIRECTORY + " Base path: " + basePath);
		if (!Files.isWritable(basePath)) throw new IllegalArgumentException(ErrorMessages.BASE_PATH_NOT_WRITEABLE + " Base path: " + basePath);*/
	}

	private static Map<String, Object> getPropertiesFromEnvironment() {

		log.debug("Loading from environment: {}", System::getenv);

		Map<String, Object> properties = new HashMap<>();

		String env_baseUrl = System.getenv("uniregistrar_driver_did_web_baseUrl");
		String env_basePath = System.getenv("uniregistrar_driver_did_web_basePath");
		String env_generatedFolder = System.getenv("uniregistrar_driver_did_web_generatedFolder");

		if (!Strings.isNullOrEmpty(env_baseUrl)) properties.put("baseUrl", env_baseUrl);
		if (!Strings.isNullOrEmpty(env_basePath)) properties.put("basePath", env_basePath);
		if (!Strings.isNullOrEmpty(env_generatedFolder)) properties.put("generatedFolder", env_generatedFolder);

		return properties;
	}

	@Override
	public CreateState create(CreateRequest request) throws RegistrationException {
		Preconditions.checkNotNull(request);

		log.debug("Create request for: {}", () -> request);

        DIDDocument didDocument = null;
        try {
            didDocument = request.getDidDocument() == null ? null : DIDDocument.fromJson(objectMapper.writeValueAsString(request.getDidDocument()));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        if (didDocument == null) throw new RegistrationException(ErrorMessages.DID_DOC_IS_NULL);

		RequestOptions options = request.getOptions();

		// Use generated id to build DID when given document doesn't contain an ID

		Path didPath;

		if (didDocument.getId() == null) {

			String host = options == null ? null : (String) options.getAdditionalProperty("host");
			if (host == null) host = baseUrls.get(0).getHost();

			UUID uuid = UUID.randomUUID();
			didPath = generateNewPath(uuid);
			String did = validatedAndGenerateNewDid(uuid, host);
			if (log.isInfoEnabled()) log.info("Generated UUID " + uuid + " and new path " + didPath + " and DID: " + did);

			didDocument.getJsonObject().put("id", did);
		} else {

			didPath = validateAndGetPath(didDocument.getId().toString());
			if (log.isInfoEnabled()) log.info("Validated path: " + didPath);
		}

		// Checks if given DID already exist
		if (Files.exists(didPath)) throw new RegistrationException(ErrorMessages.DID_ALREADY_EXISTS);

		try {
			storeDidDocument(didPath, didDocument);
		} catch (IOException e) {
			log.error(e);
			throw new RegistrationException("Cannot store the DID document for " + didDocument.getId().toString());
		}

		DidStateFinished didStateFinished = new DidStateFinished();
		didStateFinished.setState("finished");
		didStateFinished.setDid(didDocument.getId().toString());
		didStateFinished.setDidDocument(objectMapper.convertValue(didDocument, DidDocument.class));

		CreateState createState = new CreateState();
		createState.setDidState(didStateFinished);

		log.debug("Registration is finished: {}", createState);

		return createState;
	}

	@Override
	public UpdateState update(UpdateRequest request) throws RegistrationException {
		validateUpdateRequest(request);

        DIDDocument didDocument = null;
        try {
            didDocument = DIDDocument.fromJson(objectMapper.writeValueAsString(request.getDidDocument().get(request.getDidDocument().size()-1)));
        } catch (JsonProcessingException ex) {
            throw new RegistrationException(ex.getMessage(), ex);
        }

        if (request.getDid() == null) throw new RegistrationException(ErrorMessages.DID_IS_NULL);

		Path didPath = validateAndGetPath(request.getDid());
		if (!Files.exists(didPath)) throw new RegistrationException(ErrorMessages.DID_DOESNT_EXIST);

		Path didDocFile = Paths.get(didPath.toString());

		try {
			Files.delete(Paths.get(didDocFile.toString(), FILE_NAME));
			storeDidDocument(didDocFile, didDocument);
		} catch (IOException e) {
			log.error(e);
			throw new RegistrationException("Cannot store the DID document for " + didDocument.getId());
		}

		DidStateFinished didStateFinished = new DidStateFinished();
		didStateFinished.setState("finished");
		didStateFinished.setDid(didDocument.getId().toString());
		didStateFinished.setDidDocument(objectMapper.convertValue(didDocument, DidDocument.class));

		UpdateState updateState = new UpdateState();
		updateState.setDidState(didStateFinished);

		return updateState;
	}

	private static void validateUpdateRequest(UpdateRequest request) throws RegistrationException {
		Preconditions.checkNotNull(request);

		if(request.getDidDocumentOperation() == null) request.setDidDocumentOperation(List.of("setDidDocument"));
		for (String didDocumentOperation : request.getDidDocumentOperation()) {
			if(!"setDidDocument".equals(didDocumentOperation)) throw new RegistrationException(ErrorMessages.DID_DOC_OP_IS_INVALID);
		}

		if (request.getDidDocument() == null || request.getDidDocument().isEmpty() || request.getDidDocument().get(request.getDidDocument().size()-1) == null) throw new RegistrationException(ErrorMessages.DID_DOC_IS_NULL);
		if (request.getDidDocument().size() > request.getDidDocumentOperation().size()) throw new RegistrationException(ErrorMessages.DID_DOC_OP_IS_NULL);
		if (request.getDidDocument().size() < request.getDidDocumentOperation().size()) throw new RegistrationException(ErrorMessages.DID_DOC_IS_NULL);
	}

	@Override
	public DeactivateState deactivate(DeactivateRequest request) throws RegistrationException {
		Preconditions.checkNotNull(request);
		if (request.getDid() == null) throw new RegistrationException(ErrorMessages.DID_IS_NULL);

		Path didPath = validateAndGetPath(request.getDid());
		if (!Files.exists(didPath)) throw new RegistrationException(ErrorMessages.DID_DOESNT_EXIST);

		Path didDocFile = Paths.get(didPath.toString(), FILE_NAME);

		try {
			Files.delete(didDocFile);
		} catch (IOException e) {
			throw new RegistrationException(e.getMessage());
		}

		DidStateFinished didStateFinished = new DidStateFinished();
		didStateFinished.setState("finished");

		DeactivateState deactivateState = new DeactivateState();
		deactivateState.setDidState(didStateFinished);


		return deactivateState;

	}

	@Override
	public Map<String, Object> properties() {
		return Collections.unmodifiableMap(properties);
	}

	private Path validateAndGetPath(String did) throws RegistrationException {
		if (did == null) throw new RegistrationException(ErrorMessages.DID_IS_NULL);
		if (!did.startsWith(METHOD_PREFIX)) throw new RegistrationException(ErrorMessages.METHOD_PREFIX_MISMATCH);

		String[] parsed = did.substring(DidWebDriver.METHOD_PREFIX.length()).split(":");
		if (parsed.length < 2) throw new RegistrationException(ErrorMessages.DID_FORMAT_ERROR);
		String host = parsed[0];

		if (baseUrls.stream().noneMatch(x -> x.getHost().equals(host))) throw new RegistrationException(ErrorMessages.WRONG_HOST);

		return Paths.get(basePath.toString(), Arrays.stream(parsed)
													.skip(1)
													.map(x -> x + "/")
													.reduce("/", String::concat));
	}

	private Path generateNewPath(UUID uuid) {
		if (Strings.isNullOrEmpty(generatedFolder)) {
			return Paths.get(basePath.toString(), uuid.toString());
		}
		else
			return Paths.get(basePath.toString(), generatedFolder, uuid.toString());
	}

	private String validatedAndGenerateNewDid(UUID uuid, String host) throws RegistrationException {
		if (baseUrls.stream().noneMatch(x -> x.getHost().equals(host))) throw new RegistrationException(ErrorMessages.WRONG_HOST);

		return METHOD_PREFIX + host + ":" + (Strings.isNullOrEmpty(generatedFolder) ? uuid : generatedFolder + ":" + uuid);
	}

	public static void storeDidDocument(Path filePath, DIDDocument document) throws IOException {

		Files.createDirectories(filePath);

		try (Writer fileWriter = new OutputStreamWriter(new FileOutputStream(filePath + FILE_NAME), StandardCharsets.UTF_8)) {
			fileWriter.write(document.toJson());
			fileWriter.flush();
		}
	}

	private Map<String, Object> getProperties() {
		return properties;
	}

	/**
	 * Make base path changeable to avoid file system challenges
	 * @param basePath
	 */
	public void setBasePath(Path basePath) {
		this.basePath = basePath;
	}

	public final void setProperties(Map<String, Object> properties) {
		Preconditions.checkState(this.properties == null, ErrorMessages.PROPERTIES_ALREADY_SET);
		this.properties = Map.copyOf(properties);
		configureFromProperties();
	}

	private void configureFromProperties() {

		log.debug("Configuring from properties: {}", this::getProperties);
		String prop_baseUrl = (String) properties.get("baseUrl");
		String prop_basePath = (String) properties.get("basePath");
		String prop_generatedFolder = (String) properties.get("generatedFolder");

		if (!Strings.isNullOrEmpty(prop_baseUrl)) this.baseUrls = Arrays.stream(prop_baseUrl.trim().split(",")).map(DidWebDriver::newURL).collect(Collectors.toList());
		if (!Strings.isNullOrEmpty(prop_basePath)) this.basePath = Paths.get(prop_basePath.trim());
		if (!Strings.isNullOrEmpty(prop_generatedFolder)) this.generatedFolder = prop_generatedFolder.trim();
	}

	private static URL newURL(String urlString) {
		try {
			return new URL(urlString);
		} catch (MalformedURLException ex) {
			throw new IllegalArgumentException(ex.getMessage());
		}
	}
}
