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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.blazebit.persistence.FullSelectCTECriteriaBuilder;
import com.blazebit.persistence.ReturningModificationCriteriaBuilderFactory;
import com.blazebit.persistence.SelectRecursiveCTECriteriaBuilder;

/**
 *
 * @author Christian Beikov
 * @author Moritz Becker
 * @since 1.1.0
 */
public class CTEManager extends CTEBuilderListenerImpl {

	private final MainQuery mainQuery;

    private final Set<CTEInfo> ctes;
    private boolean recursive = false;

    CTEManager(MainQuery mainQuery) {
    	this.mainQuery = mainQuery;
        this.ctes = new LinkedHashSet<CTEInfo>();
    }
    
    Set<CTEInfo> getCtes() {
    	return ctes;
    }

    public boolean hasCtes() {
        return ctes.size() > 0;
    }

	boolean isRecursive() {
		return recursive;
	}

    void buildClause(StringBuilder sb) {
        if (ctes.isEmpty()) {
        	return;
        }

        sb.append("WITH ");
        
        if (recursive) {
        	sb.append("RECURSIVE ");
        }
        
        boolean first = true;
        for (CTEInfo cte : ctes) {
        	if (first) {
        		first = false;
        	} else {
        		sb.append(", ");
        	}
        	
        	sb.append(cte.name);
        	sb.append('(');

        	final List<String> attributes = cte.attributes; 
    		sb.append(attributes.get(0));
    		
        	for (int i = 1; i < attributes.size(); i++) {
        		sb.append(", ");
        		sb.append(attributes.get(i));
        	}

        	sb.append(')');
        	
        	sb.append(" AS(\n");
        	sb.append(cte.nonRecursiveCriteriaBuilder.getQueryString());
        	
        	if (cte.recursive) {
        	    sb.append("\nUNION ALL\n");
        	    sb.append(cte.recursiveCriteriaBuilder.getQueryString());
        	}
        	
        	sb.append("\n)");
        }
        
        sb.append("\n");
    }

    @SuppressWarnings("unchecked")
	<Y> FullSelectCTECriteriaBuilder<Y> with(Class<?> cteClass, Y result) {
		FullSelectCTECriteriaBuilderImpl<Y> cteBuilder = new FullSelectCTECriteriaBuilderImpl<Y>(mainQuery, (Class<Object>) cteClass, result, this);
        this.onBuilderStarted(cteBuilder);
		return cteBuilder;
	}

	@SuppressWarnings("unchecked")
    <Y> SelectRecursiveCTECriteriaBuilder<Y> withRecursive(Class<?> cteClass, Y result) {
		recursive = true;
		RecursiveCTECriteriaBuilderImpl<Y> cteBuilder = new RecursiveCTECriteriaBuilderImpl<Y>(mainQuery, (Class<Object>) cteClass, result, this);
        this.onBuilderStarted(cteBuilder);
		return cteBuilder;
	}

	<Y> ReturningModificationCriteriaBuilderFactory<Y> withReturning(Class<?> cteClass, Y result) {
	    ReturningModificationCriteraBuilderFactoryImpl<Y> factory = new ReturningModificationCriteraBuilderFactoryImpl<Y>(mainQuery, cteClass, result, this);
		return factory;
	}
	
    @Override
	public void onBuilderEnded(CTEInfoBuilder builder) {
		super.onBuilderEnded(builder);
		ctes.add(builder.createCTEInfo());
	}

}