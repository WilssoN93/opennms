/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
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

package org.opennms.enlinkd.generator.protocol;

import java.net.InetAddress;
import java.util.Date;
import java.util.List;

import org.opennms.enlinkd.generator.TopologyContext;
import org.opennms.enlinkd.generator.TopologyGenerator;
import org.opennms.enlinkd.generator.TopologySettings;
import org.opennms.enlinkd.generator.protocol.bridge.BridgeBuilder;
import org.opennms.enlinkd.generator.protocol.bridge.BridgeBuilderContext;
import org.opennms.enlinkd.generator.util.IdGenerator;
import org.opennms.enlinkd.generator.util.InetAddressGenerator;
import org.opennms.enlinkd.generator.util.MacAddressGenerator;
import org.opennms.netmgt.enlinkd.model.BridgeBridgeLink;
import org.opennms.netmgt.enlinkd.model.BridgeElement;
import org.opennms.netmgt.enlinkd.model.BridgeElement.BridgeDot1dBaseType;
import org.opennms.netmgt.enlinkd.model.BridgeMacLink;
import org.opennms.netmgt.enlinkd.model.IpNetToMedia;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsSnmpInterface;

/**
 * Creates a topology with Bridges and Segments.
 * Call with: enlinkd:generate-topology --protocol bridge --nodes 10 --snmpinterfaces 0 --ipinterfaces 0
 * Example:
 *
 *
 *                           bridge0
 *           ------------------------------------
 *           |      |          |        |       |
 *        bridge1  bridge3  bridge4  bridge5  Macs/Ip
 *           |          |                     no node
 *        -------    --------
 *        |          |      |
 *     Segment     host8    host9
 *   -----------
 *   |         |
 * Macs/Ip  bridge2
 * no node     |
 *          Segment
 *
 * 6 nodes are hosts: host4,host5, host6, host7, host8, host9
 * generate 4 ipnettomedia without a corresponding node
 * consider also that on port 1 of C is connected an HUB with a group of hosts connected
 * the hub has no snmp agent and therefore we are not to explore his mab forwarding table
 * we also follow the convention to start the port countin from the if of the node
 * following an integer: so port11 -> is the first generated port of node with id 1
 *                          port12 -> is the second generated port of node with id 1
 *                          port21 -> is the first generated port of node with id 2
 *                          port53 -> is the third generated port of node with id 5
 */
public class BridgeProtocol extends Protocol<BridgeBridgeLink> {

    /* this is the vlan id for all the bridgebridgelinks and bridgemaclinks */
    private final static int VLAN_ID = 1;

    private final MacAddressGenerator macGenerator = new MacAddressGenerator();
    private final InetAddressGenerator inetGenerator = new InetAddressGenerator();

    public BridgeProtocol(TopologySettings topologySettings, TopologyContext context) {
        super(topologySettings, context);
    }

    @Override
    protected TopologyGenerator.Protocol getProtocol() {
        return TopologyGenerator.Protocol.bridge;
    }

