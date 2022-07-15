/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.enlinkd.service.api;

import java.util.Objects;
import java.util.Set;

public class BridgePortWithMacs implements Topology {

    private final BridgePort m_port;
    private final Set<String> m_macs;

    public BridgePortWithMacs(BridgePort port, Set<String> macs) {
        m_port=port;
        m_macs=macs;
    }

    public BridgePort getPort() {
        return m_port;
    }

    public Set<String> getMacs() {
        return m_macs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BridgePortWithMacs that = (BridgePortWithMacs) o;
        return Objects.equals(m_port, that.m_port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_port);
    }

    @Override
    public String printTopology() {
        return m_port.printTopology() +
                " macs:" +
                m_macs;
    }


    
}
