/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.fabric.arp.packet;

import java.util.HashSet;

import org.opendaylight.controller.fabric.arp.flow.FlowWriterService;
import org.opendaylight.controller.fabric.arp.inventory.ConnectorService;
import org.opendaylight.controller.fabric.arp.inventory.HostService;
import org.opendaylight.controller.fabric.arp.util.InstanceIdentifierUtils;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.packet.ARP;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IPv4;
import org.opendaylight.controller.sal.packet.LinkEncap;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.map.rev140402.FabricMaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.map.rev140402.fabric.maps.FabricMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.map.rev140402.fabric.maps.FabricMapKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PacketHandler examines Ethernet packets to find FabricAddresses (mac, nodeConnector) pairings
 * of the sender and learns them.
 * It also forwards the data packets appropriately dependending upon whether it knows about the
 * target or not.
 */
public class PacketHandlerLastNodeTag_bak implements PacketProcessingListener {

    private final Logger _logger = LoggerFactory.getLogger(PacketHandlerLastNodeTag_bak.class);
    private final PacketProcessingService packetProcessingService;
    private final FlowWriterService flowWriterService;
    private final DataBrokerService dataService;
    private final ConnectorService connectorService;
    private final HostService hostService;

    public PacketHandlerLastNodeTag_bak(DataBrokerService dataService,
            PacketProcessingService packetProcessingService,
            FlowWriterService flowWriterService,
            ConnectorService connectorService,
            HostService hostService){
        this.dataService = dataService;
        this.packetProcessingService = packetProcessingService;
        this.flowWriterService = flowWriterService;
        this.connectorService = connectorService;
        this.hostService = hostService;
        return;
    }

    /**
    * The handler function for all incoming packets.
    * @param packetReceived  The incoming packet.
    */
    @Override
    public void onPacketReceived(PacketReceived packetReceived) {
        if(packetReceived == null){
            return;
        }
        try {
            byte[] payload = packetReceived.getPayload();
            RawPacket rawPacket = new RawPacket(payload);
            NodeConnectorRef ingress = packetReceived.getIngress();
            Packet packet = this.decodeDataPacket(rawPacket);
            if(!(packet instanceof Ethernet)) {
                _logger.error("Packet is not ethernet");
                return;
            }
            this.handlePacket(packet, ingress);
        } catch(Exception e) {
            _logger.error("Failed to handle packet {}", packetReceived, e);
        }
    }
    /**
     * deal the packet
     * @param packet
     * @param ingress
     */
    private void handlePacket(Packet packet, NodeConnectorRef ingress){
        byte[] srcMac = ((Ethernet) packet).getSourceMACAddress();
        byte[] dstMac = ((Ethernet) packet).getDestinationMACAddress();
        //If srcMac and dstMac is not exit
        if (srcMac  == null || srcMac.length  == 0){
            return;
        }
        Object enclosedPacket = packet.getPayload();

        IpAddress srcIpAddress = null;
        IpAddress dstIpAddress = null;
        // only ARP & IPv4 packets are handled by OpenFlowPlugin
        if(enclosedPacket instanceof IPv4){
            IPv4 ip = (IPv4)enclosedPacket;
            if(ip.getDestinationAddress() == -1){
                return;
            }
            srcIpAddress = this.toIpAddress(ip.getSourceAddress());
            dstIpAddress = this.toIpAddress(ip.getDestinationAddress());
        }else if(enclosedPacket instanceof ARP){
            ARP arp = (ARP)enclosedPacket;
            srcIpAddress = this.toIpAddress(arp.getSenderProtocolAddress());
            dstIpAddress = this.toIpAddress(arp.getTargetProtocolAddress());
        }else{
            return;
        }

        byte[] payload = packet.getRawPayload();
        MacAddress srcMacAddress = this.toMacAddress(srcMac);
        MacAddress dstMacAddress = this.toMacAddress(dstMac);

        // if ingress does not equal data store, update data store;
        NodeConnectorRef tempNCR = this.hostService.getNodeConnectorRefByMac(srcMacAddress);
        if(!ingress.equals(tempNCR)){
            if(tempNCR != null){
                // remove old flows
            }
            this.flowWriterService.addMacToMacFlowsHostPath(srcMacAddress, ingress);
            this.hostService.addAddressNodeConnector(srcMacAddress,ingress);
        }
        // if ip does not equal data store , update data store;
        MacAddress tempMac = this.hostService.getMacByIp(srcIpAddress);
        if(!srcMacAddress.equals(tempMac)){
            this.hostService.addAddress(srcIpAddress, srcMacAddress);
        }

        NodeConnectorRef dstNCR = this.hostService.getNodeConnectorRefByMac(dstMacAddress);
        if( dstNCR == null){
            // find dst ip mac
            MacAddress tempDstMac = this.hostService.getMacByIp(dstIpAddress);
            if(tempDstMac != null){
                dstNCR = this.hostService.getNodeConnectorRefByMac(tempDstMac);
                dstMacAddress = tempDstMac;
            }
        }

        if(dstNCR == null){
            this.floodExternalPorts(payload, ingress);
        }else{
            if(this.checkSameNode(ingress,dstNCR)){
                this.flowWriterService.addMacToMacFlowsSameNode(srcMacAddress, ingress, dstMacAddress, dstNCR);
                this.flowWriterService.addMacToMacFlowsSameNode(dstMacAddress, dstNCR, srcMacAddress, ingress);
            }else{
                long srcTag = this.getTag(ingress);
                long dstTag = this.getTag(dstNCR);
                this.flowWriterService.addMacToMacFlowsFabricPath(srcMacAddress, ingress, dstMacAddress, dstNCR,dstTag);
                this.flowWriterService.addMacToMacFlowsFabricPath(dstMacAddress, dstNCR, srcMacAddress, ingress,srcTag);
            }
            NodeConnectorRef ncr = this.connectorService.getNodeControllerNodeConnectorByNodeId(dstNCR);
            this.sendPacketOut(payload, ncr, dstNCR);
        }
        return;
    }
    /**
    * Floods the specified payload on external ports, which are ports not connected to switches.
    * @param payload  The payload to be flooded.
    * @param ingress  The NodeConnector where the payload came from.
    */
    private void floodExternalPorts(byte[] payload, NodeConnectorRef ingress) {
        HashSet<NodeConnectorRef> externalPorts = this.connectorService.getExternalNodeConnectors();
        externalPorts.remove(ingress);

        for (NodeConnectorRef egress : externalPorts) {
            this.sendPacketOut(payload, this.connectorService.getNodeControllerNodeConnectorByNodeId(egress), egress);
        }
    }

