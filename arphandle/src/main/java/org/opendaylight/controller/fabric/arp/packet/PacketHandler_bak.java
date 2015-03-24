/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.fabric.arp.packet;

import java.util.List;

import org.opendaylight.controller.fabric.arp.addresstracker.AddressTracker;
import org.opendaylight.controller.fabric.arp.flow.FlowWriterService;
import org.opendaylight.controller.fabric.arp.inventory.InventoryService;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.address.tracker.rev140402.fabric.addresses.FabricAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.enumeration.rev140402.FabricTag;
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
public class PacketHandler_bak implements PacketProcessingListener {

    private final Logger _logger = LoggerFactory.getLogger(PacketHandler_bak.class);
    private final PacketProcessingService packetProcessingService;
    private final AddressTracker addressTracker;
    private final FlowWriterService flowWriterService;
    private final InventoryService inventoryService;
    private final DataBrokerService dataService;

    public PacketHandler_bak(DataBrokerService dataService,
            AddressTracker addressTracker,
            PacketProcessingService packetProcessingService,
            FlowWriterService flowWriterService,
            InventoryService inventoryService){
        this.dataService = dataService;
        this.addressTracker = addressTracker;
        this.packetProcessingService = packetProcessingService;
        this.flowWriterService = flowWriterService;
        this.inventoryService = inventoryService;
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
            this.handleEthernetPacket(packet, ingress);
        } catch(Exception e) {
            _logger.error("Failed to handle packet {}", packetReceived, e);
        }
    }

    /**
    * The handler function for Ethernet packets.
    * @param packet  The incoming Ethernet packet.
    * @param ingress  The NodeConnector where the Ethernet packet came from.
    */
    private void handleEthernetPacket(Packet packet, NodeConnectorRef ingress) {
        byte[] srcMac = ((Ethernet) packet).getSourceMACAddress();
        byte[] dstMac = ((Ethernet) packet).getDestinationMACAddress();
        //If srcMac and dstMac is not exit
        if (srcMac  == null || srcMac.length  == 0){
            return;
        }

        Object enclosedPacket = packet.getPayload();

        // only ARP & IPv4 packets are handled by OpenFlowPlugin
        if (!(enclosedPacket instanceof IPv4 || enclosedPacket instanceof ARP)){
            return;
        }

        MacAddress srcMacAddress = this.toMacAddress(srcMac);
        MacAddress dstMacAddress = this.toMacAddress(dstMac);
        // get FabricAddress by srcMac
        // if unknown, add FabricAddress
        FabricAddress src = this.addressTracker.getAddress(srcMacAddress);
        boolean isSrcKnown = (src != null);
        if (!isSrcKnown) {
            this.flowWriterService.addMacToMacFlowsHostPath(srcMacAddress, ingress);
            this.addressTracker.addAddress(srcMacAddress, ingress);
        }
        // get host by destMac
        // if known set destMac known to true
        FabricAddress dst = this.addressTracker.getAddress(dstMacAddress);
        boolean isDestKnown = (dst != null);

        byte[] payload = packet.getRawPayload();

        if(isSrcKnown & isDestKnown) {
            //if in the same node
            //not use fabric
            if(this.checkSameNode(src.getNodeConnectorRef(), dst.getNodeConnectorRef())){
                this.flowWriterService.addMacToMacFlowsSameNode(srcMacAddress, src.getNodeConnectorRef(), dstMacAddress, dst.getNodeConnectorRef());
                this.flowWriterService.addMacToMacFlowsSameNode(dstMacAddress, dst.getNodeConnectorRef(), srcMacAddress, src.getNodeConnectorRef());
            }else{
                long srcTag = this.getTag(src.getNodeConnectorRef());
                long dstTag = this.getTag(dst.getNodeConnectorRef());
                long tag = (srcTag <<FabricTag.VLAN.getIntValue()) + dstTag;
                this.flowWriterService.addMacToMacFlowsFabricPath(srcMacAddress, src.getNodeConnectorRef(), dstMacAddress, dst.getNodeConnectorRef(),tag);
                tag = (dstTag << FabricTag.VLAN.getIntValue()) + srcTag;
                this.flowWriterService.addMacToMacFlowsFabricPath(dstMacAddress, dst.getNodeConnectorRef(), srcMacAddress, src.getNodeConnectorRef(),tag);
            }
            this.sendPacketOut(payload, this.inventoryService.getControllerNodeConnector(dst.getNodeConnectorRef()), dst.getNodeConnectorRef());
        } else {
            //if (dest unknown)
            //sendpacket to external links minus ingress
            this.floodExternalPorts(payload, ingress);
        }
    }
    /**
    * Floods the specified payload on external ports, which are ports not connected to switches.
    * @param payload  The payload to be flooded.
    * @param ingress  The NodeConnector where the payload came from.
    */
    private void floodExternalPorts(byte[] payload, NodeConnectorRef ingress) {
        List<NodeConnectorRef> externalPorts = inventoryService.getExternalNodeConnectors();
        externalPorts.remove(ingress);

        for (NodeConnectorRef egress : externalPorts) {
            this.sendPacketOut(payload, this.inventoryService.getControllerNodeConnector(egress), egress);
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
     * check the src & dst is from the same node
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
