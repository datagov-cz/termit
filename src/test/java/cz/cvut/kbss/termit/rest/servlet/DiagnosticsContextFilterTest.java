/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.rest.servlet;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.security.model.AuthenticationToken;
import cz.cvut.kbss.termit.security.model.TermItUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class DiagnosticsContextFilterTest {

    @Mock
    private HttpServletRequest requestMock;

    @Mock
    private HttpServletResponse responseMock;

    @Mock
    private FilterChain chainMock;

    private final DiagnosticsContextFilter filter = new DiagnosticsContextFilter();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void setsDiagnosticsContextWhenProcessingChain() throws Exception {
        final UserAccount user = Generator.generateUserAccount();
        final Principal token = new AuthenticationToken(Collections.emptyList(), new TermItUserDetails(user));
        when(requestMock.getUserPrincipal()).thenReturn(token);
        doAnswer((answer) -> {
            assertEquals(user.getUsername(), MDC.get(DiagnosticsContextFilter.MDC_KEY));
            return null;
        }).when(chainMock).doFilter(requestMock, responseMock);

        filter.doFilter(requestMock, responseMock, chainMock);
        verify(chainMock).doFilter(requestMock, responseMock);
    }

    @Test
    void doesNotSetDiagnosticsContextForAnonymousPrincipal() throws Exception {
        when(requestMock.getUserPrincipal()).thenReturn(null);
        doAnswer((answer) -> {
            assertNull(MDC.get(DiagnosticsContextFilter.MDC_KEY));
            return null;
        }).when(chainMock).doFilter(requestMock, responseMock);

        filter.doFilter(requestMock, responseMock, chainMock);
        verify(chainMock).doFilter(requestMock, responseMock);
    }
}
