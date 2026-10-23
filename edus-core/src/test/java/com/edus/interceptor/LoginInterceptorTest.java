package com.edus.interceptor;

import com.edus.auth.service.SignatureService;
import com.edus.auth.service.TokenService;
import com.edus.common.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginInterceptorTest {

    @AfterEach
    void clearUserContext() {
        UserContext.remove();
    }

    @Test
    void scoreboardPermitAllAllowsRequestWithoutToken() throws Exception {
        TokenService tokenService = mock(TokenService.class);
        SignatureService signatureService = mock(SignatureService.class);
        LoginInterceptor interceptor = new LoginInterceptor(tokenService, signatureService, true);
        HttpServletRequest request = request("/scoreboard/matches");
        HttpServletResponse response = mock(HttpServletResponse.class);

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals("scoreboard-dev", UserContext.get().getName());
        verify(request).setAttribute("authType", "scoreboard-permit-all");
        verifyNoInteractions(tokenService, signatureService);
    }

    @Test
    void scoreboardPermitAllDoesNotOpenOtherBusinessApis() throws Exception {
        TokenService tokenService = mock(TokenService.class);
        SignatureService signatureService = mock(SignatureService.class);
        LoginInterceptor interceptor = new LoginInterceptor(tokenService, signatureService, true);
        HttpServletRequest request = request("/user/list");
        HttpServletResponse response = unauthorizedResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void disabledPermitAllStillRequiresScoreboardAuthentication() throws Exception {
        TokenService tokenService = mock(TokenService.class);
        SignatureService signatureService = mock(SignatureService.class);
        LoginInterceptor interceptor = new LoginInterceptor(tokenService, signatureService, false);
        HttpServletRequest request = request("/scoreboard/group-rules");
        HttpServletResponse response = unauthorizedResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
    }

    private HttpServletRequest request(String servletPath) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn(servletPath);
        return request;
    }

    private HttpServletResponse unauthorizedResponse() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        return response;
    }
}
