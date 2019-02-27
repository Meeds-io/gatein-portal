package org.exoplatform.web.security.csrf;

import static org.exoplatform.web.security.csrf.CSRFTokenUtil.CSRF_TOKEN;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Test;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

public class CSRFTokenUtilTest {

    @After
    public void tearDown() {
        ConversationState.setCurrent(null);
    }

    @Test
    public void shouldNotGenerateTokenWhenNoConversationState() {
        String token = CSRFTokenUtil.getToken();

        assertNull(token);
    }

    @Test
    public void shouldGenerateTokenWhenConversationState() {
        startSessionAs("root");

        String token = CSRFTokenUtil.getToken();

        assertNotNull(token);
    }

    @Test
    public void shouldGetSameTokenWhenFetchingTokenTwice() {
        startSessionAs("root");

        String token1 = CSRFTokenUtil.getToken();
        String token2 = CSRFTokenUtil.getToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertEquals(token1, token2);
    }

    @Test
    public void shouldSucceedCheckWhenSameTokenInRequest() {
        startSessionAs("root");

        String token = CSRFTokenUtil.getToken();

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(CSRF_TOKEN)).thenReturn(token);

        boolean check = CSRFTokenUtil.check(request);

        assertTrue(check);
    }

    @Test
    public void shouldFailCheckWhenNoTokenInRequest() {
        startSessionAs("root");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(CSRF_TOKEN)).thenReturn(null);

        boolean check = CSRFTokenUtil.check(request);

        assertFalse(check);
    }

    @Test
    public void shouldFailCheckWhenWrongTokenInRequest() {
        startSessionAs("root");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(CSRF_TOKEN)).thenReturn("WrongToken");

        boolean check = CSRFTokenUtil.check(request);

        assertFalse(check);
    }

    protected void startSessionAs(String user) {
        Identity identity = new Identity(user, Collections.EMPTY_LIST);
        ConversationState state = new ConversationState(identity);
        ConversationState.setCurrent(state);
    }

}