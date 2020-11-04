/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.handlers.api.processor;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.core.processor.provider.ProcessorSupplier;
import io.gravitee.gateway.handlers.api.flow.FlowPolicyChainProvider;
import io.gravitee.gateway.handlers.api.flow.api.ApiFlowResolver;
import io.gravitee.gateway.handlers.api.flow.plan.PlanFlowResolver;
import io.gravitee.gateway.handlers.api.path.impl.ApiPathResolverImpl;
import io.gravitee.gateway.handlers.api.policy.api.ApiPolicyChainProvider;
import io.gravitee.gateway.handlers.api.policy.api.ApiPolicyResolver;
import io.gravitee.gateway.handlers.api.policy.plan.PlanPolicyChainProvider;
import io.gravitee.gateway.handlers.api.policy.plan.PlanPolicyResolver;
import io.gravitee.gateway.handlers.api.processor.cors.CorsPreflightRequestProcessor;
import io.gravitee.gateway.handlers.api.processor.forward.XForwardedPrefixProcessor;
import io.gravitee.gateway.handlers.api.processor.logging.ApiLoggableRequestProcessor;
import io.gravitee.gateway.handlers.api.processor.pathparameters.PathParametersIndexProcessor;
import io.gravitee.gateway.policy.PolicyResolver;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.security.core.SecurityPolicyChainProvider;
import io.gravitee.gateway.security.core.SecurityPolicyResolver;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RequestProcessorChainFactory extends ApiProcessorChainFactory {

    @Value("${reporters.logging.max_size:-1}")
    private int maxSizeLogMessage;

    @Value("${reporters.logging.excluded_response_types:#{null}}")
    private String excludedResponseTypes;

    @Value("${handlers.request.headers.x-forwarded-prefix:false}")
    private boolean overrideXForwardedPrefix;

    @Override
    public void afterPropertiesSet() {
        ProcessorSupplier<ExecutionContext, StreamableProcessor<ExecutionContext, Buffer>> loggingDecorator = null;

        if (api.getProxy().getLogging() != null && api.getProxy().getLogging().getMode() != LoggingMode.NONE) {
            loggingDecorator = new ProcessorSupplier<>(() -> {
                ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(api.getProxy().getLogging());
                // log max size limit is in MB format
                // -1 means no limit
                processor.setMaxSizeLogMessage(maxSizeLogMessage);
                processor.setExcludedResponseTypes(excludedResponseTypes);

                return StreamableProcessor.toStreamable(processor);
            });

            add(loggingDecorator);
        }

        if (api.getProxy().getCors() != null && api.getProxy().getCors().isEnabled()) {
            add(new CorsPreflightRequestProcessor(api.getProxy().getCors()));
        }

        // Prepare security policy chain
        final PolicyResolver securityPolicyResolver = new SecurityPolicyResolver();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(securityPolicyResolver);
        add(new SecurityPolicyChainProvider(securityPolicyResolver));

        if (loggingDecorator != null) {
            add(loggingDecorator);
        }

        if (overrideXForwardedPrefix) {
            add(new XForwardedPrefixProcessor());
        }

        add(inject(new PathParametersIndexProcessor(new ApiPathResolverImpl(api))));

        if (api.getDefinitionVersion() == DefinitionVersion.V1) {
            add(new PlanPolicyChainProvider(StreamType.ON_REQUEST, new PlanPolicyResolver(api), chainFactory));
            add(new ApiPolicyChainProvider(StreamType.ON_REQUEST, new ApiPolicyResolver(), chainFactory));
        } else if (api.getDefinitionVersion() == DefinitionVersion.V2) {
            add(new FlowPolicyChainProvider(StreamType.ON_REQUEST, new PlanFlowResolver(api), chainFactory));
            add(new FlowPolicyChainProvider(StreamType.ON_REQUEST, new ApiFlowResolver(api), chainFactory));
        }
    }
}
