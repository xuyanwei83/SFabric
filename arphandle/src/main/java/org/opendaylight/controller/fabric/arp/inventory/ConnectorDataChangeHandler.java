/*
q * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.fabric.arp.inventory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Listens to data change events on topology links
 * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
 * and maintains a topology graph using provided NetworkGraphService
 * {@link org.opendaylight.controller.sample.l2switch.md.topology.NetworkGraphService}.
 * It refreshes the graph after a delay(default 10 sec) to accommodate burst of change events if they come in bulk.
 * This is to avoid continuous refresh of graph on a series of change events in short time.
 */
public class ConnectorDataChangeHandler implements DataChangeListener {
    private static final String DEFAULT_TOPOLOGY_ID = "flow:1";

    private boolean refreshScheduled = false;
    private final ScheduledExecutorService refreshScheduler = Executors.newScheduledThreadPool(1);
    private final long DEFAULT_GRAPH_REFRESH_DELAY = 10;
    private final long refreshDelayInSec;

    private final DataBroker dataService;
    private final ConnectorService connectorService;

    /**
     *
     * @param dataService
     * @param connectorService
     */
    public ConnectorDataChangeHandler(DataBroker dataService,
            ConnectorService connectorService){
        this.dataService = dataService;
        this.connectorService = connectorService;
        this.refreshDelayInSec = DEFAULT_GRAPH_REFRESH_DELAY;
    }

    /**
     *
     * @param dataService
     * @param connectorService
     * @param refreshDelayInSec
     */
    public ConnectorDataChangeHandler(DataBroker dataService,
            ConnectorService connectorService,
            long refreshDelayInSec) {
        this.dataService = dataService;
        this.connectorService = connectorService;
        this.refreshDelayInSec = refreshDelayInSec;
    }

    /**
    * Based on if links have been added or removed in topology data store, schedules a refresh of network graph.
    * @param dataChangeEvent
    */

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        // TODO Auto-generated method stub
        Map<InstanceIdentifier<?>, DataObject> originalData = change.getCreatedData(); //change.getOriginalOperationalData();
        Map<InstanceIdentifier<?>, DataObject> updatedData = change.getUpdatedData();//change.getUpdatedOperationalData();
        // change this logic, once MD-SAL start populating DeletedOperationData Set
        if(originalData != null && updatedData != null
            && (originalData.size() != 0 || updatedData.size() != 0)
            && !refreshScheduled) {
            refreshScheduled = originalData.size() != updatedData.size();
            if(refreshScheduled) {
                refreshScheduler.schedule(new ConnectorRefresher(), refreshDelayInSec, TimeUnit.SECONDS);
            }
        }
    }
//    @Override
//    public void onDataChanged(DataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent) {
//        Map<InstanceIdentifier<?>, DataObject> originalData = change.getOriginalOperationalData();
//        Map<InstanceIdentifier<?>, DataObject> updatedData = change.getUpdatedOperationalData();
//        // change this logic, once MD-SAL start populating DeletedOperationData Set
//        if(originalData != null && updatedData != null
//            && (originalData.size() != 0 || updatedData.size() != 0)
//            && !refreshScheduled) {
//            refreshScheduled = originalData.size() != updatedData.size();
//            if(refreshScheduled) {
//                refreshScheduler.schedule(new ConnectorRefresher(), refreshDelayInSec, TimeUnit.SECONDS);
//            }
//        }
//    }

    /**
    * Registers as a data listener to receive changes done to
    * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
    * under {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology}
    * operation data root.
    */
    public void registerAsDataChangeListener() {
        this.connectorService.update();
        //Link change
        InstanceIdentifier<Link> linkInstance = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(DEFAULT_TOPOLOGY_ID))).child(Link.class).toInstance();
        this.dataService.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,linkInstance,this,DataChangeScope.ONE);//.registerDataChangeListener(linkInstance, this);
//        //Node change
//        InstanceIdentifier<Node> nodeInstance = InstanceIdentifier.builder(NetworkTopology.class)
//                .child(Topology.class, new TopologyKey(new TopologyId(DEFAULT_TOPOLOGY_ID))).child(Node.class).toInstance();
//        this.dataService.registerDataChangeListener(nodeInstance, this);
    }

    private class ConnectorRefresher implements Runnable {
        @Override
        public void run() {
            refreshScheduled = false;
            //TODO: it should refer to changed links only from DataChangeEvent above.
            connectorService.clear();
            connectorService.update();
        }

    }
}
