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

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<%@ page contentType="text/html;charset=UTF-8" language="java" import="org.opennms.web.api.Util" %>

<%@ page import="org.opennms.web.utils.Bootstrap" %>
<% Bootstrap.with(pageContext)
          .headTitle("List")
          .headTitle("Thresholds")
          .headTitle("Admin")
          .breadcrumb("Admin", "admin/index.jsp")
          .breadcrumb("Threshold Groups")
          .build(request);
%>
<jsp:directive.include file="/includes/bootstrap.jsp" />

<div class="card">
  <div class="card-header">
    <span>Threshold Configuration</span>
  </div>
    <form method="post" name="allGroups">
      <table class="table table-sm table-striped table-hover">
        <tr>
                <th>Name</th>
                <th>RRD Repository</th>
                <th>&nbsp;</th>
        </tr>
        <c:forEach var="mapEntry" items="${groupMap}">
                <tr>
                        <td>${mapEntry.key}</td>
                        <td>${mapEntry.value.rrdRepository}</td>
                        <td><a href="<%= Util.calculateUrlBase(request, "admin/thresholds/index.htm") %>?groupName=${mapEntry.key}&editGroup">Edit</a></td>
                </tr>
        </c:forEach>
      </table>
    </form>
</div> <!-- panel -->

<script type="text/javascript">
function doReload() {
    if (confirm("Are you sure you want to do this?")) {
        document.location = "<%= Util.calculateUrlBase(request, "admin/thresholds/index.htm") %>?reloadThreshdConfig";
    }
}
</script>
<button type="button" class="btn btn-secondary mb-4" onclick="doReload()">Reload Threshold Configuration</button>
<jsp:include page="/includes/bootstrap-footer.jsp" flush="false" />
