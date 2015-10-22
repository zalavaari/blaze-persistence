/*
 * Copyright 2014 Blazebit.
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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import javax.persistence.metamodel.Attribute;

import com.blazebit.persistence.JoinType;
import com.blazebit.persistence.impl.expression.Expression;
import com.blazebit.persistence.impl.expression.FunctionExpression;
import com.blazebit.persistence.impl.expression.PathExpression;
import com.blazebit.persistence.impl.expression.VisitorAdapter;
import com.blazebit.persistence.impl.predicate.AndPredicate;
import com.blazebit.persistence.impl.predicate.EqPredicate;
import com.blazebit.persistence.impl.predicate.Predicate;

/**
 *
 * @author Christian Beikov
 * @author Moritz Becker
 * @since 1.0
 */
public class JoinNode {

    private JoinAliasInfo aliasInfo;
    private JoinType type = JoinType.LEFT;
    private boolean fetch = false;

    // We need this for count and id queries where we do not need all the joins
    private final EnumSet<ClauseType> clauseDependencies = EnumSet.noneOf(ClauseType.class);

    private final JoinNode parent;
    private final JoinTreeNode parentTreeNode;

    private final Class<?> propertyClass;
    private final Map<String, JoinTreeNode> nodes = new TreeMap<String, JoinTreeNode>(); // Use TreeMap so that joins get applied
                                                                                         // alphabetically for easier testing

    // contains other join nodes which this node depends on
    private final Set<JoinNode> dependencies = new HashSet<JoinNode>();

    private AndPredicate onPredicate;
    
    // Cache
    private boolean dirty = true;
    private boolean cardinalityMandatory;

    public JoinNode(JoinNode parent, JoinTreeNode parentTreeNode, JoinAliasInfo aliasInfo, JoinType type, Class<?> propertyClass) {
        this.parent = parent;
        this.parentTreeNode = parentTreeNode;
        this.aliasInfo = aliasInfo;
        this.type = type;
        this.propertyClass = propertyClass;
        onUpdate(null);
    }
    
    private void onUpdate(StateChange stateChange) {
        // Once mandatory, only a type change can cause a change of the cardinality mandatory
        if (cardinalityMandatory && stateChange != StateChange.JOIN_TYPE) {
            return;
        }
        
        dirty = true;
        if (parent != null) {
            parent.onUpdate(StateChange.CHILD);
        }
    }

    public boolean isCardinalityMandatory() {
        if (dirty) {
            updateCardinalityMandatory();
            dirty = false;
        }
        
        return cardinalityMandatory;
    }
    
    private void updateCardinalityMandatory() {
        boolean computedMandatory = false;
        if (type == JoinType.INNER) {
            // If the relation is optional/nullable or the join has a condition
            // the join is mandatory, because doing omitting it might change the semantics result set 
            if (parentTreeNode.isOptional() || !isEmptyCondition()) {
                computedMandatory = true;
            }
        } else if (type == JoinType.LEFT) {
            // If the join has a condition which is not an array expression condition
            // we definitively need the join
            // NOTE: an array expression condition with a left join will always produce 1 row
            // so the join is not yet absolutely mandatory 
            if (!isEmptyCondition() && !isArrayExpressionCondition()) {
                computedMandatory = true;
            }

            // Check if any of the child nodes is mandatory for the cardinality
            OUTER: for (Map.Entry<String, JoinTreeNode> nodeEntry : nodes.entrySet()) {
                JoinTreeNode treeNode = nodeEntry.getValue();

                for (JoinNode childNode : treeNode.getJoinNodes().values()) {
                    if (childNode.isCardinalityMandatory()) {
                        computedMandatory = true;
                        break OUTER;
                    }
                }
            }
        }
        
        if (computedMandatory != cardinalityMandatory) {
            cardinalityMandatory = computedMandatory;
        }
    }
    
    private boolean isEmptyCondition() {
        return onPredicate == null || onPredicate.getChildren().isEmpty();
    }
    
