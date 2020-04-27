package uk.gov.ida.hub.policy.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.controller.EidasCountrySelectedStateController;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectingState;
import uk.gov.ida.hub.policy.exception.EidasCountryNotSupportedException;
import uk.gov.ida.hub.policy.exception.EidasNotSupportedException;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CountriesServiceTest {
    @Mock
    private TransactionsConfigProxy configProxy;
    @Mock
    private SessionRepository sessionRepository;

    private CountriesService service;

    private SessionId sessionId;

    private static final String RELYING_PARTY_ID = "relyingPartyId";

    private static final EidasCountryDto COUNTRY_1 = new EidasCountryDto("id1", "country1", true);
    private static final EidasCountryDto COUNTRY_2 = new EidasCountryDto("id2", "country2", true);
    private static final EidasCountryDto DISABLED_COUNTRY = new EidasCountryDto("id3", "country3", false);

    @Before
    public void setUp() {
        service = new CountriesService(sessionRepository, configProxy);
        sessionId = SessionIdBuilder.aSessionId().with("coffee-pasta").build();
        when(sessionRepository.getTransactionSupportsEidas(sessionId)).thenReturn(true);
    }

    @Test(expected = EidasNotSupportedException.class)
    public void shouldReturnErrorWhenGettingCountriesWithTxnNotSupportingEidas() {
        when(sessionRepository.getTransactionSupportsEidas(sessionId)).thenReturn(false);

        service.getCountries(sessionId);
    }

    @Test
    public void shouldReturnEnabledSystemWideCountriesWhenRpHasNoExplicitlyEnabledCountries() {
        setSystemWideCountries(asList(COUNTRY_1, COUNTRY_2, DISABLED_COUNTRY));

        List<EidasCountryDto> countries = service.getCountries(sessionId);

        assertThat(countries).isEqualTo(asList(COUNTRY_1, COUNTRY_2));
    }

    @Test
    public void shouldReturnIntersectionOfEnabledSystemWideCountriesAndRPConfiguredCountries() {
        setSystemWideCountries(asList(COUNTRY_1, COUNTRY_2, DISABLED_COUNTRY));
        when(sessionRepository.getRequestIssuerEntityId(sessionId)).thenReturn(RELYING_PARTY_ID);
        when(configProxy.getEidasSupportedCountriesForRP(RELYING_PARTY_ID)).thenReturn(singletonList(COUNTRY_2.getEntityId()));

        assertThat(service.getCountries(sessionId)).isEqualTo(singletonList(COUNTRY_2));
    }

    @Test
    public void shouldSetSelectedCountry() {
        EidasCountrySelectedStateController mockEidasCountrySelectedStateController = mock(EidasCountrySelectedStateController.class);
        when(sessionRepository.getStateController(sessionId, EidasCountrySelectingState.class)).thenReturn(mockEidasCountrySelectedStateController);
        setSystemWideCountries(singletonList(COUNTRY_1));

        service.setSelectedCountry(sessionId, COUNTRY_1.getSimpleId());

        verify(mockEidasCountrySelectedStateController).selectCountry(COUNTRY_1.getEntityId());
    }

    @Test(expected = EidasCountryNotSupportedException.class)
    public void shouldReturnErrorWhenAnInvalidCountryIsSelected() {
        setSystemWideCountries(singletonList(COUNTRY_1));

        service.setSelectedCountry(sessionId, "not-a-valid-country-code");
    }

    @Test(expected = EidasNotSupportedException.class)
    public void shouldReturnErrorWhenACountryIsSelectedWithTxnNotSupportingEidas() {
        when(sessionRepository.getTransactionSupportsEidas(sessionId)).thenReturn(false);

        service.setSelectedCountry(sessionId, "NL");
    }

    private void setSystemWideCountries(List<EidasCountryDto> countryList) {
        when(configProxy.getEidasSupportedCountries()).thenReturn(countryList);
    }
}
