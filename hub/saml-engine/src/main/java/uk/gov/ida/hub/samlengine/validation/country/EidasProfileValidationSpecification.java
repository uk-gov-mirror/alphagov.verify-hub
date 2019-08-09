package uk.gov.ida.hub.samlengine.validation.country;

import org.slf4j.event.Level;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;

import static java.text.MessageFormat.format;

public class EidasProfileValidationSpecification {

    public static SamlTransformationErrorException authnResponseAssertionIssuerFormatMismatch(String responseIssuerFormat, String assertionIssuerFormat) {
        return new SamlTransformationErrorException(
            format("The Authn Response issuer format [{0}] does not match the assertion issuer format [{1}]", responseIssuerFormat, assertionIssuerFormat),
            Level.ERROR
        );
    }

    public static SamlTransformationErrorException authnResponseAssertionIssuerValueMismatch(String responseIssuer, String assertionIssuer) {
        return new SamlTransformationErrorException(
            format("The Authn Response issuer [{0}] does not match the assertion issuer [{1}]", responseIssuer, assertionIssuer),
            Level.ERROR
        );
    }
}
