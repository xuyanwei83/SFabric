/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.fabric.arp.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.nodes.rev140402.FabricNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.nodes.rev140402.fabric.node.Extern;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.nodes.rev140402.fabric.nodes.FabricNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.nodes.rev140402.fabric.nodes.FabricNodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

//import java.util.*;

/**
 * InventoryService provides functions related to Nodes & NodeConnectors.
 */
public class InventoryService {
  private DataBrokerService dataService;
  // Key: SwitchId, Value: NodeConnectorRef that corresponds to NC between controller & switch
  private HashMap<String, NodeConnectorRef> controllerSwitchConnectors;
  //private ArrayList<NodeConnectorRef> externalNodeConnectors;

  private String DEFAULT_TOPOLOGY_ID = "flow:1";
  /**
   * Construct an InventoryService object with the specified inputs.
   * @param dataService  The DataBrokerService associated with the InventoryService.
   */
  public InventoryService(DataBrokerService dataService) {
    this.dataService = dataService;
    this.controllerSwitchConnectors = new HashMap<String, NodeConnectorRef>();
  }

  public HashMap<String, NodeConnectorRef> getControllerSwitchConnectors() {
    return controllerSwitchConnectors;
  }
  public List<NodeConnectorRef> getExternalNodeConnectors() {
      // External NodeConnectors = All - Internal
      ArrayList<NodeConnectorRef> externalNodeConnectors = new ArrayList<NodeConnectorRef>();
      // Get all nodes
      InstanceIdentifier.InstanceIdentifierBuilder<Nodes> nodesInsIdBuilder = InstanceIdentifier.<Nodes>builder(Nodes.class);
      Nodes nodes = (Nodes)dataService.readOperationalData(nodesInsIdBuilder.toInstance());
      if (nodes != null) {
          for (Node node : nodes.getNode()) {
              InstanceIdentifier<FabricNode> path = InstanceIdentifier.builder(FabricNodes.class)
                      .child(FabricNode.class,new FabricNodeKey(node.getId())).toInstance();
              FabricNode fabricNode = (FabricNode)this.dataService.readOperationalData(path);
              if(fabricNode != null){
                  List<Extern> list = fabricNode.getExtern();
                  for(Extern ex : list){
                      NodeConnector nc = new NodeConnectorBuilder().setId(ex.getId()).build();
                      NodeConnectorRef ncRef = new NodeConnectorRef(InstanceIdentifier.builder(Nodes.class).child(Node.class,node.getKey()).child(NodeConnector.class,nc.getKey()).toInstance());
                      externalNodeConnectors.add(ncRef);
                  }
              }
          }
      }
      return externalNodeConnectors;
  }
    /**
    * Gets the NodeConnector that connects the controller & switch for a specified switch port/node connector.
    * @param nodeConnectorRef  The nodeConnector of a switch.
    * @return  The NodeConnector that that connects the controller & switch.
    */
    public NodeConnectorRef getControllerNodeConnector(NodeConnectorRef nodeConnectorRef) {
        NodeId nodeId = nodeConnectorRef.getValue().firstKeyOf(Node.class,NodeKey.class).getId();
        InstanceIdentifier<FabricNode> path = InstanceIdentifier.builder(FabricNodes.class)
                .child(FabricNode.class,new FabricNodeKey(nodeId)).toInstance();
        FabricNode fabricNode = (FabricNode)this.dataService.readOperationalData(path);
        if(fabricNode != null){
            return fabricNode.getController();
//            List<Intern> list = fabricNode.getIntern();
//            for(Intern in : list){
//                String[] s = in.getId().getValue().split(":");
//                if(Long.parseLong(s[2])>(long)48){
//                    NodeConnector nc = new NodeConnectorBuilder().setId(in.getId()).build();
//                    NodeConnectorRef ncRef = new NodeConnectorRef(InstanceIdentifier.builder(Nodes.class).child(Node.class,new NodeKey(nodeId)).child(NodeConnector.class,nc.getKey()).toInstance());
//                    return ncRef;
//                }
//            }
        }

        return null;
    }
}