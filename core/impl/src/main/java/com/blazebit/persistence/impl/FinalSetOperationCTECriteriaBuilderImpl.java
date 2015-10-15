/*
 * Copyright 2015 Blazebit.
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
package com.blazebit.persistence.impl;

import java.util.List;

import com.blazebit.persistence.FinalSetOperationCTECriteriaBuilder;
import com.blazebit.persistence.spi.SetOperationType;

/**
 *
 * @param <T> The query result type
 * @author Christian Beikov
 * @since 1.1.0
 */
public class FinalSetOperationCTECriteriaBuilderImpl<T> extends BaseFinalSetOperationBuilderImpl<T, FinalSetOperationCTECriteriaBuilder<T>, FinalSetOperationCTECriteriaBuilderImpl<T>> implements FinalSetOperationCTECriteriaBuilder<T>, CTEInfoBuilder {

    private final T result;
    private final CTEBuilderListener listener;
    private final FullSelectCTECriteriaBuilderImpl<?> initiator;
    
    public FinalSetOperationCTECriteriaBuilderImpl(MainQuery mainQuery, Class<T> clazz, T result, SetOperationType operator, boolean nested, CTEBuilderListener listener, FullSelectCTECriteriaBuilderImpl<?> initiator) {
        super(mainQuery, false, clazz, operator, nested);
        this.result = result;
        this.listener = listener;
        this.initiator = initiator;
    }
    
    public FullSelectCTECriteriaBuilderImpl<?> getInitiator() {
        return initiator;
    }

    @Override
    public T end() {
        listener.onBuilderEnded(this);
        return result;
    }

    @Override
    public CTEInfo createCTEInfo() {
        List<String> attributes = initiator.prepareAndGetAttributes();
        CTEInfo info = new CTEInfo(initiator.cteName, initiator.cteType, attributes, false, false, this, null);
        return info;
    }

}