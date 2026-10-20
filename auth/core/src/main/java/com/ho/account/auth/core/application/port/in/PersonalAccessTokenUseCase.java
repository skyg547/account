package com.ho.account.auth.core.application.port.in;

import com.ho.account.auth.core.application.model.PersonalAccessTokenCreateResponse;
import com.ho.account.auth.core.application.model.PersonalAccessTokenDto;
import com.ho.account.auth.core.application.model.ListPage;

/** PAT management operations bound to the verified Authorization header. */
public interface PersonalAccessTokenUseCase {

    PersonalAccessTokenCreateResponse createToken(String authorizationHeader, String tokenName, int expireDays);

    ListPage<PersonalAccessTokenDto> getUserTokens(String authorizationHeader, int page, int size);

    ListPage<PersonalAccessTokenDto> getAllTokensForAdmin(String authorizationHeader, int page, int size);

    boolean revokeOwnToken(String authorizationHeader, String tokenId);

    boolean revokeTokenForAdmin(String authorizationHeader, String tokenId);
}
