package uk.gov.ida.hub.config.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableList;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.data.ConfigEntityDataRepository;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateDetails;
import uk.gov.ida.hub.config.domain.EntityConfigDataToCertificateDtoTransformer;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.OCSPCertificateChainValidityChecker;
import uk.gov.ida.hub.config.domain.SignatureVerificationCertificate;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.dto.CertificateDto;
import uk.gov.ida.hub.config.dto.CertificateHealthCheckDto;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.hub.config.dto.InvalidCertificateDto;
import uk.gov.ida.hub.config.exceptions.ExceptionFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static uk.gov.ida.hub.config.dto.CertificateHealthCheckDto.createCertificateHealthCheckDto;

@Path(Urls.ConfigUrls.CERTIFICATES_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class CertificatesResource {
    private final ConfigEntityDataRepository<TransactionConfigEntityData> transactionDataSource;
    private final ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceDataSource;
    private final ExceptionFactory exceptionFactory;
    private final ConfigConfiguration configuration;
    private final OCSPCertificateChainValidityChecker ocspCertificateChainValidityChecker;
    private final EntityConfigDataToCertificateDtoTransformer configDataToCertificateDtoTransformer;

    @Inject
    public CertificatesResource(
            ConfigEntityDataRepository<TransactionConfigEntityData> transactionDataSource,
            ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceDataSource,
            ExceptionFactory exceptionFactory,
            ConfigConfiguration configuration,
            OCSPCertificateChainValidityChecker ocspCertificateChainValidityChecker,
            EntityConfigDataToCertificateDtoTransformer configDataToCertificateDtoTransformer
    ) {
        this.transactionDataSource = transactionDataSource;
        this.matchingServiceDataSource = matchingServiceDataSource;
        this.exceptionFactory = exceptionFactory;
        this.configuration = configuration;
        this.ocspCertificateChainValidityChecker = ocspCertificateChainValidityChecker;
        this.configDataToCertificateDtoTransformer = configDataToCertificateDtoTransformer;
    }

    @GET
    @Path(Urls.ConfigUrls.ENCRYPTION_CERTIFICATE_PATH)
    @Timed
    public CertificateDto getEncryptionCertificate(@PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {
        Optional<TransactionConfigEntityData> transactionData = getTransactionConfigData(entityId);
        if (transactionData.isPresent()) {
            return new CertificateDto(
                    transactionData.get().getEntityId(),
                    transactionData.get().getEncryptionCertificate().getX509(),
                    CertificateDto.KeyUse.Encryption,
                    FederationEntityType.RP);
        }

        Optional<MatchingServiceConfigEntityData> matchingServiceData = matchingServiceDataSource.getData(entityId);
        if (matchingServiceData.isPresent()) {
            return new CertificateDto(
                    matchingServiceData.get().getEntityId(),
                    matchingServiceData.get().getEncryptionCertificate().getX509(),
                    CertificateDto.KeyUse.Encryption,
                    FederationEntityType.MS);
        }

        throw exceptionFactory.createNoDataForEntityException(entityId);
    }

    @GET
    @Path(Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATE_PATH)
    @Timed
    public Collection<CertificateDto> getSignatureVerificationCertificates(
            @PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {

        Optional<TransactionConfigEntityData> transactionData = getTransactionConfigData(entityId);
        if (transactionData.isPresent()) {
            return transform(entityId, transactionData.get().getSignatureVerificationCertificates(), FederationEntityType.RP);
        }

        Optional<MatchingServiceConfigEntityData> matchingServiceData = matchingServiceDataSource.getData(entityId);
        if (matchingServiceData.isPresent()) {
            return transform(entityId, matchingServiceData.get().getSignatureVerificationCertificates(), FederationEntityType.MS);
        }

        throw exceptionFactory.createNoDataForEntityException(entityId);
    }

    /**
     * This checks expiry dates of RP & MSA certificates and returns details of all certs
     */
    @GET
    @Path(Urls.ConfigUrls.CERTIFICATES_HEALTH_CHECK_PATH)
    public Response getHealthCheck() throws CertificateException {
        return Response.ok(getCertHealthCheckDtos()).build();
    }

    /**
     * This performs OCSP checking of RP & MSA certificates
     */
    @GET
    @Path(Urls.ConfigUrls.INVALID_CERTIFICATES_CHECK_PATH)
    public Response invalidCertificatesCheck() {
        ImmutableList<CertificateDetails> certificateDtos = configDataToCertificateDtoTransformer.transform(transactionDataSource.getAllData(), matchingServiceDataSource.getAllData());
        ImmutableList<InvalidCertificateDto> invalidCertificateDtos = ocspCertificateChainValidityChecker.check(certificateDtos);
        return Response.ok(invalidCertificateDtos).build();
    }

    private List<CertificateHealthCheckDto> getCertHealthCheckDtos() throws CertificateException {
        List<CertificateHealthCheckDto> certs = new LinkedList<>();
        // IDP certs are now in the federation metadata and checked for expiry and OCSP status in separate sensu checks
        for(TransactionConfigEntityData transaction : transactionDataSource.getAllData()) {
            certs.add(createCertificateHealthCheckDto(
                    transaction.getEntityId(),
                    transaction.getEncryptionCertificate(),
                    configuration.getCertificateWarningPeriod()));
            addCertificateHealthCheckDtos(
                    certs,
                    transaction.getEntityId(),
                    transaction.getSignatureVerificationCertificates());
        }
        for(MatchingServiceConfigEntityData ms : matchingServiceDataSource.getAllData()) {
            certs.add(createCertificateHealthCheckDto(
                    ms.getEntityId(),
                    ms.getEncryptionCertificate(),
                    configuration.getCertificateWarningPeriod()));
            addCertificateHealthCheckDtos(
                    certs,
                    ms.getEntityId(),
                    ms.getSignatureVerificationCertificates());
        }
        return certs;
    }

    private void addCertificateHealthCheckDtos(
            List<CertificateHealthCheckDto> dtos,
            final String entityId,
            Collection<? extends Certificate> certificates) throws CertificateException {
        for(Certificate cert : certificates) {
            dtos.add(createCertificateHealthCheckDto(
                    entityId,
                    cert,
                    configuration.getCertificateWarningPeriod()));
        }
    }

    private Collection<CertificateDto> transform(String entityId, Collection<SignatureVerificationCertificate> signatureVerificationCertificates, FederationEntityType federationEntityType) {
        Collection<CertificateDto> certificates = new ArrayList<>();
        for (Certificate signatureVerificationCertificate : signatureVerificationCertificates) {
            final CertificateDto certificate = new CertificateDto(
                    entityId,
                    signatureVerificationCertificate.getX509(),
                    CertificateDto.KeyUse.Signing,
                    federationEntityType);
            certificates.add(certificate);
        }
        return certificates;
    }

    private Optional<TransactionConfigEntityData> getTransactionConfigData(String entityId) {
        final Optional<TransactionConfigEntityData> configData = transactionDataSource.getData(entityId);
        if (configData.isPresent() && !configData.get().isEnabled()) {
            throw exceptionFactory.createDisabledTransactionException(entityId);
        }
        return configData;
    }
}
