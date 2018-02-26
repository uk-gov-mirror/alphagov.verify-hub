package uk.gov.ida.saml.hub.validators.response.common;

import com.google.common.base.Strings;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.emptyIssuer;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.missingIssuer;

public class IssuerValidator {

    public static void validate(Response response) {
        Issuer issuer = response.getIssuer();
        if (issuer == null) throwError(missingIssuer());

        String issuerId = issuer.getValue();
        if (Strings.isNullOrEmpty(issuerId)) throwError(emptyIssuer());
    }

    private static void throwError(SamlValidationSpecificationFailure failure) {
        throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
    }
}
