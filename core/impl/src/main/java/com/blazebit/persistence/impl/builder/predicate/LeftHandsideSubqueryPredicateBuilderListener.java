/*
 * Copyright 2014 - 2018 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blazebit.persistence.impl.builder.predicate;

import com.blazebit.persistence.impl.SubqueryBuilderListenerImpl;
import com.blazebit.persistence.impl.SubqueryInternalBuilder;
import com.blazebit.persistence.impl.expression.SubqueryExpression;

/**
 *
 * @author Moritz Becker
 * @since 1.0.0
 */
public class LeftHandsideSubqueryPredicateBuilderListener<T extends LeftHandsideSubqueryPredicateBuilder> extends SubqueryBuilderListenerImpl<T> {

    @Override
    public void onBuilderEnded(SubqueryInternalBuilder<T> builder) {
        super.onBuilderEnded(builder);
        T leftHandsideSubqueryPredicateBuilder = builder.getResult();
        leftHandsideSubqueryPredicateBuilder.setLeftExpression(new SubqueryExpression(builder));
    }
}
