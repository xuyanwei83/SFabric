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
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.fabric.topology.NetworkGraphService;
import org.opendaylight.controller.fabric.util.FabricNodeFlows;
import org.opendaylight.controller.fabric.util.FabricNodeFlowsUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.enumeration.rev140402.FabricProtocal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.enumeration.rev140402.FabricTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.collect.ImmutableList;

/**
 * Implementation of FlowWriterService{@link org.opendaylight.controller.sample.l2switch.md.flow.FlowWriterService},
 * that builds required flow and writes to configuration data store using provided DataBroker
 * {@link org.opendaylight.controller.sal.binding.api.data.DataBroker}
 */
public class FlowWriterServiceVlanMix implements FlowWriterService {
    private final DataBroker dataBroker;
    private final SalFlowService flowService;
    private final NetworkGraphService networkGraphService;
    private AtomicLong flowCookieInc = new AtomicLong(0x2a00000000000000L);
    private final FabricNodeFlowsUtils fabricNodeFlowsUtils;

    // consts
    private final int prioritySwitchInPutFlow = 10;
    private final int priorityHostInPutFlow = 0;
    private final int priorityMissMatchPushTagFlow = 0;
    private final int priorityFabricSwapTagFlow = 20;
    private final int priorityArpGotoControllerTagFlow = 5;
    private final int priorityTempSwapTagFlow = 40;
    private final int idleTimeoutTemp = 100;

    public FlowWriterServiceVlanMix(DataBroker dataBroker, NetworkGraphService networkGraphService,SalFlowService flowService) {
        this.dataBroker = dataBroker;
        this.networkGraphService = networkGraphService;
        this.flowService = flowService;
        this.fabricNodeFlowsUtils = new FabricNodeFlowsUtils();
    }

    /**
     * clear flows
     */
    @Override
    public void clearAllFlows(Collection<NodeId> nodeIdCollection) {
        //Read node from topology
        if(nodeIdCollection == null || nodeIdCollection.isEmpty()){
            return;
        }
        for(NodeId id:nodeIdCollection){
            this.clearAllFlowsByNodeId(id);
        }
        return;
    }

    /**
     * clear flows by nodeId
     * @param id
     */
    private void clearAllFlowsByNodeId(NodeId id){
        // build NodeRef
        NodeRef nr = this.createNodeRef(id);
        FabricNodeFlows f = this.fabricNodeFlowsUtils.getFabricNodeFlowsByNodeId(nr);
        f.init();

        // build match without any match in order to delete all flows
        Match match = new MatchBuilder().build();
        // build flow table all
//        Flow flow = new FlowBuilder().setTableId((short)0xff).setMatch(match).build();
//        RemoveFlowInput input = new RemoveFlowInputBuilder(flow).setNode(nrf).build();
//        this.flowService.removeFlow(input);

        // build flow table "InPutTable"
        Flow flow = new FlowBuilder().setTableId((short)FabricTable.InPutTable.getIntValue()).setMatch(match).build();
        RemoveFlowInput input = new RemoveFlowInputBuilder(flow).setNode(nr).build();
        this.flowService.removeFlow(input);

        //build match table "PushTagTable"
        flow = new FlowBuilder().setTableId((short)FabricTable.PushTagTable.getIntValue()).setMatch(match).build();
        input = new RemoveFlowInputBuilder(flow).setNode(nr).build();
        this.flowService.removeFlow(input);

        //build match table "PushTagTable"
        flow = new FlowBuilder().setTableId((short)FabricTable.SwapTagTable.getIntValue()).setMatch(match).build();
        input = new RemoveFlowInputBuilder(flow).setNode(nr).build();
        this.flowService.removeFlow(input);

        return;
    }
    /**
     * add fabric base flows
     */
    @Override
    public void addNodeFabricBaseFlows(NodeId nodeIdnet,Set<String> internalNodeConnectors){
        // build NodeKey
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId nodeId = new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(nodeIdnet.getValue());
        NodeKey nodeKey = new NodeKey(nodeId);
        // build InstanceIdentifier of node
        InstanceIdentifier<Node> nodeIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class,nodeKey).toInstance();
        NodeRef nr = new NodeRef( nodeIdentifier);

