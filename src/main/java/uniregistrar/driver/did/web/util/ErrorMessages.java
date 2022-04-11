package uniregistrar.driver.did.web.util;

public class ErrorMessages {

	public static final String DID_ALREADY_EXISTS = "Given DID already exists!";
	public static final String DID_DOC_IS_NULL = "DID Document is null!";
	public static final String DID_DOC_OP_IS_NULL = "DID Document Operation is null!";
	public static final String DID_DOC_OP_IS_INVALID = "Only 'setDidDocument' DID Document Operation operation must be provided!";
	public static final String MULTIPLE_DID_DOCS_IN_REQUEST = "Only one DIDDocument must be provided!";
	public static final String DID_IS_NULL = "DID is Null!";
	public static final String DID_DOESNT_EXIST = "DID does not exist!";
	public static final String METHOD_PREFIX_MISMATCH = "Not a DID of did:web method!";
	public static final String BASE_PATH_NOT_DIRECTORY = "Base path is not a directory!";
	public static final String BASE_PATH_NOT_WRITEABLE = "Base path is not writable!";
	public static final String BASE_PATH_NOT_DEFINED = "Base path is not defined!";
	public static final String BASE_URL_NOT_DEFINED = "Base URL is not defined!";
	public static final String WRONG_URL_PROTOCOL = "Provided URL protocol is not HTTPS!";
	public static final String WRONG_DOMAIN = "Domain name mismatch!";
	public static final String DID_FORMAT_ERROR = "DID Format error!";
	public static final String PROPERTIES_ALREADY_SET = "Properties are already set!";
}
