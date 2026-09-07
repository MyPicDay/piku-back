package com.pikume.back.user.auth.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.security.adapter.in.web.AuthUserResponseMapper;
import com.pikume.back.user.application.dto.UserAvatarReference;
import com.pikume.back.user.application.port.in.*;
import com.pikume.back.user.auth.application.dto.*;
import com.pikume.back.user.auth.application.port.in.*;
import com.pikume.back.user.auth.application.exception.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.cors.CorsConfiguration;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SignupHttpContractTest {
    SignupFlowUseCase flow = mock(SignupFlowUseCase.class);
    QuerySignupConfigurationUseCase config = mock(QuerySignupConfigurationUseCase.class);
    IssueUserSessionUseCase issue = mock(IssueUserSessionUseCase.class);
    LegacySignupProofUseCase legacy = mock(LegacySignupProofUseCase.class);
    VerifyEmailUseCase verify = mock(VerifyEmailUseCase.class);
    GoogleAuthenticationUseCase google = mock(GoogleAuthenticationUseCase.class);
    MockMvc mvc;
    static final String BINDING="b".repeat(43), CSRF="c".repeat(43), PROOF="p".repeat(43);
    @BeforeEach void setup() {
        var credentials = new SignupWebCredentials(request -> {var cors=new CorsConfiguration();cors.setAllowedOrigins(List.of("https://www.pikume.com"));return cors;});
        var sessions = new SignupSessionResponseWriter(issue,new AuthUserResponseMapper((value,accessible) -> "https://assets.example/"+value),credentials);
        var controller = new SignupController(flow,mock(QuerySignupAgreementUseCase.class),config,mock(QueryUserAccessUseCase.class),mock(ReserveSignupNicknameUseCase.class),mock(CompleteSignupProfileUseCase.class),mock(WithdrawPendingSignupUseCase.class),credentials,sessions);
        var old = new AuthController(legacy,verify,mock(ResetPasswordUseCase.class),mock(QueryAllowedEmailUseCase.class),config,credentials);
        var settings = new SignupWebSettings(); settings.setCompletionUri("https://www.pikume.com/auth/complete");
        mvc=MockMvcBuilders.standaloneSetup(controller,old,new GoogleAuthenticationController(google,credentials,sessions,settings))
            .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new SignupExceptionHandler(new ProblemDetailFactory())).build();
    }
    @Test void pageEntryIssuesOnlyCookiesAndNoSignupRecord() throws Exception {
        given(config.querySignupConfiguration()).willReturn(new SignupConfiguration(true,false));
        given(flow.progress(isNull(),anyString())).willReturn(new SignupProgress(SignupNextAction.AUTHENTICATE,null,null,null,null));
        var response=mvc.perform(get("/api/auth/signup/progress")).andExpect(status().isOk())
            .andExpect(jsonPath("$.progress.nextAction").value("AUTHENTICATE")).andExpect(jsonPath("$.csrfToken").exists())
            .andExpect(jsonPath("$.callerBinding").doesNotExist()).andReturn().getResponse();
        assertThat(response.getHeaders("Set-Cookie")).hasSize(2);
        verify(flow).progress(isNull(),anyString()); verifyNoMoreInteractions(flow);
    }
    @Test void webWriteRequiresCsrfBeforeApplicationCall() throws Exception {
        mvc.perform(post("/api/auth/signup/agreements").header("Origin","https://www.pikume.com")
            .contentType(MediaType.APPLICATION_JSON).content("{\"agreements\":[{\"type\":\"TERMS\",\"version\":\"v1\",\"agreed\":true}]}"))
            .andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(403)).andExpect(jsonPath("$.detail").isString()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        verifyNoInteractions(flow);
    }
    @Test void webProofLivesOnlyInSecureCookie() throws Exception {
        given(flow.authenticateEmail(any())).willReturn(new SignupProofResult(PROOF,new SignupProgress(SignupNextAction.AGREEMENTS,"user@gmail.com",null,null,Instant.now().plusSeconds(600))));
        var response=mvc.perform(post("/api/auth/signup/email").header("Origin","https://www.pikume.com").header("X-Signup-CSRF",CSRF)
            .cookie(cookies()).contentType(MediaType.APPLICATION_JSON).content(emailBody()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.proof").doesNotExist()).andExpect(jsonPath("$.tokens").doesNotExist())
            .andReturn().getResponse();
        assertThat(response.getHeaders("Set-Cookie")).anySatisfy(cookie -> assertThat(cookie).contains(SignupWebCredentials.PROOF+"="+PROOF,"HttpOnly","Secure"));
        verifyNoInteractions(issue);
    }
    @Test void mobileCannotUseWebCookiesAsProofOfCaller() throws Exception {
        mvc.perform(post("/api/mobile/auth/signup/email").cookie(cookies()).contentType(MediaType.APPLICATION_JSON).content(emailBody()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CALLER_REQUIRED"));
        verifyNoInteractions(flow);
    }
    @Test void mobileProofIsReturnedInBodyWithoutCookies() throws Exception {
        given(flow.authenticateEmail(any())).willReturn(new SignupProofResult(PROOF,new SignupProgress(SignupNextAction.AGREEMENTS,"user@gmail.com",null,null,Instant.now().plusSeconds(600))));
        mvc.perform(post("/api/mobile/auth/signup/email").header("X-Signup-Binding",BINDING).contentType(MediaType.APPLICATION_JSON).content(emailBody()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.proof").value(PROOF)).andExpect(header().doesNotExist("Set-Cookie"));
    }
    @Test void agreementCompletionUsesExistingWebSessionContract() throws Exception {
        given(flow.agree(any())).willReturn(new SignupProofResult(PROOF,new SignupProgress(SignupNextAction.PROFILE,"user@gmail.com","user","REQUIRED",Instant.now().plusSeconds(500))));
        given(issue.issueSession("user","device")).willReturn(new LoginResult("access","refresh",new LoginResult.UserInfo("user","가입대기_a",new UserAvatarReference("base.webp",false,true),"REQUIRED")));
        mvc.perform(post("/api/auth/signup/agreements").header("Origin","https://www.pikume.com").header("X-Signup-CSRF",CSRF).header("Device-Id","device")
            .cookie(cookies()).contentType(MediaType.APPLICATION_JSON).content("{\"agreements\":[{\"type\":\"TERMS\",\"version\":\"v1\",\"agreed\":true}]}"))
            .andExpect(status().isOk()).andExpect(header().string("Authorization","Bearer access"))
            .andExpect(jsonPath("$.user.profileSetupStatus").value("REQUIRED")).andExpect(jsonPath("$.tokens").doesNotExist());
    }
    @Test void legacySignupClosesWhenChapteredSignupEnabled() throws Exception {
        given(config.querySignupConfiguration()).willReturn(new SignupConfiguration(true,false));
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"user@gmail.com\",\"password\":\"abc@123\",\"nickname\":\"nick\",\"fixedCharacterId\":1}"))
            .andExpect(status().isGone()).andExpect(jsonPath("$.code").value("LEGACY_SIGNUP_DISABLED"));
        verifyNoInteractions(legacy);
    }
    @Test void callbackRedirectContainsNoCredentialsAndUsesBoundDevice() throws Exception {
        given(google.completeWeb("state","code",BINDING)).willReturn(new GoogleAuthenticationResult(new SignupProofResult(PROOF,new SignupProgress(SignupNextAction.AGREEMENTS,"user@gmail.com",null,null,Instant.now().plusSeconds(600))),"bound-device"));
        mvc.perform(get("/api/auth/oauth/google/callback").param("state","state").param("code","code").cookie(cookies()))
            .andExpect(status().isSeeOther()).andExpect(header().string("Location","https://www.pikume.com/auth/complete"))
            .andExpect(header().string("Referrer-Policy","no-referrer"));
        verifyNoInteractions(issue);
    }
    @Test void cancelledCallbackReturnsToFrontendWithBoundedPublicError() throws Exception {
        mvc.perform(get("/api/auth/oauth/google/callback").param("state","state").param("error","access_denied").cookie(cookies()))
            .andExpect(status().isSeeOther()).andExpect(header().string("Location","https://www.pikume.com/auth/complete?oauthError=GOOGLE_CANCELLED"));
        verify(google).failWeb("state",BINDING);
        verifyNoInteractions(issue);
    }
    @Test void replayCallbackReturnsToFrontendWithoutIssuingCredentials() throws Exception {
        given(google.completeWeb("state","code",BINDING)).willThrow(new com.pikume.back.user.auth.domain.exception.OAuthRequestException(com.pikume.back.user.auth.domain.exception.OAuthRequestException.Reason.REPLAY));
        mvc.perform(get("/api/auth/oauth/google/callback").param("state","state").param("code","code").cookie(cookies()))
            .andExpect(status().isSeeOther()).andExpect(header().string("Location","https://www.pikume.com/auth/complete?oauthError=OAUTH_REPLAY"));
        verifyNoInteractions(issue);
    }
    private Cookie[] cookies() {return new Cookie[]{new Cookie(SignupWebCredentials.BINDING,BINDING),new Cookie(SignupWebCredentials.CSRF,CSRF),new Cookie(SignupWebCredentials.PROOF,PROOF)};}
    private String emailBody() {return "{\"challengeId\":\"challenge\",\"email\":\"user@gmail.com\",\"code\":\"123456\",\"password\":\"abc@123\"}";}
}