    @Override
    protected void createAndPersistProtocolSpecificEntities(List<OnmsNode> nodes) {

        OnmsNode bridge0 =  nodes.get(0);
        OnmsNode bridge1 =  nodes.get(1);
        OnmsNode bridge2 =  nodes.get(2);
        OnmsNode bridge3 =  nodes.get(3);
        OnmsNode host4   =  nodes.get(4);
        OnmsNode host5   =  nodes.get(5);
        OnmsNode host6   =  nodes.get(6);
        OnmsNode host7   =  nodes.get(7);
        OnmsNode host8   =  nodes.get(8);
        OnmsNode host9   =  nodes.get(9);
        int bridge0portCounter=1;
        int bridge1portCounter=11;
        int bridge2portCounter=21;
        int bridge3portCounter=31;
        int host4portCounter = 41;
        int host5portCounter = 51;
        int host6portCounter = 61;
        int host7portCounter = 71;

        BridgeBuilderContext context = new BridgeBuilderContext(this.context.getTopologyPersister(), this.macGenerator, this.inetGenerator);
        BridgeBuilder bridge0B = new BridgeBuilder(bridge0, bridge0portCounter - 1, context);


        // save bridge element
        this.context.getTopologyPersister().persist(
            createBridgeElement(bridge0),
                // instead of:
            createBridgeElement(bridge1),
            createBridgeElement(bridge2),
                // instead of:
                createBridgeElement(bridge3)
        );
        
        //bridge0
        //bridge0:port1 connected to up bridge1:port11
        BridgeBuilder bridge1B = bridge0B.connectToNewBridge(bridge1, bridge1portCounter);
        // instead of:
        // createAndPersistBridgeBridgeLink(bridge1, bridge1portCounter, bridge0, bridge0portCounter);

        //bridge3:port31 connected to up bridge0:port2
        bridge0B.connectToNewBridge(bridge3, bridge3portCounter);
        bridge0portCounter++;
        // instead of:
        // createAndPersistBridgeBridgeLink(bridge3, bridge3portCounter, bridge0, bridge0portCounter);

        //bridge0:port3 connected to cloud
        bridge0portCounter++;
        // bridge0B.createAndPersistCloud(2, 2);
        // instead off:
        createAndPersistCloud(bridge0, bridge0portCounter, 2, 2);

        // bridge0:port4 connected to host4:port41
        bridge0portCounter++;
        // bridge0B.createAndPersistBridgeMacLink(host4, host4portCounter);
        // instead of:
        createAndPersistBridgeMacLink(bridge0, bridge0portCounter, host4, host4portCounter, bridge0);

        // bridge0:port5 connected to host5:port51
        bridge0portCounter++;
        // bridge0B.createAndPersistBridgeMacLink(host5, host5portCounter);
        // instead of:
        createAndPersistBridgeMacLink(bridge0, bridge0portCounter, host5, host5portCounter, bridge0);

        //bridge1
        //bridge2:port21 connected to bridge1:port12 with clouds
        bridge1portCounter++;
        bridge1B.connectToNewBridge(bridge2, bridge2portCounter);
// instead of
  // createAndPersistBridgeBridgeLink(bridge2, bridge2portCounter, bridge1, bridge1portCounter);
//         bridge1B.createAndPersistCloud(2, 2);
         createAndPersistCloud(bridge1, bridge1portCounter, 2, 2);
        
        //bridge2
        // host6 and host 7 connected to port 22 
        bridge2portCounter++;
        createAndPersistBridgeMacLink(bridge2, bridge2portCounter, true,  host6, host6portCounter, bridge0);
        createAndPersistBridgeMacLink(bridge2, bridge2portCounter, false, host7, host7portCounter, bridge0);
        
        // bridge3
        // host8:with-no-snmp connected bridge3:port32
        bridge3portCounter++;
        createAndPersistBridgeMacLink(bridge3, bridge3portCounter, host8, null, bridge0);
        // host9:with-no-snmp connected bridge3:port33
        bridge3portCounter++;
        createAndPersistBridgeMacLink(bridge3, bridge3portCounter, host9, null, bridge0);
    }

    private void createAndPersistBridgeBridgeLink(final OnmsNode bridge, final Integer port, final OnmsNode designated, final Integer designatedPort) {
        createAndPersistBridgeBridgeLink(bridge, port, true, designated, designatedPort, true);
    }

    private void createAndPersistBridgeBridgeLink(OnmsNode bridge, Integer port, boolean createSnmpInterface, OnmsNode designated, Integer designatedPort, boolean createdesignatedsnmpInterface) {
        if (createSnmpInterface) {
            context.getTopologyPersister().persist(
                    createSnmpInterface(port, bridge)
            );
        }
        if (createdesignatedsnmpInterface) {
            context.getTopologyPersister().persist(
                    createSnmpInterface(designatedPort, designated)
            );
        }
        context.getTopologyPersister().persist(
                createBridgeBridgeLink(bridge, designated, port, designatedPort)
        );
    }

    private void createAndPersistBridgeMacLink(OnmsNode bridge, Integer port, OnmsNode host, Integer hostPort, OnmsNode gateway) {
        createAndPersistBridgeMacLink(bridge, port, true, host, hostPort, gateway);
    }

