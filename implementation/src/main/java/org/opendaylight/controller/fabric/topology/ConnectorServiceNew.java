/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.fabric.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.fabric.util.InstanceIdentifierUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class ConnectorServiceNew {
    private final Hashtable<NodeId,NodeConnectorRef> nodeIdControllerNodeConnector;
    private final Hashtable<NodeId,HashSet<NodeConnectorRef>> nodeIdSwitchNodeConnectors;
    private final Hashtable<NodeId,HashSet<NodeConnectorRef>> nodeIdHostNodeConnectors;

    /**
     * constructor
     * @param dataService
     */
    public ConnectorServiceNew(){
        this.nodeIdControllerNodeConnector = new Hashtable<NodeId,NodeConnectorRef>();
        this.nodeIdSwitchNodeConnectors = new Hashtable<NodeId,HashSet<NodeConnectorRef>>();
        this.nodeIdHostNodeConnectors = new Hashtable<NodeId,HashSet<NodeConnectorRef>>();
        return;
    }
    /**
     * clear all connectors
     */
    public void clear(){
        this.nodeIdControllerNodeConnector.clear();
        this.nodeIdHostNodeConnectors.clear();
        this.nodeIdSwitchNodeConnectors.clear();
        return;
    }
    /**
     * update all connectors
     */
    public void update(Topology topology){
        this.clear();
        if(topology == null){
            return;
        }
        Set<String> internalNodeConnectors = new HashSet<>();
        List<Node> listNode = new ArrayList<Node>();
        Iterator<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> iNode = topology.getNode().iterator();
        NodeBuilder nb = new NodeBuilder();
        NodeConnectorBuilder ncb = new NodeConnectorBuilder();
        while(iNode.hasNext()){
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node tNode = iNode.next();
            String nodeId = tNode.getNodeId().getValue();
            List<NodeConnector> listNodeConnector = new ArrayList<NodeConnector>();
            // add tp
            if(tNode.getTerminationPoint() != null){
                Iterator<TerminationPoint> iTP = tNode.getTerminationPoint().iterator();
                while(iTP.hasNext()){
                    listNodeConnector.add(ncb.setId(new NodeConnectorId(iTP.next().getTpId().getValue())).build());
                }
            }
            Node n = nb.setId(new NodeId(nodeId))
                    .setNodeConnector(listNodeConnector)
                    .build();
            listNode.add(n);
        }

        List<Link> links = topology.getLink();
        if(links != null){
            for (Link link : links) {
                internalNodeConnectors.add(link.getDestination().getDestTp().getValue());
                internalNodeConnectors.add(link.getSource().getSourceTp().getValue());
            }
        }

        for(Node node : listNode){
            if(!node.getId().getValue().contains("controller")){
                this.initNodeConnectors(node, internalNodeConnectors);
            }
        }
        return;
    }
    /**
     * get all external connectors
     * @return
     */
    public HashSet<NodeConnectorRef> getExternalNodeConnectors(){
        HashSet<NodeConnectorRef> ret = new HashSet<NodeConnectorRef>();
        Collection<HashSet<NodeConnectorRef>> cNodeConnectorRef = this.nodeIdHostNodeConnectors.values();
        for(HashSet<NodeConnectorRef> tmp : cNodeConnectorRef){
            ret.addAll(tmp);
        }
        return ret;
    }
    /**
     * get the node's controller connector
     * @param nodeId
     * @return
     */
    public NodeConnectorRef getNodeControllerNodeConnectorByNodeId(NodeConnectorRef ref){
        InstanceIdentifier<Node> nodePath = InstanceIdentifierUtils.generateNodeInstanceIdentifier(ref);
        NodeKey nodeKey = InstanceIdentifierUtils.getNodeKey(nodePath);
        return this.nodeIdControllerNodeConnector.get(nodeKey.getId());
    }

    /**
     * init node connectors
     * @param node
     * @param internalNodeConnectors
     */
    private void initNodeConnectors(Node node, Set<String> internalNodeConnectors){
        List<NodeConnector> nodeConnetorList = node.getNodeConnector();
        // if switch has no connectors return
        if(nodeConnetorList == null){
            return;
        }
        HashSet<NodeConnectorRef> switchNodeConnectors = new HashSet<NodeConnectorRef>();
        HashSet<NodeConnectorRef> hostNodeConnectors = new HashSet<NodeConnectorRef>();
        this.nodeIdSwitchNodeConnectors.put(node.getId(), switchNodeConnectors);
        this.nodeIdHostNodeConnectors.put(node.getId(),  hostNodeConnectors);
        for(NodeConnector nodeConnector : nodeConnetorList){
            // it's a switch connector
            if(internalNodeConnectors.remove(nodeConnector.getId().getValue())){
                NodeConnectorRef ncr = new NodeConnectorRef(InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class, node.getKey())
                        .child(NodeConnector.class,nodeConnector.getKey()).toInstance());
                switchNodeConnectors.add(ncr);
            }else if(this.controllerConnector(nodeConnector)){
                // it's a controller connector
                NodeConnectorRef ncr = new NodeConnectorRef(InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class, node.getKey())
                        .child(NodeConnector.class,nodeConnector.getKey()).toInstance());
                this.nodeIdControllerNodeConnector.put(node.getId(), ncr);
            }else{
                // it's a host connector
                NodeConnectorRef ncr = new NodeConnectorRef(InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class, node.getKey())
                        .child(NodeConnector.class,nodeConnector.getKey()).toInstance());
                hostNodeConnectors.add(ncr);
            }
        }
    }
    /**
     * check if the node connector is connect to the controller
     * @param nodeConnector
     * @return
     */
    private boolean controllerConnector(NodeConnector nodeConnector){
        String[] s = nodeConnector.getId().getValue().split(":");
        try{
            long tmp = Long.parseLong(s[2]);
            if(tmp>(long)48){
                return true;
            }else{
                return false;
            }
        }catch(Exception ex){
            return true;
        }
    }
}