        // get FabricNodeFlows f
        FabricNodeFlows f = this.fabricNodeFlowsUtils.getFabricNodeFlowsByNodeId(nr);

        // add host input IP flow
        Flow flowBody = this.createHostInPutFlow();
        AddFlowInput inputIP = new AddFlowInputBuilder(flowBody).setNode(nr).build();
        this.flowService.addFlow(inputIP);
        // add host input IP flow body
        f.addHostInputFlow(flowBody);

        // add IP MissMatch flow in PushTagTable
        flowBody = this.createMissMatchFlow();
        AddFlowInput inputIPMissMatch = new AddFlowInputBuilder(flowBody).setNode(nr).build();
        this.flowService.addFlow(inputIPMissMatch);
        // add IP MissMatch flow in PushTagTable
        f.addIPMissMatchPushtagFlow(flowBody);

        // add Arp MissMatch flow in PushTagTable
        flowBody = this.createARPGotoControllerFlow();
        AddFlowInput inputARPMissMatch = new AddFlowInputBuilder(flowBody).setNode(nr).build();
        this.flowService.addFlow(inputARPMissMatch);
        // add Arp MissMatch flow in PushTagTable
        f.addARPMissMatchPushtagFlow(flowBody);

        return;
    }
    @Override
    public void addNodeFabricBaseFlowsByLinks(List<Link> listLink) {
        // TODO Auto-generated method stub
        if(listLink != null){
            Iterator<Link> iLink = listLink.iterator();
            while(iLink.hasNext()){
                Link link = iLink.next();
                this.addLinkFlow(link.getDestination().getDestNode(), link.getDestination().getDestTp().getValue());
                this.addLinkFlow(link.getSource().getSourceNode(), link.getSource().getSourceTp().getValue());
            }
        }
        return;
    }
    private void addLinkFlow(NodeId id,String ncs){

        NodeConnector nc = new NodeConnectorBuilder()
                .setId(new NodeConnectorId(ncs)).build();
        // build InstanceIdentifier of node
        NodeRef nr = this.createNodeRef(id);
        Flow flowBody = this.createSwitchInPutFlow(nc);
        AddFlowInput input = new AddFlowInputBuilder(flowBody).setNode(nr).build();
        this.flowService.addFlow(input);
        // add switch input flow
        FabricNodeFlows f = this.fabricNodeFlowsUtils.getFabricNodeFlowsByNodeId(nr);
        f.addSwitchInputFlow(flowBody);
        return;
    }
    /**
     * create switch input flows
     * @param nodeConnector
     * @return
     */
    private Flow createSwitchInPutFlow(NodeConnector nodeConnector){
        // create a FlowBuilder object
        FlowBuilder flow = new FlowBuilder()
            .setTableId((short)FabricTable.InPutTable.getIntValue())
            .setFlowName("InPut:"+nodeConnector.getId().toString());

        // create flow id
        flow.setId(new FlowId(Long.toString(flow.hashCode())));

        // create match
        NodeConnectorId inport = nodeConnector.getId();
        EthernetMatch ethernetMatch = new EthernetMatchBuilder()
            .setEthernetType(new EthernetTypeBuilder()
                .setType(new EtherType((long)FabricProtocal.IP.getIntValue()))
                .build())
            .build();
        Match match = new MatchBuilder()
            .setInPort(inport)
            .setEthernetMatch(ethernetMatch)
            .build();

        // create GoToTable in an Instruction
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
            .setPriority(this.prioritySwitchInPutFlow) //
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(0) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return flow.build();
    }

    /**
     * create host input flow
     * @return
     */
    private Flow createHostInPutFlow(){
        // create a FlowBuilder object
        FlowBuilder flow = new FlowBuilder()
            .setTableId((short)FabricTable.InPutTable.getIntValue())
            .setFlowName("HostInPut");

        // create flow id
        flow.setId(new FlowId(Long.toString(flow.hashCode())));

        // create match
        EthernetMatch ethernetMatch = new EthernetMatchBuilder()
            .setEthernetType(new EthernetTypeBuilder()
                .setType(new EtherType((long)FabricProtocal.IP.getIntValue()))
                .build())
            .build();

        Match match = new MatchBuilder()
            .setEthernetMatch(ethernetMatch)
         .build();

        // create GoToTable in an Instruction
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
            .setPriority(this.priorityHostInPutFlow)
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(0) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return flow.build();
    }

    /**
     * create miss match flow
     * @return
     */
    private Flow createMissMatchFlow(){
        // create a FlowBuilder object
        FlowBuilder flow = new FlowBuilder() //
            .setTableId((short)FabricTable.PushTagTable.getIntValue()) //
            .setFlowName("MissMatch");

        // create flow id
        flow.setId(new FlowId(Long.toString(flow.hashCode())));

        // create match
        EthernetMatch ethernetMatch = new EthernetMatchBuilder()
            .setEthernetType(new EthernetTypeBuilder()
                .setType(new EtherType((long)FabricProtocal.IP.getIntValue()))
                .build())
            .build();

        Match match = new MatchBuilder()
            .setEthernetMatch(ethernetMatch)
         .build();

        // create out put uri
        Uri destPortUri = new Uri(OutputPortValues.CONTROLLER.toString());

        // create out put action
        Action outputToControllerAction = new ActionBuilder()
            .setOrder(1)//
            .setAction(new OutputActionCaseBuilder() //
                .setOutputAction(new OutputActionBuilder() //
                    .setMaxLength(new Integer(0xffff)) //
                    .setOutputNodeConnector(destPortUri) //
                    .build()) //
                .build()) //
            .build();

        // create an apply action
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.of(outputToControllerAction))
            .build();

        // wrap our apply action in an Instruction
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
            .setPriority(this.priorityMissMatchPushTagFlow) //
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(0) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return flow.build();
    }
    /**
     * create miss match flow
     * @return
     */
    private Flow createARPGotoControllerFlow(){
        // create a FlowBuilder object
        FlowBuilder flow = new FlowBuilder() //
            .setTableId((short)FabricTable.InPutTable.getIntValue()) //
            .setFlowName("ARP");

        // create flow id
        flow.setId(new FlowId(Long.toString(flow.hashCode())));

        // create match
        EthernetMatch ethernetMatch = new EthernetMatchBuilder()
            .setEthernetType(new EthernetTypeBuilder()
                .setType(new EtherType((long)FabricProtocal.ARP.getIntValue()))
                .build())
            .build();

        Match match = new MatchBuilder()
            .setEthernetMatch(ethernetMatch)
         .build();

        // create out put uri
        Uri destPortUri = new Uri(OutputPortValues.CONTROLLER.toString());

        // create out put action
        Action outputToControllerAction = new ActionBuilder()
            .setOrder(1)//
            .setAction(new OutputActionCaseBuilder() //
                .setOutputAction(new OutputActionBuilder() //
                    .setMaxLength(new Integer(0xffff)) //
                    .setOutputNodeConnector(destPortUri) //
                    .build()) //
                .build()) //
            .build();

        // create an apply action
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.of(outputToControllerAction))
            .build();

        // wrap our apply action in an Instruction
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
            .setPriority(this.priorityArpGotoControllerTagFlow) //
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(0) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return flow.build();
    }
    /**
     * add a from source to destination path
     */
    @Override
    public List<Link> addNodeToNodeFabricFlows(NodeId srcNodeId, NodeId dstNodeId,long tag) {
        // get the path
        List<Link> linkList = networkGraphService.getPath(srcNodeId, dstNodeId);
        if(linkList == null || linkList.isEmpty()){
            return null;
        }
        NodeRef nr =null;
        Flow flow = null;
        Uri destPort = null;
        AddFlowInput input = null;
        FabricNodeFlows f = null;

        // create first node fabric flow
        ArrayList<Link> linkTempList = new ArrayList<Link>(linkList);
        Link tempLink = linkTempList.remove(0);
        nr = this.createNodeRef(srcNodeId);
        f = this.fabricNodeFlowsUtils.getFabricNodeFlowsByNodeId(nr);
        if(f.getFabricSwapFlowByTag(tag)==null){
            destPort = tempLink.getSource().getSourceTp();
            flow = this.createFabricFirstFlow(tag, destPort);
            input = new AddFlowInputBuilder(flow).setNode(nr).build();
            f.addFabricSwapFlow(tag, flow);
            this.flowService.addFlow(input);
        }
        //this.addFabricFirstFlow(tag, this.getSourceNodeConnectorRef(tempLink));

        // create middle nodes fabric flow
        Iterator<Link> linkIterator = linkTempList.iterator();
        while(linkIterator.hasNext()){
            tempLink = linkIterator.next();
            nr = this.createNodeRef(tempLink.getSource().getSourceNode());
            f = this.fabricNodeFlowsUtils.getFabricNodeFlowsByNodeId(nr);
            if(f.getFabricInputFlowByTag(tag) == null){
                destPort = tempLink.getSource().getSourceTp();
                flow = this.createFabricFlow(tag, destPort);
                input = new AddFlowInputBuilder(flow).setNode(nr).build();
                f.addFabricInputFlow(tag, flow);
                this.flowService.addFlow(input);
            }
            //this.addFabricFlow(tag,this.getSourceNodeConnectorRef(tempLink));
        }

        // create last node fabric flow
        nr = this.createNodeRef(dstNodeId);
        f = this.fabricNodeFlowsUtils.getFabricNodeFlowsByNodeId(nr);
        if(f.getFabricSwapFlowByTag(tag) == null){
            flow = this.createFabricLastFlow(tag);
            input = new AddFlowInputBuilder(flow).setNode(nr).build();
            f.addFabricSwapFlow(tag, flow);
            this.flowService.addFlow(input);
        }
        //this.addFabricLastFlow(tag, this.getDestNodeConnectorRef(tempLink));
        return linkList;
    }

    @Override
    public void addNodeToNodeFabricFlows(List<Link> linkList,long tag) {
        NodeRef nr =null;
        Flow flow = null;
        Uri destPort = null;
        AddFlowInput input = null;
        FabricNodeFlows f = null;

        // create first node fabric flow
        ArrayList<Link> linkTempList = new ArrayList<Link>(linkList);
        Link tempLink = linkTempList.remove(0);
        nr = this.createNodeRef(tempLink.getSource().getSourceNode());
        f = this.fabricNodeFlowsUtils.getFabricNodeFlowsByNodeId(nr);
        if(f.getFabricSwapFlowByTag(tag)==null){
            destPort = tempLink.getSource().getSourceTp();
            flow = this.createFabricFirstFlow(tag, destPort);
            input = new AddFlowInputBuilder(flow).setNode(nr).build();
            f.addFabricSwapFlow(tag, flow);
            this.createFlowWithCheck(input);
            //this.flowService.addFlow(input);
        }

        // create middle nodes fabric flow
        Iterator<Link> linkIterator = linkTempList.iterator();
        while(linkIterator.hasNext()){
            tempLink = linkIterator.next();
            nr = this.createNodeRef(tempLink.getSource().getSourceNode());
            f = this.fabricNodeFlowsUtils.getFabricNodeFlowsByNodeId(nr);
            if(f.getFabricInputFlowByTag(tag) == null){
                destPort = tempLink.getSource().getSourceTp();
                flow = this.createFabricFlow(tag, destPort);
                input = new AddFlowInputBuilder(flow).setNode(nr).build();
                f.addFabricInputFlow(tag, flow);
                this.createFlowWithCheck(input);
                //this.flowService.addFlow(input);
            }
        }

        // create last node fabric flow
        nr = this.createNodeRef(tempLink.getDestination().getDestNode());
        f = this.fabricNodeFlowsUtils.getFabricNodeFlowsByNodeId(nr);
        if(f.getFabricSwapFlowByTag(tag) == null){
            flow = this.createFabricLastFlow(tag);
            input = new AddFlowInputBuilder(flow).setNode(nr).build();
            f.addFabricSwapFlow(tag, flow);
            this.createFlowWithCheck(input);
            //this.flowService.addFlow(input);
        }
        return;
    }

    /**
     * create fabric flow in first node
     * @param tag
     * @param destPort
     * @return
     */
    private Flow createFabricFirstFlow(long tag, Uri destPort){
        // create a FlowBuilder object
        FlowBuilder fabricFlow = new FlowBuilder()
            .setTableId((short)FabricTable.SwapTagTable.getIntValue())
            .setFlowName("VLAN" + tag);

        // create flow id
        fabricFlow.setId(new FlowId(Long.toString(fabricFlow.hashCode())));

        // create vlan match
        VlanMatch vlanMatch = new VlanMatchBuilder()
            .setVlanId(new VlanIdBuilder()
                .setVlanId(new VlanId((int)tag))
                .setVlanIdPresent(true)
                .build())
            .build();

        // create match
        Match match = new MatchBuilder()
            .setVlanMatch(vlanMatch)
            .build();

        // create out put uri
        //Uri destPortUri = destPort.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();

        // create out put action
        Action outputToControllerAction = new ActionBuilder()
            .setOrder(1)//
            .setAction(new OutputActionCaseBuilder() //
                .setOutputAction(new OutputActionBuilder() //
                    .setMaxLength(new Integer(0xffff)) //
                    .setOutputNodeConnector(destPort) //
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
        fabricFlow
            .setMatch(match) //
            .setInstructions(new InstructionsBuilder() //
                .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                .build()) //
            .setPriority(this.priorityFabricSwapTagFlow) //
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(0) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return fabricFlow.build();
    }

    /**
     * create fabric flow in last node
     * @param tag
     * @param destPort
     * @return
     */
    private Flow createFabricLastFlow(long tag){
        // create a FlowBuilder object
        FlowBuilder fabricFlow = new FlowBuilder()
            .setTableId((short)FabricTable.SwapTagTable.getIntValue())
            .setFlowName("VLAN" + tag);

        // create flow id
        fabricFlow.setId(new FlowId(Long.toString(fabricFlow.hashCode())));

        // create vlan match
        VlanMatch vlanMatch = new VlanMatchBuilder()
            .setVlanId(new VlanIdBuilder()
                .setVlanId(new VlanId((int)tag))
                .setVlanIdPresent(true)
                .build())
            .build();

        // create match
        Match match = new MatchBuilder()
            .setVlanMatch(vlanMatch)
            .build();

        /*
         * Instruction 1: pop vlan
         */
        // create pop vlan action
        Action popVLANAction = new ActionBuilder()
            .setOrder(1)
            .setAction(new PopVlanActionCaseBuilder()
                .setPopVlanAction(new PopVlanActionBuilder()
                    .build())
                .build())
            .build();

        // create an apply action
        ApplyActions applyActions = new ApplyActionsBuilder()
            .setAction(ImmutableList.of(popVLANAction))
            .build();

        // Wrap our apply action in an instruction
        Instruction applyActionsInstruction = new InstructionBuilder()
            .setOrder(1)
            .setInstruction(new ApplyActionsCaseBuilder()//
                .setApplyActions(applyActions) //
                .build()) //
            .build();

        /*
         * Instruction 2: goto talbe:OutPutTable
         */
        // Wrap our apply action in an instruction
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
            .setPriority(this.priorityFabricSwapTagFlow) //
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(0) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return fabricFlow.build();
    }

    /**
     * create fabric flow in middle nodes
     * @param tag
     * @param destPort
     * @return
     */
    private Flow createFabricFlow(long tag, Uri destPort){
        // create a FlowBuilder object
        FlowBuilder fabricFlow = new FlowBuilder()
            .setTableId((short)FabricTable.InPutTable.getIntValue())
            .setFlowName("VLAN" + tag);

        // create flow id
        fabricFlow.setId(new FlowId(Long.toString(fabricFlow.hashCode())));

        // create vlan match
        VlanMatch vlanMatch = new VlanMatchBuilder()
            .setVlanId(new VlanIdBuilder()
                .setVlanId(new VlanId((int)tag))
                .setVlanIdPresent(true)
                .build())
            .build();

        // create match
        Match match = new MatchBuilder()
            .setVlanMatch(vlanMatch)
            .build();

        // create out put uri
        //Uri destPortUri = destPort.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();

        // create out put action
        Action outputToControllerAction = new ActionBuilder()
            .setOrder(1)//
            .setAction(new OutputActionCaseBuilder() //
                .setOutputAction(new OutputActionBuilder() //
                    .setMaxLength(new Integer(0xffff)) //
                    .setOutputNodeConnector(destPort) //
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
        fabricFlow
            .setMatch(match) //
            .setInstructions(new InstructionsBuilder() //
                .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                .build()) //
            .setPriority(this.priorityFabricSwapTagFlow) //
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(0) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return fabricFlow.build();
    }
    @Override
    public void addFabricLastFlow(NodeId nodeId,long tag) {
        NodeRef nr = this.createNodeRef(nodeId);
        FabricNodeFlows f = this.fabricNodeFlowsUtils.getFabricNodeFlowsByNodeId(nr);
        Flow flow = this.createFabricLastFlow(tag);
        //AddFlowInput input = new AddFlowInputBuilder(flow).setNode(nrf).build();
        f.addFabricSwapFlow(tag, flow);
        //this.createFlowWithCheck(input);
        //this.flowService.addFlow(input);
    }
    @Override
    public void addFabricMiddleFlow(NodeId nodeId,long tag,Uri destPort) {
        NodeRef nr = this.createNodeRef(nodeId);
        FabricNodeFlows f = this.fabricNodeFlowsUtils.getFabricNodeFlowsByNodeId(nr);
        Flow flow = this.createFabricFlow(tag, destPort);
        //AddFlowInput input = new AddFlowInputBuilder(flow).setNode(nrf).build();
        f.addFabricInputFlow(tag, flow);
        //this.createFlowWithCheck(input);
        //this.flowService.addFlow(input);
    }
    @Override
    public void addFabricFirstFlow(NodeId nodeId,long tag,Uri destPort) {
        NodeRef nr = this.createNodeRef(nodeId);
        FabricNodeFlows f = this.fabricNodeFlowsUtils.getFabricNodeFlowsByNodeId(nr);
        Flow flow = this.createFabricFirstFlow(tag, destPort);
        //AddFlowInput input = new AddFlowInputBuilder(flow).setNode(nrf).build();
        f.addFabricSwapFlow(tag, flow);
        //this.createFlowWithCheck(input);
        //this.flowService.addFlow(input);
    }
    @Override
    public void downloadAllFabricFlows() {
        // TODO Auto-generated method stub
        AddFlowInputBuilder afib = new AddFlowInputBuilder();
        Iterator<Entry<NodeRef,FabricNodeFlows>> iEntry= this.fabricNodeFlowsUtils.getAllFabricNodeFlows().entrySet().iterator();
        while(iEntry.hasNext()){
            Entry<NodeRef,FabricNodeFlows> entry = iEntry.next();
            NodeRef nr = entry.getKey();
            FabricNodeFlows fnf = entry.getValue();
            Iterator<Flow> iFlow = fnf.getFabricInputFlows().values().iterator();
            while(iFlow.hasNext()){
                afib.setNode(nr).fieldsFrom(iFlow.next());
                AddFlowInput input = afib.build();
                this.createFlowWithCheck(input);
            }
            iFlow = fnf.getFabricSwapFlows().values().iterator();
            while(iFlow.hasNext()){
                afib.setNode(nr).fieldsFrom(iFlow.next());
                AddFlowInput input = afib.build();
                this.createFlowWithCheck(input);
            }
        }
        return;
    }

    @Override
    public void downloadFabricFlowsByNodeId(NodeId nodeId,long sleepTime) {
        // TODO Auto-generated method stub
        AddFlowInputBuilder afib = new AddFlowInputBuilder();
        NodeRef nr = this.createNodeRef(nodeId);
        FabricNodeFlows fnf = this.fabricNodeFlowsUtils.getFabricNodeFlowsByNodeId(nr);
        Iterator<Flow> iFlow = fnf.getFabricInputFlows().values().iterator();
        while(iFlow.hasNext()){
            try{
                Thread.sleep(sleepTime);
            }catch(Exception ex){
            }
            afib.setNode(nr).fieldsFrom(iFlow.next());
            AddFlowInput input = afib.build();
            this.createFlowWithCheck(input);
        }
        iFlow = fnf.getFabricSwapFlows().values().iterator();
        while(iFlow.hasNext()){
            try{
                Thread.sleep(sleepTime);
            }catch(Exception ex){
            }
            afib.setNode(nr).fieldsFrom(iFlow.next());
            AddFlowInput input = afib.build();
            this.createFlowWithCheck(input);
        }
    }
    @Override
    public void deleteNodeFabricFlows(String nodeId, long tag) {
        // TODO Auto-generated method stub
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId tNodeId = new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(nodeId);
        NodeKey nodeKey = new NodeKey(tNodeId);
        // build InstanceIdentifier of node
        InstanceIdentifier<Node> nodeIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class,nodeKey).toInstance();
        NodeRef nr = new NodeRef( nodeIdentifier);
        // get fabric flows
        FabricNodeFlows fabricNodeFlows = this.fabricNodeFlowsUtils.getFabricNodeFlowsByNodeId(nr);
        // remove input fabric flow
        Flow inputFlow = fabricNodeFlows.removeFabricInputFlowByTag(tag);
        RemoveFlowInput input = new RemoveFlowInputBuilder(inputFlow).setNode(nr).build();
        this.flowService.removeFlow(input);
        // remove input fabric flow
        Flow swapFlow = fabricNodeFlows.removeFabricSwapFlowByTag(tag);
        RemoveFlowInput swap = new RemoveFlowInputBuilder(swapFlow).setNode(nr).build();
        this.flowService.removeFlow(swap);
        return;
    }
    @Override
    public void addTempFirstFlow(NodeId nodeId,long tag,Uri destPort) {
        NodeRef nr = this.createNodeRef(nodeId);
        Flow flow = this.createTempFirstFlow(tag, destPort);
        AddFlowInput input = new AddFlowInputBuilder(flow).setNode(nr).build();
        this.createFlowWithCheck(input);
    }

    @Override
    public void addTempMiddleFlow(NodeId nodeId, long tag, Uri destPort) {
        NodeRef nr = this.createNodeRef(nodeId);
        Flow flow = this.createTempFlow(tag, destPort);
        AddFlowInput input = new AddFlowInputBuilder(flow).setNode(nr).build();
        this.createFlowWithCheck(input);
    }
    /**
     * create temp flow in first node
     * @param tag
     * @param destPort
     * @return
     */
    private Flow createTempFirstFlow(long tag, Uri destPort){
        // create a FlowBuilder object
        FlowBuilder fabricFlow = new FlowBuilder()
            .setTableId((short)FabricTable.SwapTagTable.getIntValue())
            .setFlowName("VLAN" + tag);

        // create flow id
        fabricFlow.setId(new FlowId(Long.toString(fabricFlow.hashCode())));

        // create vlan match
        VlanMatch vlanMatch = new VlanMatchBuilder()
            .setVlanId(new VlanIdBuilder()
                .setVlanId(new VlanId((int)tag))
                .setVlanIdPresent(true)
                .build())
            .build();

        // create match
        Match match = new MatchBuilder()
            .setVlanMatch(vlanMatch)
            .build();

        // create out put uri
        //Uri destPortUri = destPort.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();

        // create out put action
        Action outputToControllerAction = new ActionBuilder()
            .setOrder(1)//
            .setAction(new OutputActionCaseBuilder() //
                .setOutputAction(new OutputActionBuilder() //
                    .setMaxLength(new Integer(0xffff)) //
                    .setOutputNodeConnector(destPort) //
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
        fabricFlow
            .setMatch(match) //
            .setInstructions(new InstructionsBuilder() //
                .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                .build()) //
            .setPriority(this.priorityTempSwapTagFlow) //
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(this.idleTimeoutTemp) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return fabricFlow.build();
    }
    /**
     * create fabric flow in middle nodes
     * @param tag
     * @param destPort
     * @return
     */
    private Flow createTempFlow(long tag, Uri destPort){
        // create a FlowBuilder object
        FlowBuilder fabricFlow = new FlowBuilder()
            .setTableId((short)FabricTable.InPutTable.getIntValue())
            .setFlowName("VLAN" + tag);

        // create flow id
        fabricFlow.setId(new FlowId(Long.toString(fabricFlow.hashCode())));

        // create vlan match
        VlanMatch vlanMatch = new VlanMatchBuilder()
            .setVlanId(new VlanIdBuilder()
                .setVlanId(new VlanId((int)tag))
                .setVlanIdPresent(true)
                .build())
            .build();

        // create match
        Match match = new MatchBuilder()
            .setVlanMatch(vlanMatch)
            .build();

        // create out put uri
        //Uri destPortUri = destPort.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();

        // create out put action
        Action outputToControllerAction = new ActionBuilder()
            .setOrder(1)//
            .setAction(new OutputActionCaseBuilder() //
                .setOutputAction(new OutputActionBuilder() //
                    .setMaxLength(new Integer(0xffff)) //
                    .setOutputNodeConnector(destPort) //
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
        fabricFlow
            .setMatch(match) //
            .setInstructions(new InstructionsBuilder() //
                .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                .build()) //
            .setPriority(this.priorityTempSwapTagFlow) //
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(this.idleTimeoutTemp) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return fabricFlow.build();
    }
    /*
     * utils
     */
    /**
     * save the node-connectors maps
     * @param node
     */
//    private void saveFabricNode(FabricNode node){
//        DataModificationTransaction it = this.dataBroker.beginTransaction();
//        InstanceIdentifier<FabricNode> path = InstanceIdentifier.builder(FabricNodes.class)
//                    .child(FabricNode.class,node.getKey()).toInstance();
//        it.putOperationalData(path, node);
//        it.commit();
//        return;
//    }

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
    /**
     * create a nodeRef by NodeId
     * @param nodeIdnet
     * @return
     */
    private NodeRef createNodeRef(NodeId nodeIdnet){
        // build NodeKey
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId nodeId = new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(nodeIdnet.getValue());
        NodeKey nodeKey = new NodeKey(nodeId);

        // build InstanceIdentifier of node
        InstanceIdentifier<Node> nodeIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class,nodeKey).toInstance();
        NodeRef nrf = new NodeRef( nodeIdentifier);
        return nrf;
    }

    private boolean createFlow(AddFlowInput input){
        Future<RpcResult<AddFlowOutput>> ret = this.flowService.addFlow(input);
        try{
            RpcResult<AddFlowOutput> rpcRet = ret.get(100,TimeUnit.MILLISECONDS);
            return rpcRet.isSuccessful();
        }catch(Exception ex){
        }
        return false;
    }

    private void createFlowWithCheck(AddFlowInput input){
        int times = 100;
        while((!this.createFlow(input))&& times > 0){
            times--;
        }
        return;
    }
}
