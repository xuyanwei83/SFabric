/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.fabric.topology;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.fabric.FabricService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Listens to data change events on topology links
 * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
 * and maintains a topology graph using provided NetworkGraphService
 * {@link org.opendaylight.controller.sample.l2switch.md.topology.NetworkGraphService}.
 * It refreshes the graph after a delay(default 10 sec) to accommodate burst of change events if they come in bulk.
 * This is to avoid continuous refresh of graph on a series of change events in short time.
 */
public class TopologyLinkDataChangeHandler implements DataChangeListener {
    private static final Logger _logger = LoggerFactory.getLogger(TopologyLinkDataChangeHandler.class);
    private static final String DEFAULT_TOPOLOGY_ID = "flow:1";

    private boolean refreshScheduled = false;
    private final ScheduledExecutorService refreshScheduler = Executors.newScheduledThreadPool(1);
    private final long DEFAULT_GRAPH_REFRESH_DELAY = 10;
    private final long refreshDelayInSec;

    private final NetworkGraphService networkGraphService;
    private final DataBroker dataBroker;
    private final FabricService fabricService;
    private ListenerRegistration<DataChangeListener> linksListenerRegistration;

    /**
    * Uses default delay to refresh topology graph if this constructor is used.
    * @param dataBroker
    * @param networkGraphService
    */
    public TopologyLinkDataChangeHandler(DataBroker dataBroker,
              NetworkGraphService networkGraphService,
              FabricService fabricService) {
        Preconditions.checkNotNull(dataBroker, "dataBroker should not be null.");
        Preconditions.checkNotNull(networkGraphService, "networkGraphService should not be null.");
        this.dataBroker = dataBroker;
        this.networkGraphService = networkGraphService;
        this.fabricService = fabricService;
        this.refreshDelayInSec = DEFAULT_GRAPH_REFRESH_DELAY;
    }

    /**
    *
    * @param dataBroker
    * @param networkGraphService
    * @param graphRefreshDelayInSec
    */
    public TopologyLinkDataChangeHandler(DataBroker dataBroker,
              NetworkGraphService networkGraphService,
              FabricService fabricService,
              long refreshDelayInSec) {
        Preconditions.checkNotNull(dataBroker, "dataBroker should not be null.");
        Preconditions.checkNotNull(networkGraphService, "networkGraphService should not be null.");
        this.dataBroker = dataBroker;
        this.networkGraphService = networkGraphService;
        this.fabricService = fabricService;
        this.refreshDelayInSec = refreshDelayInSec;
    }

    /**
    * Based on if links have been added or removed in topology data store, schedules a refresh of network graph.
    * @param dataChangeEvent
    */
    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent) {
        // TODO Auto-generated method stub
        if(dataChangeEvent == null) {
            return;
        }
        Map<InstanceIdentifier<?>, DataObject> createdData = dataChangeEvent.getCreatedData();
        Set<InstanceIdentifier<?>> removedPaths = dataChangeEvent.getRemovedPaths();
        Map<InstanceIdentifier<?>, DataObject> originalData = dataChangeEvent.getOriginalData();
        boolean isGraphUpdated = false;

        if(createdData != null && !createdData.isEmpty()) {
          Set<InstanceIdentifier<?>> linksIds = createdData.keySet();
          for(InstanceIdentifier<?> linkId : linksIds) {
            if(Link.class.isAssignableFrom(linkId.getTargetType())) {
              Link link = (Link) createdData.get(linkId);
              if(!(link.getLinkId().getValue().contains("host"))) {
                isGraphUpdated = true;
                _logger.debug("Graph is updated! Added Link {}", link.getLinkId().getValue());
                break;
              }
            }
          }
        }

        if(removedPaths != null && !removedPaths.isEmpty() && originalData != null && !originalData.isEmpty()) {
          for(InstanceIdentifier<?> instanceId : removedPaths) {
            if(Link.class.isAssignableFrom(instanceId.getTargetType())) {
              Link link = (Link) originalData.get(instanceId);
              if(!(link.getLinkId().getValue().contains("host"))) {
                isGraphUpdated = true;
                _logger.debug("Graph is updated! Removed Link {}", link.getLinkId().getValue());
                break;
              }
            }
          }
        }

        if(!isGraphUpdated) {
          return;
        }
        if(!refreshScheduled) {
            synchronized(this) {
              if(!refreshScheduled) {
                  refreshScheduler.schedule(new FabricRefresher(), DEFAULT_GRAPH_REFRESH_DELAY, TimeUnit.SECONDS);
                refreshScheduled = true;
                _logger.debug("Scheduled Graph for refresh.");
              }
            }
        } else {
            _logger.debug("Already scheduled for network graph refresh.");
        }
    }

    /**
    * Registers as a data listener to receive changes done to
    * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
    * under {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology}
    * operation data root.
    */
    public void registerAsDataChangeListener() {
        InstanceIdentifier<Topology> topologyPath = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(DEFAULT_TOPOLOGY_ID))).toInstance();
        //Topology completeTopology =(Topology)dataBroker.readOperationalData(topologyPath);
        ReadOnlyTransaction it = dataBroker.newReadOnlyTransaction();
        Topology completeTopology = null;
        try{
        Optional<Topology> data = it.read(LogicalDatastoreType.OPERATIONAL,topologyPath).get();

        if(data.isPresent()) {
               // data are present in data store.
                completeTopology = data.get();
            } else {
            }
        }catch(Exception ex){
        }
        this.networkGraphService.updateTopology(completeTopology);
        if(completeTopology != null){
            this.networkGraphService.addLinks(completeTopology.getLink());
        }
        //this.fabricService.SetupFabric();

        //Link change
        InstanceIdentifier<Link> linkInstance = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(DEFAULT_TOPOLOGY_ID))).child(Link.class).toInstance();
        this.linksListenerRegistration = this.dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,linkInstance,this,DataChangeScope.BASE);
    }
    public void close(){
        if(this.linksListenerRegistration != null){
            this.linksListenerRegistration.close();
        }
        return;
    }

    private class FabricRefresher implements Runnable {
        @Override
        public void run() {
            refreshScheduled = false;
            //TODO: it should refer to changed links only from DataChangeEvent above.
            Topology topology = this.getTopology(DEFAULT_TOPOLOGY_ID);
            networkGraphService.clear();// can remove this once changed links are addressed
            networkGraphService.updateTopology(topology);
            if(topology != null ){
                List<Link> links = topology.getLink();
                if(links != null && !links.isEmpty()) {
                    networkGraphService.addLinks(links);
                }
            }
        }
        /**
         * @param topologyId
         * @return
         */
        private Topology getTopology(String topologyId) {
            InstanceIdentifier<Topology> topologyPath = InstanceIdentifier.builder(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId(DEFAULT_TOPOLOGY_ID))).toInstance();
            //return (Topology)dataBroker.readOperationalData(topologyPath);
            ReadOnlyTransaction it = dataBroker.newReadOnlyTransaction();
            Topology completeTopology = null;
            try{
                Optional<Topology> data = it.read(LogicalDatastoreType.OPERATIONAL,topologyPath).get();
                if(data.isPresent()) {
                    // data are present in data store.
                     completeTopology = data.get();
                }
            }catch(Exception ex){
            }
            return completeTopology;
        }
    }
}
