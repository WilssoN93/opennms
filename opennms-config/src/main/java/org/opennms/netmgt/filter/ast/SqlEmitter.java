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

import org.opennms.netmgt.config.api.DatabaseSchemaConfig;
import org.opennms.netmgt.config.filter.Table;
import org.opennms.netmgt.filter.api.FilterParseException;

/**
 * Visitor that emits a SQL WHERE-clause fragment for each AST node.
 * Uses the same SQL shape as the legacy rule expansion in JdbcFilterDao.
 */
public class SqlEmitter implements ExprVisitor<String> {

    private final List<Table> tables;
    private final DatabaseSchemaConfig schemaConfig;
    private final List<String> extractedStrings;

    public SqlEmitter(List<Table> tables, DatabaseSchemaConfig schemaConfig, List<String> extractedStrings) {
        this.tables = tables;
        this.schemaConfig = schemaConfig;
        this.extractedStrings = extractedStrings != null ? extractedStrings : List.of();
    }

    @Override
    public String visitAnd(AndExpr e) {
        return e.getChildren().stream()
                .map(c -> "(" + c.accept(this) + ")")
                .collect(Collectors.joining(" AND "));
    }

    @Override
    public String visitOr(OrExpr e) {
        return e.getChildren().stream()
                .map(c -> "(" + c.accept(this) + ")")
                .collect(Collectors.joining(" OR "));
    }

    @Override
    public String visitNot(NotExpr e) {
        return "NOT (" + e.getChild().accept(this) + ")";
    }

    @Override
    public String visitCatinc(CatincExpr e) throws FilterParseException {
        String name = e.getCategoryName().replace("'", "''");
        String nodeId = schemaConfig.addColumn(tables, "nodeID");
        return "EXISTS (SELECT 1 FROM category_node cn JOIN categories c ON c.categoryID = cn.categoryID "
                + "WHERE c.categoryName = '" + name + "' AND cn.nodeID = " + nodeId + ")";
    }

    @Override
    public String visitCatincIn(CatincInExpr e) throws FilterParseException {
        String inList = e.getCategoryNames().stream()
                .map(s -> "'" + s.replace("'", "''") + "'")
                .collect(Collectors.joining(", "));
        String nodeId = schemaConfig.addColumn(tables, "nodeID");
        return "EXISTS (SELECT 1 FROM category_node cn JOIN categories c ON c.categoryID = cn.categoryID "
                + "WHERE c.categoryName IN (" + inList + ") AND cn.nodeID = " + nodeId + ")";
    }

    @Override
    public String visitCatincAndIn(CatincAndInExpr e) throws FilterParseException {
        String nodeId = schemaConfig.addColumn(tables, "nodeID");
        StringBuilder sb = new StringBuilder("EXISTS (SELECT 1 FROM category_node cn1 JOIN categories c1 ON c1.categoryID = cn1.categoryID");
        List<String> whereClauses = new ArrayList<>();
        String inList0 = e.getCategoryGroups().get(0).stream()
                .map(s -> "'" + s.replace("'", "''") + "'")
                .collect(Collectors.joining(", "));
        whereClauses.add("c1.categoryName IN (" + inList0 + ")");
        for (int i = 1; i < e.getCategoryGroups().size(); i++) {
            String cn = "cn" + (i + 1);
            String c = "c" + (i + 1);
            sb.append(" JOIN category_node ").append(cn).append(" ON cn1.nodeID = ").append(cn).append(".nodeID");
            sb.append(" JOIN categories ").append(c).append(" ON ").append(c).append(".categoryID = ").append(cn).append(".categoryID");
            String inList = e.getCategoryGroups().get(i).stream()
                    .map(s -> "'" + s.replace("'", "''") + "'")
                    .collect(Collectors.joining(", "));
            whereClauses.add(c + ".categoryName IN (" + inList + ")");
        }
        whereClauses.add("cn1.nodeID = " + nodeId);
        sb.append(" WHERE ").append(String.join(" AND ", whereClauses)).append(")");
        return sb.toString();
    }

    @Override
    public String visitIsService(IsServiceExpr e) throws FilterParseException {
        String col = schemaConfig.addColumn(tables, "serviceName");
        String name = e.getServiceName().replace("'", "''");
        return col + " = '" + name + "'";
    }

    @Override
    public String visitIsServiceIn(IsServiceInExpr e) throws FilterParseException {
        String col = schemaConfig.addColumn(tables, "serviceName");
        String inList = e.getServiceNames().stream()
                .map(s -> "'" + s.replace("'", "''") + "'")
                .collect(Collectors.joining(", "));
        return col + " IN (" + inList + ")";
    }

    @Override
    public String visitNotIsService(NotIsServiceExpr e) throws FilterParseException {
        String ipAddrCol = schemaConfig.addColumn(tables, "ipAddr");
        String name = e.getServiceName().replace("'", "''");
        return ipAddrCol + " NOT IN (SELECT ifServices.ipAddr FROM ifServices, service WHERE service.serviceName ='"
                + name + "' AND service.serviceID = ifServices.serviceID)";
    }

    @Override
    public String visitColumnRef(ColumnRefExpr e) throws FilterParseException {
        return schemaConfig.addColumn(tables, e.getColumnName());
    }

    @Override
    public String visitPlaceholder(PlaceholderExpr e) {
        int idx = Integer.parseInt(e.getIndex());
        if (idx < 0 || idx >= extractedStrings.size()) {
            return "###@" + e.getIndex() + "@###";
        }
        return extractedStrings.get(idx);
    }

    @Override
    public String visitComparison(ComparisonExpr e) throws FilterParseException {
        String leftSql = e.getLeft().accept(this);
        String rightSql = e.getRight().accept(this);
        return leftSql + " " + e.getOp() + " " + rightSql;
    }

    @Override
    public String visitStringLiteral(StringLiteralExpr e) {
        return "'" + e.getValue().replace("'", "''") + "'";
    }

    @Override
    public String visitIplike(IplikeExpr e) throws FilterParseException {
        String leftSql = e.getLeft().accept(this);
        String rightSql;
        // Operator form "col IPLIKE pattern" may have unquoted pattern tokenized as identifier (ColumnRefExpr).
        if (e.getRight() instanceof ColumnRefExpr) {
            String pattern = ((ColumnRefExpr) e.getRight()).getColumnName();
            rightSql = "'" + pattern.replace("'", "''") + "'";
        } else {
            rightSql = e.getRight().accept(this);
        }
        return "IPLIKE(" + leftSql + ", " + rightSql + ")";
    }

    @Override
    public String visitIn(InExpr e) throws FilterParseException {
        String leftSql = e.getLeft().accept(this);
        String inList = e.getValues().stream()
                .map(v -> {
                    try {
                        return v.accept(this);
                    } catch (FilterParseException ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .collect(Collectors.joining(", "));
        return leftSql + " IN (" + inList + ")";
    }

    @Override
    public String visitIsNull(IsNullExpr e) throws FilterParseException {
        String exprSql = e.getExpr().accept(this);
        return e.isNotNull() ? exprSql + " IS NOT NULL" : exprSql + " IS NULL";
    }
}
