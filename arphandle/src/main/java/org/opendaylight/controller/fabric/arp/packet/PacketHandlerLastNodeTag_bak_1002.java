/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.fabric.arp.packet;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.fabric.FabricProvider;
import org.opendaylight.controller.fabric.FabricService;
import org.opendaylight.controller.fabric.arp.flow.FlowWriterService;
import org.opendaylight.controller.fabric.arp.inventory.ConnectorService;
import org.opendaylight.controller.fabric.arp.inventory.FabricImpl;
import org.opendaylight.controller.fabric.arp.inventory.HostImpl;
import org.opendaylight.controller.fabric.arp.inventory.HostService;
import org.opendaylight.controller.fabric.arp.util.InstanceIdentifierUtils;
import org.opendaylight.controller.group.GroupService;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.compatibility.NodeMapping;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.packet.ARP;
import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IPv4;
import org.opendaylight.controller.sal.packet.LinkEncap;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PacketHandler examines Ethernet packets to find FabricAddresses (mac, nodeConnector) pairings
 * of the sender and learns them.
 * It also forwards the data packets appropriately dependending upon whether it knows about the
 * target or not.
 */
public class PacketHandlerLastNodeTag_bak_1002 implements PacketProcessingListener {

    private final Logger _logger = LoggerFactory.getLogger(PacketHandlerLastNodeTag_bak_1002.class);
    private final PacketProcessingService packetProcessingService;
    private final FlowWriterService flowWriterService;
    private final DataBroker dataService;
    private final ConnectorService connectorService;
    private final HostService hostService;
    //private Set<IfHostListener> hostListeners = new CopyOnWriteArraySet<IfHostListener>();
    private IfIptoHost hostListener = null;
    private FabricService fabricService = null;
    private GroupService groupService = null;
    // hosts
    private ConcurrentHashMap<IpAddress,HostImpl> hostSets;
    private BlockingQueue<HostImpl> addHostFlowQueue = new LinkedBlockingQueue<HostImpl>();
    // fabrics
    private BlockingQueue<FabricImpl> addFabricFlowQueue = new LinkedBlockingQueue<FabricImpl>();
    // thread
    private Thread addHostFlowThread;
    private Thread addFabricFlowThread;
    // broad packet
    private final byte bBroadSourcePacket[] = {(byte)0,(byte)0,(byte)0,(byte)0};
    private final byte bBroadTargetPacket[] = {(byte)255,(byte)255,(byte)255,(byte)255};
    private final IpAddress BroadSourcePacket;
    private final IpAddress BroadTargetPacket;

