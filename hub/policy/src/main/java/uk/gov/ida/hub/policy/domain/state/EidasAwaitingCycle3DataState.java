package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;

import java.net.URI;
import java.util.Optional;

public class EidasAwaitingCycle3DataState extends AbstractAwaitingCycle3DataState {

    private static final long serialVersionUID = -9056285913241958733L;

    @JsonProperty
    private final String encryptedIdentityAssertion;
    @JsonProperty
    private final CountrySignedResponseContainer countrySignedResponseContainer;

    @JsonCreator
    public EidasAwaitingCycle3DataState(
        @JsonProperty("requestId") final String requestId,
        @JsonProperty("requestIssuerId") final String requestIssuerId,
        @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
        @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
        @JsonProperty("sessionId") final SessionId sessionId,
        @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas,
        @JsonProperty("identityProviderEntityId") final String identityProviderEntityId,
        @JsonProperty("matchingServiceAdapterEntityId") final String matchingServiceAdapterEntityId,
        @JsonProperty("relayState") final String relayState,
        @JsonProperty("persistentId") final PersistentId persistentId,
        @JsonProperty("levelOfAssurance") final LevelOfAssurance levelOfAssurance,
        @JsonProperty("encryptedIdentityAssertion") final String encryptedIdentityAssertion,
        @JsonProperty("forceAuthentication") final Boolean forceAuthentication,
        @JsonProperty("countrySignedResponseContainer") final CountrySignedResponseContainer countrySignedResponseContainer) {

        super(
            requestId,
            requestIssuerId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            sessionId,
            transactionSupportsEidas,
            identityProviderEntityId,
            matchingServiceAdapterEntityId,
            relayState,
            persistentId,
            levelOfAssurance,
            forceAuthentication
        );

        this.encryptedIdentityAssertion = encryptedIdentityAssertion;
        this.countrySignedResponseContainer = countrySignedResponseContainer;
    }

    public String getEncryptedIdentityAssertion() {
        return encryptedIdentityAssertion;
    }

    public Optional<CountrySignedResponseContainer> getCountrySignedResponseContainer() {
        return Optional.ofNullable(countrySignedResponseContainer);
    }
}
