package uk.gov.ida.saml.hub.validators.response.idp;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import uk.gov.ida.saml.core.validators.DestinationValidator;
import uk.gov.ida.saml.hub.validators.response.idp.components.EncryptedResponseFromIdpValidator;
import uk.gov.ida.saml.hub.validators.response.idp.components.ResponseAssertionsFromIdpValidator;
import uk.gov.ida.saml.security.AssertionDecrypter;
import uk.gov.ida.saml.security.SamlAssertionsSignatureValidator;
import uk.gov.ida.saml.security.exception.SamlFailedToDecryptException;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IdpResponseValidatorTest {

    @Mock
    private SamlResponseSignatureValidator samlResponseSignatureValidator;
    @Mock
    private AssertionDecrypter assertionDecrypter;
    @Mock
    private AssertionDecrypter badAssertionDecrypter;
    @Mock
    private SamlAssertionsSignatureValidator samlAssertionsSignatureValidator;
    @Mock
    private EncryptedResponseFromIdpValidator encryptedResponseFromIdpValidator;
    @Mock
    private DestinationValidator responseDestinationValidator;
    @Mock
    private ResponseAssertionsFromIdpValidator responseAssertionsFromIdpValidator;
    @Mock
    private Response response;

    @Rule
    public final ExpectedException samlValidationException = ExpectedException.none();

    private IdpResponseValidator validator;

    @Before
    public void setUp() {
        validator = new IdpResponseValidator(
            samlResponseSignatureValidator,
            Arrays.asList(assertionDecrypter, badAssertionDecrypter),
            samlAssertionsSignatureValidator,
            encryptedResponseFromIdpValidator,
            responseDestinationValidator,
            responseAssertionsFromIdpValidator);
    }

    @Test
    public void shouldValidateResponseIsEncrypted() {
        validator.validate(response);
        verify(encryptedResponseFromIdpValidator).validate(response);
    }

    @Test
    public void shouldValidateResponseDestination() {
        validator.validate(response);
        verify(responseDestinationValidator).validate(response.getDestination());
    }

    @Test
    public void shouldValidateSamlResponseSignature() {
        validator.validate(response);
        verify(samlResponseSignatureValidator).validate(response, IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
    }

    @Test
    public void shouldValidateSamlAssertionSignature() {
        Assertion assertion = mock(Assertion.class);
        List<Assertion> assertions = List.of(assertion);
        ValidatedResponse validatedResponse = mock(ValidatedResponse.class);

        when(samlResponseSignatureValidator.validate(response, IDPSSODescriptor.DEFAULT_ELEMENT_NAME)).thenReturn(validatedResponse);
        when(assertionDecrypter.decryptAssertions(validatedResponse)).thenReturn(assertions);
        when(badAssertionDecrypter.decryptAssertions(validatedResponse)).thenThrow(SamlFailedToDecryptException.class);

        validator.validate(response);

        verify(samlAssertionsSignatureValidator).validate(assertions, IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
    }

    @Test
    public void shouldThrowIfAllDecryptersFail() {
        ValidatedResponse validatedResponse = mock(ValidatedResponse.class);

        when(samlResponseSignatureValidator.validate(response, IDPSSODescriptor.DEFAULT_ELEMENT_NAME)).thenReturn(validatedResponse);
        when(assertionDecrypter.decryptAssertions(validatedResponse)).thenThrow(SamlFailedToDecryptException.class);
        when(badAssertionDecrypter.decryptAssertions(validatedResponse)).thenThrow(SamlFailedToDecryptException.class);

        samlValidationException.expect(SamlFailedToDecryptException.class);
        samlValidationException.expectMessage("Could not decrypt IDP assertions with any of the decrypters");
        validator.validate(response);
    }
}