    private void createAndPersistBridgeMacLink(OnmsNode bridge, Integer port, boolean createsnmpinterface, OnmsNode host, Integer hostPort, OnmsNode gateway) {
        String mac=macGenerator.next();
        InetAddress ip= inetGenerator.next();
        if (createsnmpinterface) {
            context.getTopologyPersister().persist(
                    createSnmpInterface(port, bridge)
            );
        }
        if (hostPort != null) {
            OnmsSnmpInterface snmpInterface = createSnmpInterface(hostPort, host);
            context.getTopologyPersister().persist(
                    snmpInterface
            );
            OnmsIpInterface ipInterface = createIpInterface(snmpInterface, ip);
            context.getTopologyPersister().persist(ipInterface);
        } else {
            OnmsIpInterface ipInterface = createIpInterface(null,ip);
            ipInterface.setNode(host);
            context.getTopologyPersister().persist(ipInterface);
        }
        context.getTopologyPersister().persist(
                createIpNetToMedia(host, hostPort, mac, ip, gateway)
        );
        context.getTopologyPersister().persist(
                createBridgeMacLink(bridge, port, mac)
        );
    }

    private void createAndPersistCloud(OnmsNode bridge, Integer port, int macaddresses, int ipaddresses) {
        for (int i=0; i<ipaddresses;i++) {
            String nextMac = macGenerator.next();
            context.getTopologyPersister().persist(
                    createIpNetToMedia(null, null, nextMac, inetGenerator.next(), bridge)
            );
            context.getTopologyPersister().persist(
                    createBridgeMacLink(bridge, port, nextMac));
        }

        for (int i=0; i<macaddresses;i++) {
            context.getTopologyPersister().persist(createBridgeMacLink(bridge, port, macGenerator.next()));
        }
    }

    private BridgeElement createBridgeElement(OnmsNode node) {
        BridgeElement bridge = new BridgeElement();
        bridge.setNode(node);
        bridge.setBaseBridgeAddress(macGenerator.next());
        bridge.setBaseType(BridgeDot1dBaseType.DOT1DBASETYPE_TRANSPARENT_ONLY);
        bridge.setBridgeNodeLastPollTime(new Date());
        bridge.setBaseNumPorts(topologySettings.getAmountLinks());
        bridge.setVlan(VLAN_ID);
        bridge.setVlanname("default");
        return bridge;
    }

    private BridgeBridgeLink createBridgeBridgeLink(OnmsNode node, OnmsNode designatedNode, int port, int designatedPort) {
        BridgeBridgeLink link = new BridgeBridgeLink();
        link.setBridgePortIfIndex(port);
        link.setBridgePort(port);
        link.setVlan(1);
        link.setNode(node);
        link.setDesignatedNode(designatedNode);
        link.setDesignatedPort(designatedPort);
        link.setDesignatedPortIfIndex(designatedPort);
        link.setDesignatedVlan(VLAN_ID);
        link.setBridgeBridgeLinkLastPollTime(new Date());
        return link;
    }

    private BridgeMacLink createBridgeMacLink(OnmsNode bridge, Integer bridgePort, String mac){
        BridgeMacLink bridgeMacLink = new BridgeMacLink();
        bridgeMacLink.setNode(bridge);
        bridgeMacLink.setBridgePort(bridgePort);
        bridgeMacLink.setBridgePortIfIndex(bridgePort);
        bridgeMacLink.setLinkType(BridgeMacLink.BridgeMacLinkType.BRIDGE_LINK);
        bridgeMacLink.setMacAddress(mac);
        bridgeMacLink.setVlan(VLAN_ID);
        bridgeMacLink.setBridgeMacLinkLastPollTime(new Date());
        return bridgeMacLink;
    }

    private IpNetToMedia createIpNetToMedia(OnmsNode node, Integer ifindex, String mac, InetAddress inetAddress, OnmsNode sourceNode){
        IpNetToMedia ipNetToMedia = new IpNetToMedia();
        ipNetToMedia.setCreateTime(new Date());
        if (node != null && ifindex == null ) {
            ipNetToMedia.setIfIndex(-1);
        } else {
            ipNetToMedia.setIfIndex(ifindex);
        }
        ipNetToMedia.setLastPollTime(new Date());
        ipNetToMedia.setIpNetToMediaType(IpNetToMedia.IpNetToMediaType.IPNETTOMEDIA_TYPE_DYNAMIC);
        ipNetToMedia.setNetAddress(inetAddress);
        ipNetToMedia.setPhysAddress(mac);
        ipNetToMedia.setNode(node);
        ipNetToMedia.setSourceIfIndex(0);
        ipNetToMedia.setSourceNode(sourceNode);
        return ipNetToMedia;
    }
}
