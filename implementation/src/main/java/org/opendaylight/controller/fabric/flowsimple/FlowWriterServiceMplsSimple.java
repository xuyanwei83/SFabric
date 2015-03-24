package org.opendaylight.controller.fabric.flowsimple;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.fabric.flow.FlowWriterServiceImpl;
import org.opendaylight.controller.fabric.topology.NetworkGraphService;
import org.opendaylight.controller.fabric.util.InstanceIdentifierUtils;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.enumeration.rev140402.FabricProtocal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.enumeration.rev140402.FabricTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.model.rev140402.fabric.data.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.model.rev140402.fabric.datas.FabricData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.model.rev140402.fabric.datas.FabricDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFieldsBuilder;
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

import com.google.common.collect.ImmutableList;

public class FlowWriterServiceMplsSimple implements FlowWriterServiceSimple{
    private static final Logger _logger = LoggerFactory.getLogger(FlowWriterServiceImpl.class);
    private final DataBrokerService dataBrokerService;
    private final NetworkGraphService networkGraphService;
    private AtomicLong flowIdInc = new AtomicLong();
    private AtomicLong flowCookieInc = new AtomicLong(0x2a00000000000000L);
    private final String DEFAULT_TOPOLOGY_ID = "flow:1";
    private final int priority = 0;

    ///////////////////////////////////////////////
    //Construction function
    ///////////////////////////////////////////////
    /**
     * Construction function
     * @param dataBrokerService
     * @param networkGraphService
     */
    public FlowWriterServiceMplsSimple(DataBrokerService dataBrokerService,
            NetworkGraphService networkGraphService){
        this.dataBrokerService = dataBrokerService;
        this.networkGraphService = networkGraphService;
        return;
    }

    ///////////////////////////////////////////////
    //Override functions: implement interface
    ///////////////////////////////////////////////
    /**
     * AddNodeToNodeFabricFlows:
     * 1.Calculate and setup the fabric path(install flows)
     * 2.Return the fabric path
     *    /restconf/operational/fabric-model:fabrics/fabric-data/{tag}/
     * @param srcNodeId: Source Node ID
     * @param dstNodeId: Destination Node ID
     * @param tag: Fabric tag, e.g. mpls / vlan ...
     */
    @Override
    public FabricData addNodeToNodeFabricFlows(NodeId srcNodeId,NodeId dstNodeId,long tag) {
        // TODO Auto-generated method stub
        //Get Path
        List<Link> linksInBeween = networkGraphService.getPath(srcNodeId, dstNodeId);
        if(linksInBeween == null || linksInBeween.isEmpty()){
            return null;
        }
        //Add links to fabric data store
        FabricDataBuilder ret = new FabricDataBuilder();
        ret.setTag(tag);
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.model.rev140402.fabric.data.Link> list = new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.model.rev140402.fabric.data.Link>();
        LinkBuilder linkBuilder;
        boolean skipFirst = false;
        // assumes the list order is maintained and starts with link that has source as source node
        for(Link link : linksInBeween) {
            linkBuilder = new LinkBuilder(link);
            list.add(linkBuilder.build());
            if(skipFirst){
                this.AddFabricFlow(tag, this.GetSourceNodeConnectorRef(link));
            }else{
                skipFirst = true;
            }
        }
        ret.setLink(list);
        return ret.build();
    }

