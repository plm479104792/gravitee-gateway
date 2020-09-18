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
package io.gravitee.gateway.handlers.api.flow.evaluation;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.handlers.api.flow.ConditionEvaluator;

/**
 * This {@link ConditionEvaluator} evaluates to true if the path of the request is matching the
 * path declared within the {@link Flow} depending on the {@link io.gravitee.definition.model.flow.Operator}
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathBasedConditionEvaluator implements ConditionEvaluator {

    @Override
    public boolean evaluate(Flow flow, ExecutionContext context) {
        return (flow.getOperator() == Operator.EQUALS) ?
                context.request().pathInfo().equals(flow.getPath()) :
                context.request().pathInfo().startsWith(flow.getPath());
    }

}
