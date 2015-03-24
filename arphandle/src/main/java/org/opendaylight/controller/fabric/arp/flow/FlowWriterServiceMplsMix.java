/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.fabric.arp.flow;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.fabric.arp.util.InstanceIdentifierUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.mpls.action._case.PushMplsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.enumeration.rev140402.FabricTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFieldsBuilder;

import com.google.common.collect.ImmutableList;

/**
 * Implementation of FlowWriterService{@link org.opendaylight.controller.arphandler_new.md.flow.FlowWriterService},
 * that builds required flow and writes to configuration data store using provided DataBrokerService
 * {@link org.opendaylight.controller.sal.binding.api.data.DataBrokerService}
 */
public class FlowWriterServiceMplsMix implements FlowWriterService {
    private final SalFlowService flowService;
    private AtomicLong flowCookieInc = new AtomicLong(0x2a00000000000000L);

    private final int fabricIdleTimeOut = 100;
    private final int hostNodeIdleTimeOut = 0;
    private final int sameNodeFlowPriority = 10;
    private final int hostNodeFlowPriority = 10;

    public FlowWriterServiceMplsMix(SalFlowService flowService) {
        this.flowService = flowService;
    }

    @Override
    public void addMacToMacFlowsSameNode(MacAddress srcMac, NodeConnectorRef srcNodeConnectorRef, MacAddress dstMac, NodeConnectorRef dstNodeConnectorRef){
        // build a flow that target given mac id
        Flow flowBody = this.createMacToMacFlow((short)FabricTable.InPutTable.getIntValue(),
                this.sameNodeFlowPriority,
                this.fabricIdleTimeOut,
                srcMac,
                dstMac,
                dstNodeConnectorRef);
        // commit the flow in config data
        NodeRef nrf = new NodeRef( InstanceIdentifierUtils.generateNodeInstanceIdentifier(dstNodeConnectorRef));
        AddFlowInput input = new AddFlowInputBuilder(flowBody).setNode(nrf).build();
        this.flowService.addFlow(input);
        return;
    }
    @Override
    public void addMacToMacFlowsHostPath(MacAddress dstMac,NodeConnectorRef dstNodeConnectorRef) {
        // build a flow that target given mac id without srcMac
        Flow flowBodyDest = this.createMacToMacFlow((short) FabricTable.OutPutTable.getIntValue(),
                this.hostNodeFlowPriority,
                this.hostNodeIdleTimeOut,
                null,
                dstMac,
                dstNodeConnectorRef);

        // commit the flow in config data
        NodeRef nrf = new NodeRef( InstanceIdentifierUtils.generateNodeInstanceIdentifier(dstNodeConnectorRef));
        AddFlowInput input = new AddFlowInputBuilder(flowBodyDest).setNode(nrf).build();
        this.flowService.addFlow(input);
        return;
    }

    @Override
    public void addMacToMacFlowsFabricPath(MacAddress srcMac,
            NodeConnectorRef srcNodeConnectorRef,
            MacAddress dstMac,
            NodeConnectorRef dstNodeConnectorRef,
            long tag) {

        Flow flowBodySrc = this.createFabricFlowPushMpls(
                (short) FabricTable.PushTagTable.getIntValue(),
                10,
                srcMac,
                dstMac,
                tag,
                (short) FabricTable.SwapTagTable.getIntValue());
        // commit the flow in config data
        NodeRef nrf = new NodeRef( InstanceIdentifierUtils.generateNodeInstanceIdentifier(srcNodeConnectorRef));
        AddFlowInput input = new AddFlowInputBuilder(flowBodySrc).setNode(nrf).build();
        this.flowService.addFlow(input);
        return;
    }

