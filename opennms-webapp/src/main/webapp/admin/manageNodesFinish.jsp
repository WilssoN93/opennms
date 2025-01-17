<%--
/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

--%>

<%@page language="java"
	contentType="text/html"
	session="true"
	import="org.opennms.core.utils.WebSecurityUtils,
		org.opennms.web.element.*,
		org.opennms.netmgt.model.OnmsNode
	"
%>

<%@ page import="org.opennms.web.utils.Bootstrap" %>
<% Bootstrap.with(pageContext)
          .headTitle("Manage Interfaces")
          .headTitle("Admin")
          .breadcrumb("Admin", "admin/index.jsp")
          .breadcrumb("Manage/Unmanage Interfaces Finish")
          .build(request);
%>
<jsp:directive.include file="/includes/bootstrap.jsp" />

<%
	OnmsNode node = null;
	String nodeIdString = request.getParameter("node");
	if (nodeIdString != null) {
		try {
			int nodeId = WebSecurityUtils.safeParseInt(nodeIdString);
			node = NetworkElementFactory.getInstance(getServletContext()).getNode(nodeId);
		} catch (NumberFormatException e) {
			// ignore this, we just won't put a link if it fails
		}
	}
%>

<div class="card">
  <div class="card-header">
    <span>Database Update Complete After Management Changes</span>
  </div>
  <div class="card-body">
    <p>
      These changes take effect immediately. OpenNMS does not need to be restarted.
    </p>

    <p>
      Changes for a specific node will become effective upon execution of
      a forced rescan on that node. The node must be up when rescanned for the
      inventory information to be updated.
    </p>

    <% if (node != null) { %>
    <p>
      <a href="element/rescan.jsp?node=<%= node.getId() %>">Rescan this node</a>
    </p>
    <p>
      <a href="element/node.jsp?node=<%= node.getId() %>">Return to node page</a>
    </p>
    <% } %>
  </div> <!-- card-body -->
</div> <!-- panel -->

<jsp:include page="/includes/bootstrap-footer.jsp" flush="true"/>
