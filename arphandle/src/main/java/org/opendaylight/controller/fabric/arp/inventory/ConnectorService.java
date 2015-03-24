/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.fabric.arp.inventory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.fabric.arp.util.InstanceIdentifierUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;


public class ConnectorService {
    private final DataBroker dataService;
    private final Hashtable<NodeId,NodeConnectorRef> nodeIdControllerNodeConnector;
    private final Hashtable<NodeId,HashSet<NodeConnectorRef>> nodeIdSwitchNodeConnectors;
    private final Hashtable<NodeId,HashSet<NodeConnectorRef>> nodeIdHostNodeConnectors;
    private final String DEFAULT_TOPOLOGY_ID = "flow:1";

    /**
     * constructor
     * @param dataService
     */
    public ConnectorService(DataBroker dataService){
        this.dataService = dataService;
        this.nodeIdControllerNodeConnector = new Hashtable<NodeId,NodeConnectorRef>();
        this.nodeIdSwitchNodeConnectors = new Hashtable<NodeId,HashSet<NodeConnectorRef>>();
        this.nodeIdHostNodeConnectors = new Hashtable<NodeId,HashSet<NodeConnectorRef>>();
        this.clear();
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
    public void update(){
        Set<String> internalNodeConnectors = new HashSet<>();
        Nodes nodes = null;
        // get all nodes
        InstanceIdentifier<Nodes> nodeIdentifier = InstanceIdentifier.builder(Nodes.class).toInstance();
//        nodes = (Nodes)this.dataService.readOperationalData(nodeIdentifier);
        try {
            ReadOnlyTransaction it = this.dataService.newReadOnlyTransaction();
            Optional<Nodes> dataOptional = it.read(LogicalDatastoreType.OPERATIONAL, nodeIdentifier).get();
            if(dataOptional.isPresent())
                nodes = (Nodes) dataOptional.get();
        } catch(Exception e) {
        }
        // if there is no nodes stop
        if(nodes == null || nodes.getNode().isEmpty()){
            return;
        }

        // get all links
        InstanceIdentifier<Topology> topologyPath = InstanceIdentifier.builder(NetworkTopology.class)
          .child(Topology.class, new TopologyKey(new TopologyId(DEFAULT_TOPOLOGY_ID))).toInstance();
        Topology topology = null;
        try{
            ReadOnlyTransaction it = this.dataService.newReadOnlyTransaction();
            Optional<Topology> dataOptional = it.read(LogicalDatastoreType.OPERATIONAL, topologyPath).get();
            if(dataOptional.isPresent())
                topology = (Topology) dataOptional.get();
        } catch(Exception e) {
        }
        //topology =(Topology)this.dataService.readOperationalData(topologyPath);
        if(topology != null){
            List<Link> links = topology.getLink();
            if(links != null){
                for (Link link : links) {
                    internalNodeConnectors.add(link.getDestination().getDestTp().getValue());
                    internalNodeConnectors.add(link.getSource().getSourceTp().getValue());
                }
            }
        }

        List<Node> listNode = nodes.getNode();
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