    /**
     * ClearAllFabricFlows
     * Clear all FabricFlows in the nodes below
     * @param nodeIdCollection: the Node which need to delete its fabric flow
     */
    @Override
    public void clearAllFabricFlows(Collection<NodeId> nodeIdCollection) {
        // TODO Auto-generated method stub
        //Check parameters
        if(nodeIdCollection == null || nodeIdCollection.isEmpty()){
            return;
        }
        //Read node from topology
        //if can not read the node from topology, return;
        InstanceIdentifier<Topology> topologyPath = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(DEFAULT_TOPOLOGY_ID))).toInstance();
        Topology completeTopology =(Topology)this.dataBrokerService.readOperationalData(topologyPath);
        if(completeTopology == null){
            return;
        }
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> listNode = completeTopology.getNode();
        if(listNode == null){
            return;
        }
        for(NodeId id:nodeIdCollection){
            //If the node is not removed, remove its flows from data store.
            if(CheckNodeIdInNodeList(listNode,id)){
                this.RemoveFlowsByNodeId(id);
            }else{
                //If the node is removed, remove node from data store.
                this.RemoveNodeByNodeId(id);
            }
        }
    }
    ///////////////////////////////////////////////
    //Private functions:
    //Business logic
    ///////////////////////////////////////////////
    /**
     * AddFabricFlow
     * Install a flow to switch
     * @param tag
     * @param destNodeConnectorRef
     */
    private void AddFabricFlow(long tag, NodeConnectorRef destNodeConnectorRef) {
        // get flow table key
        TableKey flowTableKey = new TableKey((short)FabricTable.InPutTable.getIntValue());

        //build a flow path based on node connector to program flow
        InstanceIdentifier<Flow> flowPath = this.BuildFlowPath(destNodeConnectorRef, flowTableKey);

        // build a flow that target given arguments
        Flow flowBody = this.CreateFabricFlow(flowTableKey.getId(), this.priority, tag, destNodeConnectorRef);

        // commit the flow in ConfigData
        this.WriteFlowToConfigData(flowPath, flowBody);
    }

    /**
     * CreateFabricFlow
     * Create a flow of fabric by the parameters below
     * @param tableId: where the table install
     * @param priority: the flow's priority
     * @param tag: the tag value of the flow to switch
     * @param destPort: output port
     * @return
     */
    private Flow CreateFabricFlow(Short tableId, int priority, long tag, NodeConnectorRef destPort){
        // start building flow
        FlowBuilder fabricFlow = new FlowBuilder()
                                 .setTableId(tableId)
                                 .setFlowName("MPLS" + tag);

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

        // create a match that has mpls match
        ProtocolMatchFieldsBuilder protocolMatchFieldsBuilder = new ProtocolMatchFieldsBuilder()
            .setMplsLabel(tag);
        ProtocolMatchFields protocolMatch = protocolMatchFieldsBuilder.build();
        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder();
        EthernetMatch ethernetMatch = ethernetMatchBuilder
                .setEthernetType(new EthernetTypeBuilder()
                    .setType(new EtherType((long)FabricProtocal.MPLS.getIntValue()))
                    .build())
                .build();

        Match match = new MatchBuilder()
            .setProtocolMatchFields(protocolMatch)
            .setEthernetMatch(ethernetMatch)
            .build();

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
    /**
     * RemoveFlowsByNodeId
     * Remove the flows of the node
     * @param id
     */
    private void RemoveFlowsByNodeId(NodeId id){
        InstanceIdentifier<Node> nodePath = InstanceIdentifierUtils.createNodePath(new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(id.getValue()));
        InstanceIdentifier<Table> tableInputPath = InstanceIdentifierUtils.createTablePath(nodePath, new TableKey((short)FabricTable.InPutTable.getIntValue()));
        DataModificationTransaction it = this.dataBrokerService.beginTransaction();
        it.removeConfigurationData(tableInputPath);
        it.commit();
    }
    /**
     * RemoveNodeByNodeId
     * Remove the node from the data store
     * @param id
     */
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
    ///////////////////////////////////////////////
    //Utils functions
    ///////////////////////////////////////////////
    /**
     * WriteFlowToConfigData
     * Write the flow to the data store,another module will install it
     * @param flowPath
     * @param flowBody
     * @return
     */
    private Future<RpcResult<TransactionStatus>> WriteFlowToConfigData(InstanceIdentifier<Flow> flowPath,Flow flowBody) {
        DataModificationTransaction it = dataBrokerService.beginTransaction();
        it.putConfigurationData(flowPath, flowBody);
        return it.commit();
    }

    /**
     * BuildFlowPath
     * Get the Flow's path of data store, where it save.
     * @param nodeConnectorRef
     * @param flowTableKey
     * @return
     */
    private InstanceIdentifier<Flow> BuildFlowPath(NodeConnectorRef nodeConnectorRef, TableKey flowTableKey) {
        // generate unique flow key
        FlowId flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
        FlowKey flowKey = new FlowKey(flowId);
        return InstanceIdentifierUtils.generateFlowInstanceIdentifier(nodeConnectorRef, flowTableKey, flowKey);
    }

    /**
     * GetSourceNodeConnectorRef
     * Get the NodeConnectorRef of the link source
     * @param link
     * @return NodeConnectorRef
     */
    private NodeConnectorRef GetSourceNodeConnectorRef(Link link) {
        InstanceIdentifier<NodeConnector> nodeConnectorInstanceIdentifier
            = InstanceIdentifierUtils.createNodeConnectorIdentifier(
            link.getSource().getSourceNode().getValue(),
            link.getSource().getSourceTp().getValue());
        return new NodeConnectorRef(nodeConnectorInstanceIdentifier);
    }

    /**
     * CheckNodeIdInNodeList
     * Check the nodeId in the topology list
     * @param list
     * @param id
     * @return true:in/false:out
     */
    private boolean CheckNodeIdInNodeList(List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> list,NodeId id){
        for(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node node : list){
            if(node.getNodeId().equals(id)){
                return true;
            }
        }
        return false;
    }
}
