/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.netmgt.filter.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Optimizes the filter AST via a post-order rewrite to produce faster SQL.
 * <p>
 * Implemented optimizations:
 * <ul>
 *   <li>Flatten nested AND/OR so (a AND b) AND c → a AND b AND c</li>
 *   <li>Deduplicate AND terms: identical expressions in an AndExpr are kept once</li>
 *   <li>AND of CatincInExpr/CatincExpr → CatincAndInExpr (one EXISTS with multiple JOINs)</li>
 *   <li>Or of CatincExpr → CatincInExpr (one EXISTS with categoryName IN (...))</li>
 *   <li>Or of IsServiceExpr → IsServiceInExpr (serviceName IN (...))</li>
 * </ul>
 */
public final class FilterOptimizer {

    /**
     * Optimize the tree rooted at {@code root}.
     *
     * @param root root expression (may be null)
     * @return optimized tree, or null if root was null
     */
    public Expr optimize(Expr root) {
        if (root == null) {
            return null;
        }
        return rewrite(root);
    }

    private Expr rewrite(Expr e) {
        if (e instanceof AndExpr) {
            List<Expr> rewritten = ((AndExpr) e).getChildren().stream()
                    .map(this::rewrite)
                    .collect(Collectors.toList());
            List<Expr> flattened = new ArrayList<>();
            for (Expr r : rewritten) {
                if (r instanceof AndExpr) {
                    flattened.addAll(((AndExpr) r).getChildren());
                } else {
                    flattened.add(r);
                }
            }
            List<Expr> deduped = new ArrayList<>();
            for (Expr term : flattened) {
                if (deduped.stream().noneMatch(existing -> existing.equals(term))) {
                    deduped.add(term);
                }
            }
            if (deduped.size() == 1) {
                return deduped.get(0);
            }
            List<List<String>> categoryGroups = new ArrayList<>();
            for (Expr term : deduped) {
                if (term instanceof CatincExpr) {
                    categoryGroups.add(List.of(((CatincExpr) term).getCategoryName()));
                } else if (term instanceof CatincInExpr) {
                    categoryGroups.add(new ArrayList<>(((CatincInExpr) term).getCategoryNames()));
                } else {
                    categoryGroups = null;
                    break;
                }
            }
            if (categoryGroups != null && categoryGroups.size() >= 2) {
                return new CatincAndInExpr(categoryGroups);
            }
            if (categoryGroups != null && categoryGroups.size() == 1) {
                List<String> names = categoryGroups.get(0);
                return names.size() == 1 ? new CatincExpr(names.get(0)) : new CatincInExpr(names);
            }
            return new AndExpr(deduped);
        }
        if (e instanceof OrExpr) {
            OrExpr or = (OrExpr) e;
            List<Expr> rewritten = or.getChildren().stream()
                    .map(this::rewrite)
                    .collect(Collectors.toList());
            List<Expr> flattened = new ArrayList<>();
            for (Expr r : rewritten) {
                if (r instanceof OrExpr) {
                    flattened.addAll(((OrExpr) r).getChildren());
                } else {
                    flattened.add(r);
                }
            }
            List<String> catincNames = new ArrayList<>();
            for (Expr c : flattened) {
                if (c instanceof CatincExpr) {
                    catincNames.add(((CatincExpr) c).getCategoryName());
                } else if (c instanceof CatincInExpr) {
                    catincNames.addAll(((CatincInExpr) c).getCategoryNames());
                } else {
                    catincNames = null;
                    break;
                }
            }
            if (catincNames != null && catincNames.size() >= 2) {
                return new CatincInExpr(catincNames);
            }
            if (catincNames != null && catincNames.size() == 1) {
                return new CatincExpr(catincNames.get(0));
            }
            List<String> serviceNames = new ArrayList<>();
            for (Expr c : flattened) {
                if (!(c instanceof IsServiceExpr)) {
                    break;
                }
                serviceNames.add(((IsServiceExpr) c).getServiceName());
            }
            if (serviceNames.size() == flattened.size() && serviceNames.size() >= 2) {
                return new IsServiceInExpr(serviceNames);
            }
            return flattened.size() == 1 ? flattened.get(0) : new OrExpr(flattened);
        }
        if (e instanceof NotExpr) {
            return new NotExpr(rewrite(((NotExpr) e).getChild()));
        }
        if (e instanceof ComparisonExpr) {
            ComparisonExpr comp = (ComparisonExpr) e;
            return new ComparisonExpr(
                    rewrite(comp.getLeft()),
                    comp.getOp(),
                    rewrite(comp.getRight()));
        }
        if (e instanceof IplikeExpr) {
            IplikeExpr iplike = (IplikeExpr) e;
            return new IplikeExpr(rewrite(iplike.getLeft()), rewrite(iplike.getRight()));
        }
        if (e instanceof InExpr) {
            InExpr in = (InExpr) e;
            List<Expr> values = in.getValues().stream().map(this::rewrite).collect(Collectors.toList());
            return new InExpr(rewrite(in.getLeft()), values);
        }
        if (e instanceof IsNullExpr) {
            IsNullExpr isNull = (IsNullExpr) e;
            return new IsNullExpr(rewrite(isNull.getExpr()), isNull.isNotNull());
        }
        return e;
    }
}
