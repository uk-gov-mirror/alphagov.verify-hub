package uk.gov.ida.hub.samlproxy.config;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.exceptions.CertificateChainValidationException;
import uk.gov.ida.hub.samlproxy.builders.CertificateDtoBuilder;
import uk.gov.ida.hub.samlproxy.domain.CertificateDto;
import uk.gov.ida.hub.samlproxy.domain.FederationEntityType;

import java.io.IOException;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.ida.common.shared.security.verification.CertificateValidity.invalid;
import static uk.gov.ida.common.shared.security.verification.CertificateValidity.valid;
import static uk.gov.ida.hub.samlproxy.builders.CertificateDtoBuilder.aCertificateDto;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_ONE;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_TWO;

@RunWith(MockitoJUnitRunner.class)
public class ConfigServiceKeyStoreTest {
    @Mock
    private CertificatesConfigProxy certificatesConfigProxy;
    @Mock
    private CertificateChainValidator certificateChainValidator;
    @Mock
    private X509CertificateFactory x509CertificateFactory;
    @Mock
    private TrustStoreForCertificateProvider trustStoreForCertificateProvider;
    @Mock
    private X509Certificate x509Certificate;
    @Mock
    private KeyStore trustStore;

    private String issuerId;
    private ConfigServiceKeyStore configServiceKeyStore;

    @Before
    public void setup() throws CertificateException {
        issuerId = "issuer-id";
        configServiceKeyStore = new ConfigServiceKeyStore(
                certificatesConfigProxy,
                certificateChainValidator,
                trustStoreForCertificateProvider,
                x509CertificateFactory);
    }

    @Test
    public void getVerifyingKeysForEntity_shouldGetVerifyingKeysFromConfigCertificateProxy() throws Exception {
        configServiceKeyStore.getVerifyingKeysForEntity(issuerId);

        verify(certificatesConfigProxy).getSignatureVerificationCertificates(issuerId);
    }

    @Test
    public void getVerifyingKeysForEntity_shouldReturnAllKeysReturnedByConfig() throws Exception {
        final CertificateDto certOneDto = getX509Certificate(STUB_IDP_ONE);
        final CertificateDto certTwoDto = getX509Certificate(STUB_IDP_TWO);
        when(certificatesConfigProxy.getSignatureVerificationCertificates(issuerId)).thenReturn(of(certOneDto, certTwoDto));
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(x509CertificateFactory.createCertificate(certTwoDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(trustStore);
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(valid());

        List<PublicKey> keys = configServiceKeyStore.getVerifyingKeysForEntity(issuerId);

        assertThat(keys.size()).isEqualTo(2);
    }

    @Test
    public void getVerifyingKeysForEntity_shouldValidateEachKeyReturnedByConfig() throws Exception {
        final CertificateDto certOneDto = getX509Certificate(STUB_IDP_ONE);
        final CertificateDto certTwoDto = getX509Certificate(STUB_IDP_TWO);
        when(certificatesConfigProxy.getSignatureVerificationCertificates(issuerId)).thenReturn(of(certOneDto, certTwoDto));
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(x509CertificateFactory.createCertificate(certTwoDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(trustStore);
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(valid());

        configServiceKeyStore.getVerifyingKeysForEntity(issuerId);

        verify(certificateChainValidator, times(2)).validate(x509Certificate, trustStore);
    }

    @Test
    public void getVerificationKeyForEntity_shouldThrowExceptionIfCertificateIsInvalid() throws Exception {
        final CertificateDto certOneDto = getX509Certificate(STUB_IDP_ONE);
        when(certificatesConfigProxy.getSignatureVerificationCertificates(issuerId)).thenReturn(of(certOneDto));
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(trustStore);
        CertPathValidatorException underlyingException = new CertPathValidatorException("Invalid Certificate");
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(invalid(underlyingException));
        try {
            configServiceKeyStore.getVerifyingKeysForEntity(issuerId);
            Assert.fail(String.format("Expected [%s]", CertificateChainValidationException.class.getSimpleName()));
        } catch (CertificateChainValidationException success) {
            assertThat(success.getMessage()).isEqualTo("Certificate is not valid: Unable to get DN");
            assertThat(success.getCause()).isEqualTo(underlyingException);
        }
    }

    @Test
    public void getEncryptionKeyForEntity_shouldGetEncryptionKeysFromConfigCertificateProxy() throws Exception {
        when(certificatesConfigProxy.getEncryptionCertificate(anyString())).thenReturn(aCertificateDto().build());
        when(x509CertificateFactory.createCertificate(anyString())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(trustStore);
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(valid());

        configServiceKeyStore.getEncryptionKeyForEntity(issuerId);

        verify(certificatesConfigProxy).getEncryptionCertificate(issuerId);
    }

    @Test
    public void getEncryptionKeyForEntity_shouldValidateTheKeyReturnedByConfig() throws Exception {
        final CertificateDto certOneDto = getX509Certificate(STUB_IDP_ONE);
        when(certificatesConfigProxy.getEncryptionCertificate(issuerId)).thenReturn(certOneDto);
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(trustStore);
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(valid());

        configServiceKeyStore.getEncryptionKeyForEntity(issuerId);

        verify(certificateChainValidator).validate(x509Certificate, trustStore);
    }

    @Test
    public void getEncryptionKeyForEntity_shouldThrowExceptionIfCertificateIsInvalid() throws Exception {
        final CertificateDto certOneDto = getX509Certificate(STUB_IDP_ONE);
        when(certificatesConfigProxy.getEncryptionCertificate(issuerId)).thenReturn(certOneDto);
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(trustStore);
        CertPathValidatorException underlyingException = new CertPathValidatorException("Invalid Certificate");
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(invalid(underlyingException));
        try {
            configServiceKeyStore.getEncryptionKeyForEntity(issuerId);
            Assert.fail(String.format("Expected [%s]", CertificateChainValidationException.class.getSimpleName()));
        } catch (CertificateChainValidationException success) {
            assertThat(success.getMessage()).isEqualTo("Certificate is not valid: Unable to get DN");
            assertThat(success.getCause()).isEqualTo(underlyingException);
        }
    }

    private static CertificateDto getX509Certificate(String entityId) throws IOException {
        return new CertificateDtoBuilder().withIssuerId(entityId).withCertificate(STUB_IDP_PUBLIC_PRIMARY_CERT).build();
    }
}