    public PacketHandlerLastNodeTag_bak_1002(DataBroker dataService,
            PacketProcessingService packetProcessingService,
            FlowWriterService flowWriterService,
            ConnectorService connectorService,
            HostService hostService){
        this.dataService = dataService;
        this.packetProcessingService = packetProcessingService;
        this.flowWriterService = flowWriterService;
        this.connectorService = connectorService;
        this.hostService = hostService;
        this.hostSets = new ConcurrentHashMap<IpAddress,HostImpl>();
        this.addHostFlowThread = new Thread(new AddHostFlow());
        this.addFabricFlowThread = new Thread(new AddFabricFlow());
        this.BroadSourcePacket = this.toIpAddress(bBroadSourcePacket);
        this.BroadTargetPacket = this.toIpAddress(bBroadTargetPacket);
        return;
    }
    public void start(){
        this.addHostFlowThread.start();
        this.addFabricFlowThread.start();
    }
    public void stop(){
        this.addHostFlowThread.interrupt();
        this.addFabricFlowThread.interrupt();
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
            Object enclosedPacket = packet.getPayload();
            if(enclosedPacket instanceof IPv4){
                this.handleIPPacket(packet,(IPv4)enclosedPacket, ingress);
            }else if(enclosedPacket instanceof ARP){
                ARP arp = (ARP)enclosedPacket;
                if(arp.getOpCode() == ARP.REQUEST){
                    this.handleArpRequestPacket(packet,arp, ingress);
                }else{
                    this.handleArpReplyPacket(packet,arp, ingress);
                }
            }else{
                return;
            }
        } catch(Exception e) {
            _logger.error("Failed to handle packet {}", packetReceived, e);
        }
    }
    private void handleIPPacket(Packet packet,IPv4 ipv4, NodeConnectorRef ingress){
        IpAddress srcIp = this.toIpAddress(ipv4.getSourceAddress());
        IpAddress dstIp = this.toIpAddress(ipv4.getDestinationAddress());
        if(this.checkBroadCastPacket(srcIp, dstIp)){
            this.floodExternalPorts(packet.getRawPayload(), ingress);
            return;
        }
        //check host enabled
        if(!this.checkHostIp(srcIp,dstIp)){
            return;
        }
        HostImpl senderImpl = this.hostSets.get(srcIp);
        HostImpl targetImpl = this.hostSets.get(dstIp);

        if(senderImpl == null){
            byte[] srcMac = ((Ethernet) packet).getSourceMACAddress();
            senderImpl = new HostImpl(srcIp,this.toMacAddress(srcMac),ingress);
            if(!this.addHostFlowQueue  .contains(senderImpl)){
                try{
                    this.addHostFlowQueue.put(senderImpl);
                }catch(Exception ex){
                }
            }
        }
        if(targetImpl != null){
            //check node enabled
            if(!this.checkNode(ingress,targetImpl.getNodeConnector())){
                return;
            }
            FabricImpl fabricImpl = new FabricImpl(senderImpl, targetImpl);
            if(!this.addFabricFlowQueue.contains(fabricImpl)){
                try{
                    this.addFabricFlowQueue.put(fabricImpl);
                }catch(Exception ex){
                }
            }
            NodeConnectorRef egress = targetImpl.getNodeConnector();
            NodeConnectorRef ncr = connectorService.getNodeControllerNodeConnectorByNodeId(egress);
            this.sendPacketOut(packet.getRawPayload(), ncr, egress);
        }else{
            this.floodExternalPorts(packet.getRawPayload(), ingress);
        }
        return;
    }
    private void handleArpRequestPacket(Packet packet,ARP arp, NodeConnectorRef ingress){

        // update address
        MacAddress senderMac = this.toMacAddress(arp.getSenderHardwareAddress());
        IpAddress senderIp = this.toIpAddress(arp.getSenderProtocolAddress());
        HostImpl senderImpl = new HostImpl(senderIp,senderMac,ingress);
        if(!this.addHostFlowQueue.contains(senderImpl)){
            try{
                this.addHost(arp,ingress);
                this.addHostFlowQueue.put(senderImpl);
            }catch(Exception ex){
            }
        }
        // get target
        IpAddress targetIp = this.toIpAddress(arp.getTargetProtocolAddress());
        // check host enabled
        if(!this.checkHostIp(senderIp,targetIp)){
            return;
        }
        HostImpl targetImpl = this.hostSets.get(targetIp);
        if(targetImpl == null){
            this.floodExternalPorts(packet.getRawPayload(), ingress);
        }else{
            // check node enabled
            if(!this.checkNode(ingress,targetImpl.getNodeConnector())){
                return;
            }
            try{
                ARP arpReply = this.createARP(ARP.REPLY,this.toMacByte(targetImpl.getMac()),this.toIpByte(targetImpl.getIp()),arp.getSenderHardwareAddress(),arp.getSenderProtocolAddress());
                Ethernet ethernet = createEthernet(arpReply.getSenderHardwareAddress(),arpReply.getTargetHardwareAddress(), arpReply);
                this.sendPacketOut(ethernet.serialize(), connectorService.getNodeControllerNodeConnectorByNodeId(ingress), ingress);
                FabricImpl fabricImpl = new FabricImpl(senderImpl, targetImpl);
                if(!this.addFabricFlowQueue.contains(fabricImpl)){
                    try{
                        this.addFabricFlowQueue.put(fabricImpl);
                    }catch(Exception ex){
                    }
                }
            }catch(Exception ex){
                NodeConnectorRef egress = targetImpl.getNodeConnector();
                NodeConnectorRef ncr = connectorService.getNodeControllerNodeConnectorByNodeId(egress);
                this.sendPacketOut(packet.getRawPayload(), ncr, egress);
            }
        }
        return;
    }
    private void handleArpReplyPacket(Packet packet,ARP arp, NodeConnectorRef ingress){
        // update address
        MacAddress senderMac = this.toMacAddress(arp.getSenderHardwareAddress());
        IpAddress senderIp = this.toIpAddress(arp.getSenderProtocolAddress());
        HostImpl senderImpl = new HostImpl(senderIp,senderMac,ingress);
        if(!this.addHostFlowQueue.contains(senderImpl)){
            try{
                this.addHost(arp,ingress);
                this.addHostFlowQueue.put(senderImpl);
            }catch(Exception ex){
            }
        }
        // get target
        IpAddress targetIp = this.toIpAddress(arp.getTargetProtocolAddress());
        // check host enabled
        if(!this.checkHostIp(senderIp,targetIp)){
            return;
        }

        HostImpl targetImpl = this.hostSets.get(targetIp);
        FabricImpl fabricImpl = new FabricImpl(senderImpl, targetImpl);

        // check node enabled
        if(!this.checkNode(ingress,targetImpl.getNodeConnector())){
            return;
        }
        if(!this.addFabricFlowQueue.contains(fabricImpl)){
            try{
                this.addFabricFlowQueue.put(fabricImpl);
            }catch(Exception ex){
            }
        }
        NodeConnectorRef egress = targetImpl.getNodeConnector();
        NodeConnectorRef ncr = connectorService.getNodeControllerNodeConnectorByNodeId(egress);
        this.sendPacketOut(packet.getRawPayload(), ncr, egress);
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
     * create arp
     * @param opCode
     * @param senderMacAddress
     * @param senderIP
     * @param targetMacAddress
     * @param targetIP
     * @return
     */
    private ARP createARP(short opCode, byte[] senderMacAddress, byte[] senderIP, byte[] targetMacAddress,
            byte[] targetIP) {
        ARP arp = new ARP();
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET);
        arp.setProtocolType(EtherTypes.IPv4.shortValue());
        arp.setHardwareAddressLength((byte) 6);
        arp.setProtocolAddressLength((byte) 4);
        arp.setOpCode(opCode);
        arp.setSenderHardwareAddress(senderMacAddress);
        arp.setSenderProtocolAddress(senderIP);
        arp.setTargetHardwareAddress(targetMacAddress);
        arp.setTargetProtocolAddress(targetIP);
        return arp;
    }
    /**
     * create Ethernet
     * @param sourceMAC
     * @param targetMAC
     * @param arp
     * @return
     */
    private Ethernet createEthernet(byte[] sourceMAC, byte[] targetMAC, ARP arp) {
        Ethernet ethernet = new Ethernet();
        ethernet.setSourceMACAddress(sourceMAC);
        ethernet.setDestinationMACAddress(targetMAC);
        ethernet.setEtherType(EtherTypes.ARP.shortValue());
        ethernet.setPayload(arp);
        return ethernet;
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
     * creates a byte from macaddress
     * @param m
     * @return
     */
    private byte[] toMacByte(MacAddress m){
        String[] octets = m.getValue().split(":");

        byte[] ret = new byte[octets.length];
        for (int i = 0; i < octets.length; i++) {
            ret[i] = Integer.valueOf(octets[i], 16).byteValue();
        }
        return ret;
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
     * creates a byte from macaddress
     * @param m
     * @return
     */
    private byte[] toIpByte(IpAddress m){
        String[] octets = m.getIpv4Address().getValue().split("\\.");

        byte[] ret = new byte[octets.length];
        for (int i = 0; i < octets.length; i++) {
            ret[i] = Integer.valueOf(octets[i], 10).byteValue();
        }
        return ret;
    }
    /**
     * Creates a MacAddress object out of a byte array.
     * @param ip
     * @return
     */
    private IpAddress toIpAddress(int ip) {
        byte[] t = BitBufferHelper.toByteArray(ip);
        return this.toIpAddress(t);
//        StringBuffer buf = new StringBuffer();
//        buf.append(ip&0xf000 >> 12);
//        buf.append('.');
//        buf.append(ip&0x0f00 >> 8);
//        buf.append('.');
//        buf.append(ip&0x00f0 >> 4);
//        buf.append('.');
//        buf.append(ip&0x000f);
//        return IpAddressBuilder.getDefaultInstance(buf.toString());
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
//        InstanceIdentifier<FabricMap> ref = InstanceIdentifier.builder(FabricMaps.class)
//                .child(FabricMap.class,new FabricMapKey(nodeId)).build();
//        FabricMap srcFabricMap = (FabricMap)this.dataService.readOperationalData(ref);
//        return srcFabricMap.getId();
        return this.getTagByService(nodeId);
    }
    /**
     * Get the tag from data store;
     * @param src
     * @param dst
     * @return
     */
    private long getTagByService(NodeId id){
        FabricService f = this.getFabricService();
        long ret = 0;
        if(f!= null){
            ret = f.getFabricIdByNodeId(id);
        }
        return ret;//f==null?0:f.getFabricIdByNodeId(id);
    }
    /**
     * get fabric service
     * @return
     */
    private FabricService getFabricService(){
        if(this.fabricService == null){
            try{
                BundleContext bCtx = FrameworkUtil.getBundle(FabricProvider.class).getBundleContext();
                ServiceReference<FabricService> srf = bCtx.getServiceReference(FabricService.class);
                this.fabricService = (FabricService)bCtx.getService(srf);
            }catch(Exception ex){
            }
        }
        return this.fabricService;
    }
    /**
     * get host service
     * @return
     */
    private IfIptoHost getIfHostListener(){
        //IfHostListener[] ifHostListener = (IfHostListener[]) ServiceHelper.getInstances(IfHostListener.class, GlobalConstants.DEFAULT.toString(),this,null);
        if(this.hostListener == null){
            this.hostListener = (IfIptoHost)ServiceHelper.getInstance(IfIptoHost.class, GlobalConstants.DEFAULT.toString(), this);
        }
        return this.hostListener;
    }
    /**
     * get group service
     * @return
     */
    private GroupService getGroupService(){
        if(this.groupService == null){
            this.groupService = (GroupService)ServiceHelper.getGlobalInstance(GroupService.class, this);
        }
        return this.groupService;
    }
    /**
     * check host1 and host2 can communicate
     * @param h1
     * @param h2
     * @return
     */
    private boolean checkHostIp(IpAddress h1,IpAddress h2){
        GroupService groupService = this.getGroupService();
        String host1 = String.valueOf(h1.getValue());
        String host2 = String.valueOf(h2.getValue());
        boolean ret = groupService.cheakHostsSameGroup(host1, host2);
        return ret;
    }
    /**
     * check node1 and node 2 can communicate
     * @param n1
     * @param n2
     * @return
     */
    private boolean checkNode(NodeConnectorRef n1,NodeConnectorRef n2){
        GroupService groupService = this.getGroupService();
        InstanceIdentifier<Node> nodePath1 = InstanceIdentifierUtils.generateNodeInstanceIdentifier(n1);
        NodeId nodeId1 = new NodeId(nodePath1.firstKeyOf(Node.class,NodeKey.class).getId().getValue());
        InstanceIdentifier<Node> nodePath2 = InstanceIdentifierUtils.generateNodeInstanceIdentifier(n2);
        NodeId nodeId2 = new NodeId(nodePath2.firstKeyOf(Node.class,NodeKey.class).getId().getValue());
        boolean ret = groupService.cheakSwitchsSameGroup(nodeId1.getValue(), nodeId2.getValue());
        return ret;
    }
    private void addHost(ARP arp,NodeConnectorRef ingress){
        try{
            this.getIfHostListener();
            byte[] mac = arp.getSenderHardwareAddress();
            InetAddress ip = InetAddress.getByAddress(arp.getSenderProtocolAddress());
            NodeConnector nc = NodeMapping.toADNodeConnector(ingress);
            HostNodeConnector host = new HostNodeConnector(mac,ip,nc,(short)0);
            //this.hostListener.hostListener(host);
            this.hostListener.addStaticHost(ip.getHostAddress(),HexEncode.bytesToHexStringFormat(mac), nc, null);
        }catch(Exception ex){
        }
    }
    private boolean checkBroadCastPacket(IpAddress source,IpAddress target){
        if(source.equals(this.BroadSourcePacket)){
            return true;
        }
        if(target.equals(this.BroadTargetPacket)){
            return true;
        }
        return false;
    }
    private class AddHostFlow implements Runnable{

        @Override
        public void run() {
            while (true) {
                if(!addHostFlowQueue.isEmpty()){
                    HostImpl hostImpl = addHostFlowQueue.element();
                    HostImpl temp = hostSets.get(hostImpl.getIp());
                    if(temp == null){
                        hostSets.put(hostImpl.getIp(), hostImpl);
                        flowWriterService.addMacToMacFlowsHostPath(hostImpl.getMac(), hostImpl.getNodeConnector());
                    }else if(!temp.getIp().equals(hostImpl.getIp())){
                        hostSets.put(hostImpl.getIp(), hostImpl);
                        flowWriterService.addMacToMacFlowsHostPath(hostImpl.getMac(), hostImpl.getNodeConnector());
                        // remove old flow
                        // temp..
                    }
                    addHostFlowQueue.remove();
                }
//                try{
//                    HostImpl hostImpl = addHostFlowQueue.take();
//                    HostImpl temp = hostSets.get(hostImpl.getIp());
//                    if(temp == null){
//                        hostSets.put(hostImpl.getIp(), hostImpl);
//                        flowWriterService.addMacToMacFlowsHostPath(hostImpl.getMac(), hostImpl.getNodeConnector());
//                    }else if(!temp.getIp().equals(hostImpl.getIp())){
//                        hostSets.put(hostImpl.getIp(), hostImpl);
//                        flowWriterService.addMacToMacFlowsHostPath(hostImpl.getMac(), hostImpl.getNodeConnector());
//                        // remove old flow
//                        // temp..
//                    }
//                }catch(Exception ex){
//                    ex.getMessage();
//                    return;
//                }
            }
        }
    }
    private class AddFabricFlow implements Runnable{

        @Override
        public void run() {
            while (true) {
                if(!addFabricFlowQueue.isEmpty()){
                    FabricImpl fabricImpl = addFabricFlowQueue.element();
                    HostImpl src = fabricImpl.getSrc();
                    HostImpl dst = fabricImpl.getDst();
                    NodeConnectorRef srcNCR = src.getNodeConnector();
                    NodeConnectorRef dstNCR = dst.getNodeConnector();
                    if(checkSameNode(srcNCR,dstNCR)){
                        flowWriterService.addMacToMacFlowsSameNode(src.getMac(), srcNCR, dst.getMac(), dstNCR);
                        flowWriterService.addMacToMacFlowsSameNode(dst.getMac(), dstNCR, src.getMac(), srcNCR);
                    }else{
                        long srcTag = getTag(srcNCR);
                        long dstTag = getTag(dstNCR);
                        flowWriterService.addMacToMacFlowsFabricPath(src.getMac(), srcNCR, dst.getMac(), dstNCR, dstTag);
                        flowWriterService.addMacToMacFlowsFabricPath(dst.getMac(), dstNCR, src.getMac(), srcNCR, srcTag);
                    }
                    addFabricFlowQueue.remove();
                }
//                try{
//                    FabricImpl fabricImpl = addFabricFlowQueue.take();
//                    HostImpl src = fabricImpl.getSrc();
//                    HostImpl dst = fabricImpl.getDst();
//                    NodeConnectorRef srcNCR = src.getNodeConnector();
//                    NodeConnectorRef dstNCR = dst.getNodeConnector();
//                    if(checkSameNode(srcNCR,dstNCR)){
//                        flowWriterService.addMacToMacFlowsSameNode(src.getMac(), srcNCR, dst.getMac(), dstNCR);
//                        flowWriterService.addMacToMacFlowsSameNode(dst.getMac(), dstNCR, src.getMac(), srcNCR);
//                    }else{
//                        long srcTag = getTag(srcNCR);
//                        long dstTag = getTag(dstNCR);
//                        flowWriterService.addMacToMacFlowsFabricPath(src.getMac(), srcNCR, dst.getMac(), dstNCR, dstTag);
//                        flowWriterService.addMacToMacFlowsFabricPath(dst.getMac(), dstNCR, src.getMac(), srcNCR, srcTag);
//                    }
//                }catch(Exception ex){
//                    ex.getMessage();
//                    return;
//                }
            }
        }
    }
}
