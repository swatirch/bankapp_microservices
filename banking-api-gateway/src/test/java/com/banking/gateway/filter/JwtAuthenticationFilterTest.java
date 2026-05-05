package com.banking.gateway.filter;

import com.banking.gateway.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

        @Mock
        private JwtUtil jwtUtil;

        @Mock
        private GatewayFilterChain filterChain;

        @InjectMocks
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Test
        void shouldAllowRequestToPublicPath_register() {
                when(filterChain.filter(any())).thenReturn(Mono.empty());

                MockServerHttpRequest request = MockServerHttpRequest
                                .post("/api/auth/register")
                                .build();
                MockServerWebExchange exchange = MockServerWebExchange.from(request);

                StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                                .verifyComplete();

                verify(filterChain, times(1)).filter(any());
                verify(jwtUtil, never()).isTokenValid(any());
        }

        @Test
        void shouldAllowRequestToPublicPath_login() {
                when(filterChain.filter(any())).thenReturn(Mono.empty());

                MockServerHttpRequest request = MockServerHttpRequest
                                .post("/api/auth/login")
                                .build();
                MockServerWebExchange exchange = MockServerWebExchange.from(request);

                StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                                .verifyComplete();

                verify(filterChain, times(1)).filter(any());
                verify(jwtUtil, never()).isTokenValid(any());
        }

        @Test
        void shouldReturn401WhenAuthorizationHeaderMissing() {
                MockServerHttpRequest request = MockServerHttpRequest
                                .get("/api/accounts/acc-123")
                                .build();
                MockServerWebExchange exchange = MockServerWebExchange.from(request);

                StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                                .verifyComplete();

                assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
                verify(filterChain, never()).filter(any());
        }

        @Test
        void shouldReturn401WhenTokenIsInvalid() {
                MockServerHttpRequest request = MockServerHttpRequest
                                .get("/api/accounts/acc-123")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                                .build();
                MockServerWebExchange exchange = MockServerWebExchange.from(request);

                when(jwtUtil.isTokenValid("invalid-token")).thenReturn(false);

                StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                                .verifyComplete();

                assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
                verify(filterChain, never()).filter(any());
        }

        @Test
        void shouldAllowRequestWhenTokenIsValid() {
                when(filterChain.filter(any())).thenReturn(Mono.empty());

                MockServerHttpRequest request = MockServerHttpRequest
                                .get("/api/accounts/acc-123")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                                .build();
                MockServerWebExchange exchange = MockServerWebExchange.from(request);

                when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
                when(jwtUtil.extractUsername("valid-token")).thenReturn("user@example.com");

                StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                                .verifyComplete();

                verify(filterChain, times(1)).filter(any());
        }

        @Test
        void shouldReturn401WhenBearerPrefixMissing() {
                MockServerHttpRequest request = MockServerHttpRequest
                                .get("/api/accounts/acc-123")
                                .header(HttpHeaders.AUTHORIZATION, "valid-token")
                                .build();
                MockServerWebExchange exchange = MockServerWebExchange.from(request);

                StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                                .verifyComplete();

                assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
                verify(filterChain, never()).filter(any());
        }

        @Test
        void shouldAddAuthenticatedUserHeaderWhenTokenValid() {
                when(filterChain.filter(any())).thenReturn(Mono.empty());

                MockServerHttpRequest request = MockServerHttpRequest
                                .get("/api/transactions/acc-123")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                                .build();
                MockServerWebExchange exchange = MockServerWebExchange.from(request);

                when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
                when(jwtUtil.extractUsername("valid-token")).thenReturn("user@example.com");

                StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                                .verifyComplete();

                verify(filterChain, times(1)).filter(any());
                verify(jwtUtil, times(1)).extractUsername("valid-token");
        }
}