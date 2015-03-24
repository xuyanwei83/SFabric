/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.fabric.flow;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.fabric.topology.NetworkGraphService;
import org.opendaylight.controller.fabric.util.InstanceIdentifierUtils;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.mpls.action._case.PopMplsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.enumeration.rev140402.FabricProtocal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.enumeration.rev140402.FabricTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.enumeration.rev140402.FabricTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.model.rev140402.fabric.data.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.model.rev140402.fabric.datas.FabricDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFieldsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Implementation of FlowWriterService{@link org.opendaylight.controller.sample.l2switch.md.flow.FlowWriterService},
 * that builds required flow and writes to configuration data store using provided DataBrokerService
 * {@link org.opendaylight.controller.sal.binding.api.data.DataBrokerService}
 */
public class FlowWriterServiceImpl implements FlowWriterService {
    private static final Logger _logger = LoggerFactory.getLogger(FlowWriterServiceImpl.class);
    private final DataBrokerService dataBrokerService;
    private final NetworkGraphService networkGraphService;
    private AtomicLong flowIdInc = new AtomicLong();
    private AtomicLong flowCookieInc = new AtomicLong(0x2a00000000000000L);
    private final String DEFAULT_TOPOLOGY_ID = "flow:1";
    private FabricTag fabricTagKind = FabricTag.VLAN;

    public FlowWriterServiceImpl(DataBrokerService dataBrokerService, NetworkGraphService networkGraphService) {
        Preconditions.checkNotNull(dataBrokerService, "dataBrokerService should not be null.");
        Preconditions.checkNotNull(networkGraphService, "networkGraphService should not be null.");
        this.dataBrokerService = dataBrokerService;
        this.networkGraphService = networkGraphService;
    }

