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

package com.linecorp.armeria.server.http.dynamic;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import com.linecorp.armeria.common.http.DefaultHttpResponse;
import com.linecorp.armeria.common.http.HttpMethod;
import com.linecorp.armeria.common.http.HttpRequest;
import com.linecorp.armeria.common.http.HttpResponse;
import com.linecorp.armeria.common.http.HttpStatus;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.http.AbstractHttpService;
import com.linecorp.armeria.server.http.HttpService;

/**
 * An {@link HttpService} that serves dynamic contents.
 */
public class DynamicHttpService extends AbstractHttpService {

    private final List<DynamicHttpFunctionEntry> entries;

    /**
     * Create a {@link DynamicHttpService} instance.
     */
    protected DynamicHttpService() {
        this.entries = ImmutableList.copyOf(Methods.entries(this, Collections.emptyMap()));
    }

    DynamicHttpService(Iterable<DynamicHttpFunctionEntry> entries) {
        this.entries = ImmutableList.copyOf(entries);
    }

    @Override
    public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
        HttpMethod method = req.method();
        String mappedPath = ctx.mappedPath();

        for (DynamicHttpFunctionEntry entry : entries) {
            MappedDynamicFunction mappedDynamicFunction = entry.bind(method, mappedPath);

            if (mappedDynamicFunction != null) {
                return mappedDynamicFunction.serve(ctx, req);
            }
        }

        DefaultHttpResponse res = new DefaultHttpResponse();
        res.respond(HttpStatus.NOT_FOUND);
        return res;
    }
}
