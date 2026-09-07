package com.pikume.back.user.auth.application.port.out;
import com.pikume.back.user.auth.domain.OAuthAuthorizationRequest;
import java.time.Instant;
public interface OAuthAuthorizationRequestStorePort {
    /** Atomically checks both DB rate limits and inserts the request. */
    void create(OAuthAuthorizationRequest request, String originHash, int callerLimit, int originLimit);
    /** Short exclusive DB transaction, committed before returning. */
    OAuthAuthorizationRequest claim(String stateHash, String bindingHash, OAuthAuthorizationRequest.Channel channel, Instant now);
    void finish(String id, OAuthAuthorizationRequest.Status status);
    int cleanup(Instant now, int limit);
}