    @Override
    public void clearAllFlows(Collection<NodeId> nodeIdCollection) {
        //Read node from topology
//        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> nodeInstance = InstanceIdentifier.builder(NetworkTopology.class)
//                .child(Topology.class, new TopologyKey(new TopologyId(DEFAULT_TOPOLOGY_ID)))
//                .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node.class).toInstance();
//        List<Node> listNode = (List<Node>)this.dataBrokerService.readConfigurationData(nodeInstance);
        InstanceIdentifier<Topology> topologyPath = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(DEFAULT_TOPOLOGY_ID))).toInstance();
        Topology completeTopology =(Topology)this.dataBrokerService.readOperationalData(topologyPath);
        if(completeTopology == null){
            return;
        }
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> listNode = completeTopology.getNode();
        if(nodeIdCollection == null || nodeIdCollection.isEmpty()){
            return;
        }
        for(NodeId id:nodeIdCollection){
            //If the node is not removed, remove its flows from data store.
            if(listNode != null && CheckNodeIdInNodeList(listNode,id)){
                this.RemoveFlowsByNodeId(id);
            }else{
                //If the node is removed, remove node from data store.
                this.RemoveNodeByNodeId(id);
            }
        }
        return;
    }
    private boolean CheckNodeIdInNodeList(List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> list,NodeId id){
        for(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node node : list){
            if(node.getNodeId().equals(id)){
                return true;
            }
        }
        return false;
    }

    private void RemoveFlowsByNodeId(NodeId id){
        InstanceIdentifier<Node> nodePath = InstanceIdentifierUtils.createNodePath(new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(id.getValue()));
        InstanceIdentifier<Table> tableSwapPath = InstanceIdentifierUtils.createTablePath(nodePath, new TableKey((short)FabricTable.SwapTagTable.getIntValue()));
        InstanceIdentifier<Table> tableInputPath = InstanceIdentifierUtils.createTablePath(nodePath, new TableKey((short)FabricTable.InPutTable.getIntValue()));
        InstanceIdentifier<Table> tablePushTagPath = InstanceIdentifierUtils.createTablePath(nodePath, new TableKey((short)FabricTable.PushTagTable.getIntValue()));
        DataModificationTransaction it = this.dataBrokerService.beginTransaction();
        it.removeConfigurationData(tableSwapPath);
        it.removeConfigurationData(tableInputPath);
        it.removeConfigurationData(tablePushTagPath);
        it.commit();
    }

    private void RemoveNodeByNodeId(NodeId id){
        try{
            InstanceIdentifier<Node> nodePath = InstanceIdentifierUtils.createNodePath(new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(id.getValue()));
            DataModificationTransaction it = this.dataBrokerService.beginTransaction();
            it.removeConfigurationData(nodePath);
            it.commit();
        }catch(Exception ex){
            _logger.info("RemoveNodeByNodeId Error");
        }
    }

    public void RemoveFlowsByFabricId(Long tag){

    }
    ///////////////////////////////////////////////////////////////////////
    @Override
    public void addNodeFabricBaseFlows(NodeId nodeIdnet,Set<String> internalNodeConnectors){
        TableKey flowTableKey = new TableKey((short)FabricTable.InPutTable.getIntValue());
        //Add Switch Flows
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId nodeId = new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(nodeIdnet.getValue());
        NodeKey nodeKey = new NodeKey(nodeId);
        InstanceIdentifier<Node> nodeIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class,nodeKey).toInstance();
        Node node = (Node) this.dataBrokerService.readOperationalData(nodeIdentifier);
        List<NodeConnector> nodeConnetorList = node.getNodeConnector();
        if(nodeConnetorList == null){
            return;
        }

        for(NodeConnector nodeConnector : nodeConnetorList){
            //Its a switch connector
            if(internalNodeConnectors.contains(nodeConnector.getId().getValue())){
                InstanceIdentifier<NodeConnector> nodeConnectorPath = InstanceIdentifier.builder(Nodes.class) //
                        .child(Node.class, nodeKey).build()
                        .child(NodeConnector.class,nodeConnector.getKey());
                InstanceIdentifier<Flow> flowPath = this.BuildFlowPath(new NodeConnectorRef(nodeConnectorPath), flowTableKey);
                Flow flowBody = this.CreateSwitchInPutFlow(flowTableKey.getId(), 10, nodeConnector);
                this.WriteFlowToConfigData(flowPath, flowBody);
            }
        }
        //Add IP Flows
        FlowId flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
        FlowKey flowKey = new FlowKey(flowId);
        InstanceIdentifier<Node> nodePath = InstanceIdentifierUtils.createNodePath(nodeId);
        InstanceIdentifier<Table> tablePath = InstanceIdentifierUtils.createTablePath(nodePath, flowTableKey);
        InstanceIdentifier<Flow> flowPath = InstanceIdentifierUtils.createFlowPath(tablePath,flowKey);
        Flow flowBody = this.CreateHostInPutFlow(flowTableKey.getId(), 0);
        this.WriteFlowToConfigData(flowPath, flowBody);

        //Add ARP Flows
