/*
 * Copyright 2016 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.armeria.server.http.auth;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.google.common.collect.Lists;

import com.linecorp.armeria.common.http.HttpHeaders;
import com.linecorp.armeria.common.http.HttpRequest;
import com.linecorp.armeria.common.http.HttpResponse;
import com.linecorp.armeria.server.Service;

/**
 * Builds a new {@link HttpAuthService}.
 */
public final class HttpAuthServiceBuilder {

    private final List<Authorizer<HttpRequest>> authorizers = new ArrayList<>();

    /**
     * Adds an {@link Authorizer}.
     */
    public HttpAuthServiceBuilder add(Authorizer<HttpRequest> authorizer) {
        authorizers.add(requireNonNull(authorizer, "authorizer"));
        return this;
    }

    /**
     * Adds multiple {@link Authorizer}s.
     */
    public HttpAuthServiceBuilder add(Iterable<? extends Authorizer<HttpRequest>> authorizers) {
        this.authorizers.addAll(Lists.newArrayList(requireNonNull(authorizers, "authorizers")));
        return this;
    }

    /**
     * Adds an HTTP basic {@link Authorizer}.
     */
    public HttpAuthServiceBuilder addBasicAuth(Authorizer<? super BasicToken> authorizer) {
        this.authorizers.add(
                tokenAuthorizer(AuthTokenExtractors.BASIC, requireNonNull(authorizer, "authorizer")));
        return this;
    }

    /**
     * Adds an OAuth1a {@link Authorizer}.
     */
    public HttpAuthServiceBuilder addOAuth1a(Authorizer<? super OAuth1aToken> authorizer) {
        this.authorizers.add(
                tokenAuthorizer(AuthTokenExtractors.OAUTH1A, requireNonNull(authorizer, "authorizer")));
        return this;
    }

    /**
     * Adds an OAuth2 {@link Authorizer}.
     */
    public HttpAuthServiceBuilder addOAuth2(Authorizer<? super OAuth2Token> authorizer) {
        this.authorizers.add(
                tokenAuthorizer(AuthTokenExtractors.OAUTH2, requireNonNull(authorizer, "authorizer")));
        return this;
    }

    /**
     * Creates a new {@link HttpAuthService} instance with the given {@code delegate} and all of the
     * authorization {@link Authorizer}s.
     */
    public HttpAuthService build(Service<? super HttpRequest, ? extends HttpResponse> delegate) {
        return new HttpAuthServiceImpl(requireNonNull(delegate, "delegate"), authorizers);
    }

    /**
     * Creates a new {@link HttpAuthService} {@link Service} decorator that supports all of the given
     * authorization {@link Authorizer}s.
     */
    public Function<Service<? super HttpRequest, ? extends HttpResponse>, HttpAuthService> newDecorator() {
        return HttpAuthService.newDecorator(authorizers);
    }

    private <T> Authorizer<HttpRequest> tokenAuthorizer(
            Function<HttpHeaders, T> tokenExtractor, Authorizer<? super T> authorizer) {
        return (ctx, req) -> {
            T token = tokenExtractor.apply(req.headers());
            if (token == null) {
                return CompletableFuture.completedFuture(false);
            }
            return authorizer.authorize(ctx, token);
        };
    }
}
