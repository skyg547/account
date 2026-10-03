package com.ho.account.auth.core.application.port.in;

import com.ho.account.auth.core.application.model.PersonalAccessTokenCreateResponse;
import com.ho.account.auth.core.application.model.PersonalAccessTokenDto;
import java.util.List;

/** PAT management operations bound to the verified Authorization header. */
public interface PersonalAccessTokenUseCase {

    PersonalAccessTokenCreateResponse createToken(String authorizationHeader, String tokenName, int expireDays);

    List<PersonalAccessTokenDto> getUserTokens(String authorizationHeader);

    List<PersonalAccessTokenDto> getAllTokensForAdmin(String authorizationHeader);

    boolean revokeOwnToken(String authorizationHeader, String tokenId);

    boolean revokeTokenForAdmin(String authorizationHeader, String tokenId);
}