//        flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
//        flowKey = new FlowKey(flowId);
//        nodePath = InstanceIdentifierUtils.createNodePath(nodeId);
//        tablePath = InstanceIdentifierUtils.createTablePath(nodePath, flowTableKey);
//        flowPath = InstanceIdentifierUtils.createFlowPath(tablePath,flowKey);
//        flowBody = this.CreateARPFlow(flowTableKey.getId(), 0);
//        this.WriteFlowToConfigData(flowPath, flowBody);

        //Add IP MissMatch flow in PushTagTable
        flowTableKey = new TableKey((short)FabricTable.PushTagTable.getIntValue());
        flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
        flowKey = new FlowKey(flowId);
        nodePath = InstanceIdentifierUtils.createNodePath(nodeId);
        tablePath = InstanceIdentifierUtils.createTablePath(nodePath, flowTableKey);
        flowPath = InstanceIdentifierUtils.createFlowPath(tablePath,flowKey);
        flowBody = this.CreateMissMatchFlow(flowTableKey.getId(), 0);
        this.WriteFlowToConfigData(flowPath, flowBody);
        return;
    }

    private Flow CreateSwitchInPutFlow(Short tableId, int priority,NodeConnector nodeConnector){
        FlowBuilder flow = new FlowBuilder() //
        .setTableId(tableId) //
        .setFlowName("InPut:"+nodeConnector.getId().toString());
        // use its own hash code for id.
        flow.setId(new FlowId(Long.toString(flow.hashCode())));

        NodeConnectorId inport = nodeConnector.getId();
        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder();
        EthernetMatch ethernetMatch = null;
        if( this.fabricTagKind == FabricTag.MPLS){
            ethernetMatch = ethernetMatchBuilder
                    .setEthernetType(new EthernetTypeBuilder()
                    //.setType(new EtherType(0x800L))
                    .setType(new EtherType((long)FabricProtocal.MPLS.getIntValue()))
                    .build())
                .build();

        }else{
            ethernetMatch = ethernetMatchBuilder
                    .setEthernetType(new EthernetTypeBuilder()
                    //.setType(new EtherType(0x800L))
                    .setType(new EtherType((long)FabricProtocal.IP.getIntValue()))
                    .build())
                .build();
        }
        Match match = new MatchBuilder()
            .setInPort(inport)
            .setEthernetMatch(ethernetMatch)
         .build();
        // Wrap our GoToTable in an Instruction
        GoToTable gotoTable = new GoToTableBuilder().setTableId((short)FabricTable.SwapTagTable.getIntValue()).build();
        Instruction goToTableInstruction = new InstructionBuilder() //
            .setOrder(1)
            .setInstruction(new GoToTableCaseBuilder()
                 .setGoToTable(gotoTable)
                 .build())
             .build();

        // Put our Instruction in a list of Instructions
        flow
            .setMatch(match) //
            .setInstructions(new InstructionsBuilder() //
                .setInstruction(ImmutableList.of(goToTableInstruction)) //
                .build()) //
            .setPriority(priority) //
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(0) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return flow.build();
    }
    private Flow CreateHostInPutFlow(Short tableId, int priority){
        FlowBuilder flow = new FlowBuilder() //
        .setTableId(tableId) //
        .setFlowName("HostInPut");
        // use its own hash code for id.
        flow.setId(new FlowId(Long.toString(flow.hashCode())));

        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder();
        EthernetMatch ethernetMatch = ethernetMatchBuilder
                .setEthernetType(new EthernetTypeBuilder()
                //.setType(new EtherType(0x800L))
                .setType(new EtherType((long)FabricProtocal.IP.getIntValue()))
                .build())
            .build();

        Match match = new MatchBuilder()
            .setEthernetMatch(ethernetMatch)
         .build();
        // Wrap our GoToTable in an Instruction
        GoToTable gotoTable = new GoToTableBuilder().setTableId((short)FabricTable.PushTagTable.getIntValue()).build();
        Instruction goToTableInstruction = new InstructionBuilder() //
            .setOrder(1)
            .setInstruction(new GoToTableCaseBuilder()
                 .setGoToTable(gotoTable)
                 .build())
             .build();

        // Put our Instruction in a list of Instructions
        flow
            .setMatch(match) //
            .setInstructions(new InstructionsBuilder() //
                .setInstruction(ImmutableList.of(goToTableInstruction)) //
                .build()) //
            .setPriority(priority) //
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(0) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return flow.build();
    }

    private Flow CreateMissMatchFlow(Short tableId, int priority){
        FlowBuilder flow = new FlowBuilder() //
            .setTableId(tableId) //
            .setFlowName("MissMatch");
        // use its own hash code for id.
        flow.setId(new FlowId(Long.toString(flow.hashCode())));

        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder();
        EthernetMatch ethernetMatch = ethernetMatchBuilder
                .setEthernetType(new EthernetTypeBuilder()
                .setType(new EtherType((long)FabricProtocal.IP.getIntValue()))
                .build())
            .build();

        Match match = new MatchBuilder()
            .setEthernetMatch(ethernetMatch)
         .build();

        Uri destPortUri = new Uri(OutputPortValues.CONTROLLER.toString());

        Action outputToControllerAction = new ActionBuilder()
            .setOrder(1)//
            .setAction(new OutputActionCaseBuilder() //
                .setOutputAction(new OutputActionBuilder() //
                    .setMaxLength(new Integer(0xffff)) //
                    .setOutputNodeConnector(destPortUri) //
                    .build()) //
                .build()) //
            .build();

        // Create an Apply Action
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.of(outputToControllerAction))
            .build();

        // Wrap our Apply Action in an Instruction
        Instruction applyActionsInstruction = new InstructionBuilder()
            .setOrder(1)//
            .setInstruction(new ApplyActionsCaseBuilder()//
                .setApplyActions(applyActions) //
                .build()) //
            .build();

        // Put our Instruction in a list of Instructions
        flow
            .setMatch(match) //
            .setInstructions(new InstructionsBuilder() //
                .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                .build()) //
            .setPriority(priority) //
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(0) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return flow.build();
    }

    private Flow CreateARPFlow(Short tableId, int priority){
        FlowBuilder flow = new FlowBuilder() //
        .setTableId(tableId) //
        .setFlowName("HostARP");
        // use its own hash code for id.
        flow.setId(new FlowId(Long.toString(flow.hashCode())));

        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder();
        EthernetMatch ethernetMatch = ethernetMatchBuilder
                .setEthernetType(new EthernetTypeBuilder()
                //.setType(new EtherType(0x800L))
                .setType(new EtherType((long)FabricProtocal.ARP.getIntValue()))
                .build())
            .build();

        Match match = new MatchBuilder()
            .setEthernetMatch(ethernetMatch)
         .build();
        // Wrap our GoToTable in an Instruction
        Uri destPortUri = new Uri(OutputPortValues.CONTROLLER.toString());

        Action outputToControllerAction = new ActionBuilder()
            .setOrder(1)//
            .setAction(new OutputActionCaseBuilder() //
                .setOutputAction(new OutputActionBuilder() //
                    .setMaxLength(new Integer(0xffff)) //
                    .setOutputNodeConnector(destPortUri) //
                    .build()) //
                .build()) //
            .build();

        // Create an Apply Action
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.of(outputToControllerAction))
            .build();

        // Wrap our Apply Action in an Instruction
        Instruction applyActionsInstruction = new InstructionBuilder()
            .setOrder(1)//
            .setInstruction(new ApplyActionsCaseBuilder()//
                .setApplyActions(applyActions) //
                .build()) //
            .build();


        // Put our Instruction in a list of Instructions
        flow
            .setMatch(match) //
            .setInstructions(new InstructionsBuilder() //
                .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                .build()) //
            .setPriority(priority) //
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(0) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return flow.build();
    }

    /////////////////////////////////////////////////////////////
    //@Override
    public List<Link> addNodeToNodeFabricFlows(NodeId srcNodeId, NodeId dstNodeId,long tag) {
        // TODO Auto-generated method stub
        FabricDataBuilder ret = new FabricDataBuilder();
        ret.setTag(tag);
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.model.rev140402.fabric.data.Link> list = new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.model.rev140402.fabric.data.Link>();
        LinkBuilder linkBuilder;
        //Get Path
        List<Link> linksInBeween = networkGraphService.getPath(srcNodeId, dstNodeId);

        if(linksInBeween != null) {
            Link last = null;
            // assumes the list order is maintained and starts with link that has source as source node
            for(Link link : linksInBeween) {
                linkBuilder = new LinkBuilder(link);
                list.add(linkBuilder.build());
                this.AddFabricFlow(tag, this.GetSourceNodeConnectorRef(link),false);
                last = link;
            }
            this.AddFabricFlow(tag, this.GetDestNodeConnectorRef(last),true);
        }
        ret.setLink(list);
        return linksInBeween;
    }

    private void AddFabricFlow(long tag, NodeConnectorRef destNodeConnectorRef, boolean isLastNode) {
        // get flow table key
        TableKey flowTableKey = new TableKey((short)FabricTable.SwapTagTable.getIntValue());

        //build a flow path based on node connector to program flow
        InstanceIdentifier<Flow> flowPath = this.BuildFlowPath(destNodeConnectorRef, flowTableKey);

        // build a flow that target given arguments
        Flow flowBody = null;
        if(!isLastNode){
            flowBody = this.CreateFabricFlow(flowTableKey.getId(), 0, tag, destNodeConnectorRef);
        }else{
            flowBody = this.CreateFabricLastFlow(flowTableKey.getId(), 0, tag, destNodeConnectorRef);
        }

        // commit the flow in config data
        this.WriteFlowToConfigData(flowPath, flowBody);
    }

    private Flow CreateFabricFlow(Short tableId, int priority, long tag, NodeConnectorRef destPort){
        // start building flow
        FlowBuilder fabricFlow = new FlowBuilder()
            .setTableId(tableId);
        if(this.fabricTagKind == FabricTag.MPLS){
            fabricFlow.setFlowName("MPLS" + tag);
        }else{
            fabricFlow.setFlowName("VLAN" + tag);
        }

        // use its own hash code for id.
        fabricFlow.setId(new FlowId(Long.toString(fabricFlow.hashCode())));

        Uri destPortUri = destPort.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();

        Action outputToControllerAction = new ActionBuilder()
            .setOrder(1)//
            .setAction(new OutputActionCaseBuilder() //
                .setOutputAction(new OutputActionBuilder() //
                    .setMaxLength(new Integer(0xffff)) //
                    .setOutputNodeConnector(destPortUri) //
                    .build()) //
                .build()) //
            .build();

        // Create an Apply Action
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.of(outputToControllerAction))
            .build();

        // Wrap our Apply Action in an Instruction
        Instruction applyActionsInstruction = new InstructionBuilder()
            .setOrder(1)//
            .setInstruction(new ApplyActionsCaseBuilder()//
                .setApplyActions(applyActions) //
                .build()) //
            .build();

        Match match = null;
        if(this.fabricTagKind == FabricTag.MPLS){
            match = this.GetMPLSMatch(tag);
        }else{
            match = this.GetVLANMatch(tag);
        }

        // Put our Instruction in a list of Instructions
        fabricFlow
            .setMatch(match) //
            .setInstructions(new InstructionsBuilder() //
                .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                .build()) //
            .setPriority(priority) //
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(0) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return fabricFlow.build();
    }

    private Flow CreateFabricLastFlow(Short tableId, int priority, long tag, NodeConnectorRef destPort){
     // start building flow
        FlowBuilder fabricFlow = new FlowBuilder() //
            .setTableId(tableId); //
        if(this.fabricTagKind == FabricTag.MPLS){
            fabricFlow.setFlowName("MPLS" + tag);
        }else{
            fabricFlow.setFlowName("VLAN" + tag);
        }
        // use its own hash code for id.
        fabricFlow.setId(new FlowId(Long.toString(fabricFlow.hashCode())));

//        // create a match that has mpls match
//        ProtocolMatchFieldsBuilder protocolMatchFieldsBuilder = new ProtocolMatchFieldsBuilder()
//            .setMplsLabel(tag);
//        ProtocolMatchFields protocolMatch = protocolMatchFieldsBuilder.build();
//        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder();
//        EthernetMatch ethernetMatch = ethernetMatchBuilder
//                .setEthernetType(new EthernetTypeBuilder()
//                    //.setType(new EtherType(0x8847L))
//                    .setType(new EtherType((long)FabricProtocal.MPLS.getIntValue()))
//                    .build())
//                .build();
//        Match match = new MatchBuilder()
//            .setProtocolMatchFields(protocolMatch)
//            .setEthernetMatch(ethernetMatch)
//            .build();

        /*
         * Instruction 1: pop tag
         */
//        Action popMPLSAction = new ActionBuilder()
//            .setOrder(1)
//            .setAction(new PopMplsActionCaseBuilder()
//                .setPopMplsAction(new PopMplsActionBuilder()
//                    //.setEthernetType(new Integer(0x800))
//                    .setEthernetType(FabricProtocal.IP.getIntValue())
//                    .build())
//                .build())
//            .build();

        Match match = null;
        Action popTagAction = null;
        if(this.fabricTagKind == FabricTag.MPLS){
            match = this.GetMPLSMatch(tag);
            popTagAction = this.PopMPLSAction();
        }else{
            match = this.GetVLANMatch(tag);
            popTagAction = this.PopVLANAction();
        }

        // Create an Apply Action
        ApplyActions applyActions = new ApplyActionsBuilder()
            .setAction(ImmutableList.of(popTagAction))
            .build();

        // Wrap our Apply Action in an Instruction
        Instruction applyActionsInstruction = new InstructionBuilder()
            .setOrder(1)
            .setInstruction(new ApplyActionsCaseBuilder()//
                .setApplyActions(applyActions) //
                .build()) //
            .build();

        /*
         * Instruction 2: goto talbe:OutPutTable
         */
        // Wrap our Apply Action in an Instruction
        GoToTable gotoTable = new GoToTableBuilder()
            .setTableId((short)FabricTable.OutPutTable.getIntValue())
            .build();

        // Wrap our Apply Action in an Instruction
        Instruction gotoTableInstruction = new InstructionBuilder() //
            .setOrder(2)
            .setInstruction(new GoToTableCaseBuilder()
                 .setGoToTable(gotoTable)
                 .build())
             .build();

        // Put our Instruction in a list of Instructions
        fabricFlow
            .setMatch(match) //
            .setInstructions(new InstructionsBuilder() //
                .setInstruction(ImmutableList.of(applyActionsInstruction,gotoTableInstruction)) //
                .build()) //
            .setPriority(priority) //
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(0) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return fabricFlow.build();
    }
    /**
     * @param nodeConnectorRef
     * @return
     */
    private InstanceIdentifier<Flow> BuildFlowPath(NodeConnectorRef nodeConnectorRef, TableKey flowTableKey) {

      // generate unique flow key
      FlowId flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
      FlowKey flowKey = new FlowKey(flowId);

      return InstanceIdentifierUtils.generateFlowInstanceIdentifier(nodeConnectorRef, flowTableKey, flowKey);
    }

    /**
     * Starts and commits data change transaction which
     * modifies provided flow path with supplied body.
     *
     * @param flowPath
     * @param flowBody
     * @return transaction commit
     */
    private Future<RpcResult<TransactionStatus>> WriteFlowToConfigData(InstanceIdentifier<Flow> flowPath,
                                                                       Flow flowBody) {
      DataModificationTransaction addFlowTransaction = dataBrokerService.beginTransaction();
      addFlowTransaction.putConfigurationData(flowPath, flowBody);
      return addFlowTransaction.commit();
    }

    private NodeConnectorRef GetSourceNodeConnectorRef(Link link) {
        InstanceIdentifier<NodeConnector> nodeConnectorInstanceIdentifier
            = InstanceIdentifierUtils.createNodeConnectorIdentifier(
            link.getSource().getSourceNode().getValue(),
            link.getSource().getSourceTp().getValue());
        return new NodeConnectorRef(nodeConnectorInstanceIdentifier);
      }

    private NodeConnectorRef GetDestNodeConnectorRef(Link link) {
        InstanceIdentifier<NodeConnector> nodeConnectorInstanceIdentifier
            = InstanceIdentifierUtils.createNodeConnectorIdentifier(
            link.getDestination().getDestNode().getValue(),
            link.getDestination().getDestTp().getValue());
        return new NodeConnectorRef(nodeConnectorInstanceIdentifier);
    }

    private Match GetMPLSMatch(long tag){
        // create a match that has mpls match
        ProtocolMatchFieldsBuilder protocolMatchFieldsBuilder = new ProtocolMatchFieldsBuilder()
            .setMplsLabel(tag);
        ProtocolMatchFields protocolMatch = protocolMatchFieldsBuilder.build();
        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder();
        EthernetMatch ethernetMatch = ethernetMatchBuilder
                .setEthernetType(new EthernetTypeBuilder()
                    //.setType(new EtherType(0x8847L))
                    .setType(new EtherType((long)FabricProtocal.MPLS.getIntValue()))
                    .build())
                .build();

        Match match = new MatchBuilder()
            .setProtocolMatchFields(protocolMatch)
            .setEthernetMatch(ethernetMatch)
            .build();
        return match;
    }

    private Match GetVLANMatch(long tag){
        // create a match that has vlan match
//        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder();
//        EthernetMatch ethernetMatch = ethernetMatchBuilder
//                .setEthernetType(new EthernetTypeBuilder()
//                    .setType(new EtherType((long)FabricProtocal.VLAN.getIntValue()))
//                    .build())
//                .build();
        VlanMatch vlanMatch = new VlanMatchBuilder()
                .setVlanId(new VlanIdBuilder()
                    .setVlanId(new VlanId((int)tag))
                    .setVlanIdPresent(true)
                    .build())
                .build();

        Match match = new MatchBuilder()
//            .setEthernetMatch(ethernetMatch)
            .setVlanMatch(vlanMatch)
            .build();
        return match;
    }

    private Action PopMPLSAction(){
        Action popMPLSAction = new ActionBuilder()
        .setOrder(1)
        .setAction(new PopMplsActionCaseBuilder()
            .setPopMplsAction(new PopMplsActionBuilder()
                //.setEthernetType(new Integer(0x800))
                .setEthernetType(FabricProtocal.IP.getIntValue())
                .build())
            .build())
        .build();
        return popMPLSAction;
    }

    private Action PopVLANAction(){
        Action popVLANAction = new ActionBuilder()
        .setOrder(1)
        .setAction(new PopVlanActionCaseBuilder()
            .setPopVlanAction(new PopVlanActionBuilder()
                .build())
            .build())
        .build();
        return popVLANAction;
    }

    @Override
    public void addNodeFabricBaseFlowsByLinks(List<Link> listLink) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addNodeToNodeFabricFlows(List<Link> linkList, long tag) {
        // TODO Auto-generated method stub
    }

    @Override
    public void deleteNodeFabricFlows(String nodeId, long tag) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addFabricLastFlow(NodeId nodeId, long tag) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addFabricMiddleFlow(NodeId nodeId, long tag, Uri destPort) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addFabricFirstFlow(NodeId nodeId, long tag, Uri destPort) {
        // TODO Auto-generated method stub
    }

    @Override
    public void downloadAllFabricFlows() {
        // TODO Auto-generated method stub
    }

    @Override
    public void downloadFabricFlowsByNodeId(NodeId nodeId,long sleepTime) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addTempFirstFlow(NodeId nodeId,long tag,Uri destPort) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addTempMiddleFlow(NodeId nodeId, long tag, Uri destPort) {
        // TODO Auto-generated method stub
    }

//    @Override
//    public void SetTagKind(FabricTag fabricTagKind) {
//        // TODO Auto-generated method stub
//        this.fabricTagKind = fabricTagKind;
//    }
}