    private boolean isArrayExpressionCondition() {
        if (onPredicate == null || onPredicate.getChildren().size() != 1) {
            return false;
        }

        Predicate predicate = onPredicate.getChildren().get(0);
        if (!(predicate instanceof EqPredicate)) {
            return false;
        }

        EqPredicate eqPredicate = (EqPredicate) predicate;
        Expression left = eqPredicate.getLeft();
        if (!(left instanceof FunctionExpression)) {
            return false;
        }

        FunctionExpression keyExpression = (FunctionExpression) left;
        if (!"KEY".equalsIgnoreCase(keyExpression.getFunctionName())) {
            return false;
        }

        Expression keyContentExpression = keyExpression.getExpressions().get(0);
        if (!(keyContentExpression instanceof PathExpression)) {
            return false;
        }

        PathExpression keyPath = (PathExpression) keyContentExpression;
        if (!this.equals(keyPath.getBaseNode())) {
            return false;
        }

        return true;
    }

    public void registerDependencies() {
        if (onPredicate != null) {
            onPredicate.accept(new VisitorAdapter() {

                @Override
                public void visit(PathExpression pathExpr) {
                    // prevent loop dependencies to the same join node
                    if (pathExpr.getBaseNode() != JoinNode.this && pathExpr.getBaseNode() != null) {
                        dependencies.add((JoinNode) pathExpr.getBaseNode());
                    }
                }
            });
        }
    }

    public void accept(JoinNodeVisitor visitor) {
        visitor.visit(this);
        for (JoinTreeNode treeNode : nodes.values()) {
            for (JoinNode joinNode : treeNode.getJoinNodes().values()) {
                joinNode.accept(visitor);
            }
        }
    }

    public <T> T accept(AbortableResultJoinNodeVisitor<T> visitor) {
        T result = visitor.visit(this);

        if (visitor.getStopValue().equals(result)) {
            return result;
        }

        for (JoinTreeNode treeNode : nodes.values()) {
            for (JoinNode joinNode : treeNode.getJoinNodes().values()) {
                result = joinNode.accept(visitor);

                if (visitor.getStopValue().equals(result)) {
                    return result;
                }
            }
        }

        return null;
    }

    public EnumSet<ClauseType> getClauseDependencies() {
        return clauseDependencies;
    }

    public JoinTreeNode getParentTreeNode() {
        return parentTreeNode;
    }

    public JoinNode getParent() {
        return parent;
    }

    public JoinAliasInfo getAliasInfo() {
        return aliasInfo;
    }

    public void setAliasInfo(JoinAliasInfo aliasInfo) {
        this.aliasInfo = aliasInfo;
    }

    public JoinType getType() {
        return type;
    }

    public void setType(JoinType type) {
        this.type = type;
        onUpdate(StateChange.JOIN_TYPE);
    }

    public boolean isFetch() {
        return fetch;
    }

    public void setFetch(boolean fetch) {
        this.fetch = fetch;
    }

    public Map<String, JoinTreeNode> getNodes() {
        return nodes;
    }

    public JoinTreeNode getOrCreateTreeNode(String joinRelationName, Attribute<?, ?> attribute) {
        JoinTreeNode node = nodes.get(joinRelationName);

        if (node == null) {
            node = new JoinTreeNode(joinRelationName, attribute);
            nodes.put(joinRelationName, node);
        }

        return node;
    }

    public Class<?> getPropertyClass() {
        return propertyClass;
    }

    public AndPredicate getOnPredicate() {
        return onPredicate;
    }

    public void setOnPredicate(AndPredicate onPredicate) {
        this.onPredicate = onPredicate;
        onUpdate(StateChange.ON_PREDICATE);
    }

    public Set<JoinNode> getDependencies() {
        return dependencies;
    }

    public boolean hasCollections() {
        Stack<JoinTreeNode> stack = new Stack<JoinTreeNode>();
        stack.addAll(nodes.values());

        while (!stack.isEmpty()) {
            JoinTreeNode treeNode = stack.pop();

            if (treeNode.isCollection()) {
                return true;
            }

            for (JoinNode joinNode : treeNode.getJoinNodes().values()) {
                stack.addAll(joinNode.nodes.values());
            }
        }

        return false;
    }
    
    private static enum StateChange {
        JOIN_TYPE,
        ON_PREDICATE,
        CHILD;
    }
}