    /**
     * create fabric push mpls flow
     * @param tableId
     * @param priority
     * @param sourceMac
     * @param destMac
     * @param tag
     * @param gotoTableId
     * @return
     */
    private Flow createFabricFlowPushMpls(short tableId, int priority, MacAddress sourceMac,
        MacAddress destMac, long tag, short gotoTableId) {
        // create fabric flow
        FlowBuilder fabricFlow = new FlowBuilder() //
          .setTableId(tableId) //
          .setFlowName("Fabric Push Mpls:"+tag);

        // use its own hash code for id.
        fabricFlow.setId(new FlowId(Long.toString(fabricFlow.hashCode())));

        // create a match that has mac to mac ethernet match
        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder() //
            .setEthernetDestination(new EthernetDestinationBuilder() //
              .setAddress(destMac) //
              .build());

        EthernetMatch ethernetMatch = ethernetMatchBuilder
              .setEthernetType(new EthernetTypeBuilder()
                  .setType(new EtherType(0x0800L))
                  .build())
              .build();
        Match match = new MatchBuilder()
              .setEthernetMatch(ethernetMatch)
              .build();

        // create action 1: push mpls
        Action mplsPsuhAction = new ActionBuilder()
            .setOrder(1)
            .setAction(new PushMplsActionCaseBuilder()
                .setPushMplsAction(new PushMplsActionBuilder()
                    .setEthernetType(new Integer(0x8847))
                    .build())
                .build())
            .build();
        // create action 2: set label value
        ProtocolMatchFields protocolMatch = new ProtocolMatchFieldsBuilder()
            .setMplsLabel(tag)
            .build();

        Action setFieldAction = new ActionBuilder()
            .setOrder(2)
            .setAction(new SetFieldCaseBuilder()
                .setSetField(new SetFieldBuilder()
                    .setProtocolMatchFields(protocolMatch)
                    .build())
                .build())
            .build();

        // create push mpls actions
        ApplyActions applyActions = new ApplyActionsBuilder()
            .setAction(ImmutableList.of(mplsPsuhAction,setFieldAction))
            .build();

        // instruction 1: push mpls actions
        Instruction applyActionsInstruction = new InstructionBuilder() //
            .setOrder(1)
            .setInstruction(new ApplyActionsCaseBuilder()//
                .setApplyActions(applyActions) //
                .build())
            .build();

        // instruction 2: goto table
        // create goto table
        GoToTable gotoTable = new GoToTableBuilder()
            .setTableId(gotoTableId)
            .build();
        // create instruction set goto table
        Instruction goToTableInstruction = new InstructionBuilder() //
            .setOrder(2)
            .setInstruction(new GoToTableCaseBuilder()
                .setGoToTable(gotoTable)
                .build())
            .build();

        // put our Instruction in a list of Instructions
        fabricFlow
            .setMatch(match)
            .setInstructions(new InstructionsBuilder()
                .setInstruction(ImmutableList.of(applyActionsInstruction,goToTableInstruction)) //
                .build())
            .setPriority(priority)
            .setBufferId(0L)
            .setHardTimeout(0)
            .setIdleTimeout(this.fabricIdleTimeOut)
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return fabricFlow.build();
    }
    /**
     * create mac to mac flow body
     * @param tableId
     * @param priority
     * @param idleTimeOut
     * @param sourceMac
     * @param destMac
     * @param destPort
     * @return
     */
    private Flow createMacToMacFlow(short tableId, int priority, int idleTimeOut,
              MacAddress sourceMac, MacAddress destMac, NodeConnectorRef destPort) {

        // start building flow
        FlowBuilder macToMacFlow = new FlowBuilder() //
            .setTableId(tableId) //
            .setFlowName("MacToMac");

        // use its own hash code for id.
        macToMacFlow.setId(new FlowId(Long.toString(macToMacFlow.hashCode())));

        // create a match that has mac to mac ethernet match
        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder() //
            .setEthernetDestination(new EthernetDestinationBuilder() //
            .setAddress(destMac) //
            .build());

        // set source in the match only if present
        if(sourceMac != null) {
            ethernetMatchBuilder.setEthernetSource(new EthernetSourceBuilder()
                .setAddress(sourceMac)
                .build());
        }

        EthernetMatch ethernetMatch = ethernetMatchBuilder.build();
        Match match = new MatchBuilder()
            .setEthernetMatch(ethernetMatch)
            .build();

        // set output port
        Uri destPortUri = destPort.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();

        Action outputToControllerAction = new ActionBuilder() //
            .setOrder(1)
            .setAction(new OutputActionCaseBuilder() //
                .setOutputAction(new OutputActionBuilder() //
                .setMaxLength(new Integer(0xffff)) //
                    .setOutputNodeConnector(destPortUri) //
                    .build()) //
                .build()) //
            .build();

        // Create an Apply Action
        ApplyActions applyActions = new ApplyActionsBuilder()
            .setAction(ImmutableList.of(outputToControllerAction))
            .build();

        // Wrap our Apply Action in an Instruction
        Instruction applyActionsInstruction = new InstructionBuilder() //
            .setOrder(0)
            .setInstruction(new ApplyActionsCaseBuilder()//
                .setApplyActions(applyActions) //
                .build()) //
            .build();

        // Put our Instruction in a list of Instructions
        macToMacFlow
            .setMatch(match) //
            .setInstructions(new InstructionsBuilder() //
                .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                .build()) //
            .setPriority(priority) //
            .setBufferId(0L) //
            .setHardTimeout(0) //
            .setIdleTimeout(idleTimeOut) //
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        return macToMacFlow.build();
    }
}
