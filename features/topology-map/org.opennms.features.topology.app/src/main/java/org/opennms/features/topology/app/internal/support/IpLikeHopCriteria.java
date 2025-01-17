/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014-2024 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2024 The OpenNMS Group, Inc.
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

package org.opennms.features.topology.app.internal.support;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.features.topology.api.GraphContainer;
import org.opennms.features.topology.api.support.hops.VertexHopCriteria;
import org.opennms.features.topology.api.topo.AbstractCollapsibleVertex;
import org.opennms.features.topology.api.topo.BackendGraph;
import org.opennms.features.topology.api.topo.GraphProvider;
import org.opennms.features.topology.api.topo.SearchCriteria;
import org.opennms.features.topology.api.topo.SearchResult;
import org.opennms.features.topology.api.topo.Vertex;
import org.opennms.features.topology.api.topo.VertexRef;
import org.opennms.features.topology.app.internal.IpInterfaceProvider;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This <Criteria> implementation supports the users selection of search results from an IPLIKE query
 * in the topology UI.
 * 
 * @author <a href=mailto:david@opennms.org>David Hustace</a>
 * @author <a href=mailto:thedesloge@opennms.org>Donald Desloge</a>
 * @author <a href=mailto:seth@opennms.org>Seth Leger</a>
 *
 */
public class IpLikeHopCriteria extends VertexHopCriteria implements SearchCriteria {

	public static final String NAMESPACE = "iplike";
	private final String m_ipQuery;
	
	private boolean m_collapsed = false;
	private IPVertex m_collapsedVertex;

	private IpInterfaceProvider ipInterfaceProvider;

	private GraphContainer m_graphContainer;

	private final Logger LOG = LoggerFactory.getLogger(IpLikeHopCriteria.class);

	@Override
	public String getSearchString() {
		return m_ipQuery;
	}

	public static class IPVertex extends AbstractCollapsibleVertex {

        public IPVertex(String id) {
			super(NAMESPACE, NAMESPACE + ":" + id, id);
			setIconKey("group");
		}
    }

	public IpLikeHopCriteria(SearchResult searchResult, IpInterfaceProvider ipInterfaceProvider, GraphContainer graphContainer) {
    	super(searchResult.getQuery());
    	m_collapsed = searchResult.isCollapsed();
        m_ipQuery = searchResult.getQuery();
        this.ipInterfaceProvider = Objects.requireNonNull(ipInterfaceProvider);
        m_collapsedVertex = new IPVertex(m_ipQuery);
	m_graphContainer = Objects.requireNonNull(graphContainer);
	Objects.requireNonNull(graphContainer.getTopologyServiceClient());
        m_collapsedVertex.setChildren(getVertices());
        setId(searchResult.getId());
    }

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_ipQuery == null) ? 0 : m_ipQuery.hashCode());
        result = prime * result
                + ((getNamespace() == null) ? 0 : getNamespace().hashCode());
        return result;
    }

	@Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof IpLikeHopCriteria) {
            IpLikeHopCriteria ref = (IpLikeHopCriteria)obj;
			return ref.m_ipQuery.equals(m_ipQuery) && ref.getNamespace().equals(getNamespace());
        }
        
        return false;
    }

	@Override
	public Set<VertexRef> getVertices() {
		
		CriteriaBuilder bldr = new CriteriaBuilder(OnmsIpInterface.class);

		bldr.iplike("ipAddr", m_ipQuery);
		final Set<Integer> nodeids = ipInterfaceProvider.findMatching(bldr.toCriteria()).stream().map(ip -> ip.getNode().getId()).collect(Collectors.toSet());
		LOG.debug("getVertices: nodeids: {}", nodeids);
		final GraphProvider graphProvider = m_graphContainer.getTopologyServiceClient().getGraphProviderBy(m_graphContainer.getTopologyServiceClient().getNamespace());
		final BackendGraph currentGraph = graphProvider.getCurrentGraph();
		return currentGraph.getVertices().stream().filter(v -> v.getNodeID() != null && nodeids.contains(v.getNodeID())).collect(Collectors.toSet());
	}

	@Override
	public boolean isCollapsed() {
		return m_collapsed;
	}

	@Override
	public void setCollapsed(boolean collapsed) {
		if (collapsed != isCollapsed()) {
			this.m_collapsed = collapsed;
			setDirty(true);
		}
	}

	@Override
	public Vertex getCollapsedRepresentation() {
		return m_collapsedVertex;
	}
	
}
