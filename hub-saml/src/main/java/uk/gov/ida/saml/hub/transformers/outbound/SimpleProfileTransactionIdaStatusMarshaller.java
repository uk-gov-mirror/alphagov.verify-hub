package uk.gov.ida.saml.hub.transformers.outbound;

import javax.inject.Inject;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.core.transformers.outbound.IdaStatusMarshaller;

import java.util.Map;

public class SimpleProfileTransactionIdaStatusMarshaller extends IdaStatusMarshaller<TransactionIdaStatus> {

    private static final Map<TransactionIdaStatus, DetailedStatusCode> REST_TO_SAML_CODES = Map.of(
                    TransactionIdaStatus.Success, DetailedStatusCode.Success,
                    TransactionIdaStatus.NoAuthenticationContext, DetailedStatusCode.NoAuthenticationContext,
                    // no-match is different in the simple saml profile: because there are no assertions included
                    // it should not be Success as it is in the standard signed responses from hub.  The Success
                    // response was changed to Responder for the simple saml profile (see the relatively opaque text
                    // in saml-profiles-2.0-os.pdf:544 that says a Success response MUST have an assertion
                    TransactionIdaStatus.NoMatchingServiceMatchFromHub, DetailedStatusCode.SimpleProfileNoMatchingServiceMatchFromHub,
                    TransactionIdaStatus.AuthenticationFailed, DetailedStatusCode.AuthenticationFailed,
                    TransactionIdaStatus.RequesterError, DetailedStatusCode.RequesterErrorFromIdpAsSentByHub);

    @Inject
    public SimpleProfileTransactionIdaStatusMarshaller(OpenSamlXmlObjectFactory samlObjectFactory) {
        super(samlObjectFactory);
    }


    @Override
    protected DetailedStatusCode getDetailedStatusCode(TransactionIdaStatus originalStatus) {
        return REST_TO_SAML_CODES.get(originalStatus);
    }
}
