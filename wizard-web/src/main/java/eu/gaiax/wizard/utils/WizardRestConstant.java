package eu.gaiax.wizard.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WizardRestConstant {

    public static final String API_PUBLIC_URL_PREFIX = "/public";
    public static final String PARTICIPANT_ROOT = "/participant";

    public static final String REGISTER = API_PUBLIC_URL_PREFIX + "/register";

    public static final String SEND_REQUIRED_ACTIONS_EMAIL = API_PUBLIC_URL_PREFIX + "/registration/send-email";


    public static final String MASTER_DATA_FILTER = API_PUBLIC_URL_PREFIX + "/master-data/{dataType}/filter";

    public static final String LABEL_LEVEL_QUESTIONS = API_PUBLIC_URL_PREFIX + "/label-level-questions";

    public static final String CHECK_REGISTRATION = API_PUBLIC_URL_PREFIX + "/check-registration";

    public static final String POLICY_EVALUATE = API_PUBLIC_URL_PREFIX + "/policy/evaluate";

    public static final String POLICY_EVALUATE_V2 = API_PUBLIC_URL_PREFIX + "/policy/evaluate/v2";

    public static final String PARTICIPANT_CONFIG = "/participant/config";

    public static final String PARTICIPANT_EXPORT = "/participant/{participantId}/export";

    public static final String PARTICIPANT_PROFILE = "/participant/{participantId}/profile";

    public static final String PARTICIPANT_PROFILE_IMAGE = "/participant/{participantId}/profile-image";

    public static final String SERVICE_OFFER = "/service-offer";
    public static final String TENANT = "/tenant";
    public static final String LABEL_LEVEL = API_PUBLIC_URL_PREFIX + "/label-level";
    public static final String LABEL_LEVEL_FILE_UPLOAD = API_PUBLIC_URL_PREFIX + "/label-level/file";
    public static final String LABEL_LEVEL_FILE_DOWNLOAD = "label-level/file/{fileName}/**";

    public static final String PUBLIC_SERVICE_OFFER = API_PUBLIC_URL_PREFIX + "/service-offer";

    public static final String PUBLIC_POLICY = API_PUBLIC_URL_PREFIX + "/policy";

    public static final String VALIDATE_SERVICE_OFFER = API_PUBLIC_URL_PREFIX + "/service-offer/validate";

    public static final String SERVICE_OFFER_LOCATION = API_PUBLIC_URL_PREFIX + "/service-offer/location";

    public static final String SERVICE_OFFER_FILTER = API_PUBLIC_URL_PREFIX + "/service-offer/filter";

    public static final String PARTICIPANT_SERVICE_OFFER_FILTER = "/participant/{participantId}/service-offer/filter";

    public static final String PARTICIPANT_SERVICE_OFFER_DETAILS = "/participant/{participantId}/service-offer/{serviceOfferId}";

    public static final String RESOURCE_FILTER = API_PUBLIC_URL_PREFIX + "/resource/filter";

    public static final String PARTICIPANT_RESOURCE_FILTER = "/participant/{participantId}/resource/filter";
    public static final String PARTICIPANT_RESOURCE = "/participant/{participantId}/resource";

    public static final String ONBOARD_PARTICIPANT = "/onboard/participant/{participantId}";

    public static final String VALIDATE_PARTICIPANT = API_PUBLIC_URL_PREFIX + "/validate/participant";

    public static final String WELL_KNOWN = "/.well-known/{fileName}";

    public static final String PARTICIPANT_JSON = "/{participantId}/{fileName}";

    public static final String PARTICIPANT_SUBDOMAIN = "/subdomain/{participantId}";

    public static final String PARTICIPANT_CERTIFICATE = "/certificate/{participantId}";

    public static final String PARTICIPANT_INGRESS = "/ingress/{participantId}";

    public static final String PARTICIPANT_DID = "/did/{participantId}";

    public static final String CREATE_PARTICIPANT = "/participant/{participantId}";

    //multiTenant
    public static final String MASTER_DB_KEY = "master";
    public static final String TENANT_HEADER_KEY = "X-TenantId";
    public static final String ALL_TENANT_DATASOURCE = "allTenantDbConfig";
    public static final String API_PUBLIC_CREATE_TENANT = API_PUBLIC_URL_PREFIX + "/tenant";
    public static final String API_PUBLIC_TENANTS = API_PUBLIC_URL_PREFIX + "/tenants";


}