    /**
    * Sends the specified packet on the specified port.
    * @param payload  The payload to be sent.
    * @param ingress  The NodeConnector where the payload came from.
    * @param egress  The NodeConnector where the payload will go.
    */
    private void sendPacketOut(byte[] payload, NodeConnectorRef ingress, NodeConnectorRef egress) {
        if (ingress == null || egress == null)  return;
        InstanceIdentifier<Node> egressNodePath = InstanceIdentifierUtils.getNodePath(egress.getValue());
        TransmitPacketInput input = new TransmitPacketInputBuilder() //
            .setPayload(payload) //
            .setNode(new NodeRef(egressNodePath)) //
            .setEgress(egress) //
            .setIngress(ingress) //
            .build();
        packetProcessingService.transmitPacket(input);
    }

    /**
    * Decodes an incoming packet.
    * @param raw  The raw packet to be decoded.
    * @return  The decoded form of the raw packet.
    */
    private Packet decodeDataPacket(RawPacket raw) {
        if(raw == null) {
          return null;
        }
        byte[] data = raw.getPacketData();
        if(data.length <= 0) {
          return null;
        }
        if(raw.getEncap().equals(LinkEncap.ETHERNET)) {
          Ethernet res = new Ethernet();
          try {
            res.deserialize(data, 0, data.length * NetUtils.NumBitsInAByte);
            res.setRawPayload(raw.getPacketData());
          } catch(Exception e) {
            _logger.warn("Failed to decode packet: {}", e.getMessage());
          }
          return res;
        }
        return null;
    }

    /**
    * Creates a MacAddress object out of a byte array.
    * @param dataLinkAddress  The byte-array form of a MacAddress
    * @return  MacAddress of the specified dataLinkAddress.
    */
    private MacAddress toMacAddress(byte[] dataLinkAddress) {
        return new MacAddress(HexEncode.bytesToHexStringFormat(dataLinkAddress));
    }
    /**
     * Creates a MacAddress object out of a byte array.
     * @param ip
     * @return
     */
    private IpAddress toIpAddress(int ip) {
        StringBuffer buf = new StringBuffer();
        buf.append(ip&0xf000 >> 12);
        buf.append('.');
        buf.append(ip&0x0f00 >> 8);
        buf.append('.');
        buf.append(ip&0x00f0 >> 4);
        buf.append('.');
        buf.append(ip&0x000f);
        return IpAddressBuilder.getDefaultInstance(buf.toString());
    }
    /**
     * Creates a MacAddress object out of a byte array.
     * @param ip
     * @return
     */
    private IpAddress toIpAddress(byte[] ip) {
        StringBuffer buf = new StringBuffer();
        buf.append(ip[0]&0xff);
        buf.append('.');
        buf.append(ip[1]&0xff);
        buf.append('.');
        buf.append(ip[2]&0xff);
        buf.append('.');
        buf.append(ip[3]&0xff);
        return IpAddressBuilder.getDefaultInstance(buf.toString());
    }

    /**
     * check if src & dst is the same node
     * @param src
     * @param dst
     * @return
     */
    private boolean checkSameNode(NodeConnectorRef src,NodeConnectorRef dst){
        InstanceIdentifier<Node> srcNode = InstanceIdentifierUtils.generateNodeInstanceIdentifier(src);
        InstanceIdentifier<Node> dstNode = InstanceIdentifierUtils.generateNodeInstanceIdentifier(dst);
        if(srcNode.equals(dstNode)){
            return true;
        }else{
            return false;
        }
    }
    /**
     * Get the tag from data store;
     * @param src
     * @param dst
     * @return
     */
    private long getTag(NodeConnectorRef nrc){
        InstanceIdentifier<Node> nodePath = InstanceIdentifierUtils.generateNodeInstanceIdentifier(nrc);
        NodeId nodeId = new NodeId(nodePath.firstKeyOf(Node.class,NodeKey.class).getId().getValue());
        InstanceIdentifier<FabricMap> ref = InstanceIdentifier.builder(FabricMaps.class)
                .child(FabricMap.class,new FabricMapKey(nodeId)).build();
        FabricMap srcFabricMap = (FabricMap)this.dataService.readOperationalData(ref);
        return srcFabricMap.getId();
    }
}
