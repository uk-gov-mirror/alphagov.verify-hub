package uk.gov.ida.hub.policy.builder.domain;


import com.google.common.base.Optional;
import uk.gov.ida.hub.policy.domain.IdpIdaStatus;
import uk.gov.ida.hub.policy.domain.InboundResponseFromIdpDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;

public class InboundResponseFromIdpDtoBuilder {
    public static InboundResponseFromIdpDto successResponse(String idpEntityId, LevelOfAssurance levelOfAssurance) {
        return buildDTO(IdpIdaStatus.Status.Success,
                idpEntityId,
                Optional.of(levelOfAssurance),
                null);
    }

    public static InboundResponseFromIdpDto errorResponse(String idpEntityId, IdpIdaStatus.Status status) {
        return buildDTO(status, idpEntityId,
                Optional.of(LevelOfAssurance.LEVEL_2),
                Optional.<String>absent());
    }

    public static InboundResponseFromIdpDto fraudResponse(String idpEntityId) {
        return buildDTO(IdpIdaStatus.Status.RequesterError, idpEntityId,
                Optional.of(LevelOfAssurance.LEVEL_X),
                Optional.of("fraudIndicator"));
    }

    public static InboundResponseFromIdpDto unsupportedResponse(String idpEntityId) {
        return buildDTO(IdpIdaStatus.Status.valueOf("unsupported"), idpEntityId,
                Optional.of(LevelOfAssurance.LEVEL_X),
                Optional.of("unsupported"));
    }

    public static InboundResponseFromIdpDto failedResponse(String idpEntityId) {
        return buildDTO(IdpIdaStatus.Status.AuthenticationFailed, idpEntityId, Optional.of(LevelOfAssurance.LEVEL_2), Optional.<String>absent());
    }

    public static InboundResponseFromIdpDto noAuthnContextResponse(String idpEntityId) {
        return buildDTO(IdpIdaStatus.Status.NoAuthenticationContext, idpEntityId, Optional.<LevelOfAssurance>absent(),
                Optional.<String>absent());
    }

    public static InboundResponseFromIdpDto authnPendingResponse(String idpEntityId) {
        return buildDTO(IdpIdaStatus.Status.AuthenticationPending, idpEntityId, Optional.<LevelOfAssurance>absent(),
                Optional.<String>absent());
    }

    private static InboundResponseFromIdpDto buildDTO(IdpIdaStatus.Status status, String idpEntityId,
                                                      Optional<LevelOfAssurance> levelOfAssurance,
                                                      Optional<String> fraudText) {
        return new InboundResponseFromIdpDto(
                status,
                Optional.fromNullable("message"),
                idpEntityId,
                Optional.fromNullable("authnStatement"),
                Optional.of("encrypted-mds-assertion"),
                Optional.fromNullable("pid"),
                Optional.fromNullable("principalipseenbyidp"),
                levelOfAssurance,
                fraudText,
                fraudText);
    }
}
