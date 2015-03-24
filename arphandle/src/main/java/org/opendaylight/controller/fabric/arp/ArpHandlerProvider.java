/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.fabric.arp;

import org.opendaylight.controller.fabric.arp.flow.FlowWriterService;
import org.opendaylight.controller.fabric.arp.flow.FlowWriterServiceVlanMix;
import org.opendaylight.controller.fabric.arp.inventory.ConnectorDataChangeHandler;
import org.opendaylight.controller.fabric.arp.inventory.ConnectorService;
import org.opendaylight.controller.fabric.arp.inventory.HostService;
import org.opendaylight.controller.fabric.arp.packet.PacketHandlerLastNodeTag;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Arp serves as the Activator for fabric OSGI bundle.
 */
public class ArpHandlerProvider extends AbstractBindingAwareConsumer
                              implements AutoCloseable {
    private final Logger _logger = LoggerFactory.getLogger(ArpHandlerProvider.class);
    private ListenerRegistration<NotificationListener> listenerRegistration;
    private ConnectorDataChangeHandler connectorDataChangeHandler;
    private HostService hostService;

    /**
     * Setup the L2Switch.
     * @param consumerContext  The context of the L2Switch.
     */
    @Override
    public void onSessionInitialized(ConsumerContext consumerContext) {
        // TODO Auto-generated method stub
        // get the md service from provider session
        SalFlowService flowService = consumerContext.getRpcService(SalFlowService.class);
        DataBroker dataService = (DataBroker)consumerContext.getSALService(DataBroker.class);

        // create flow writer
        FlowWriterService flowWriterService= new FlowWriterServiceVlanMix(flowService);

        // create ConnectorService
        ConnectorService connectorService = new ConnectorService(dataService);

        // get notification service
        NotificationService notificationService = (NotificationService)consumerContext.getSALService(NotificationService.class);

        // get packet processing service
        PacketProcessingService packetProcessingService = (PacketProcessingService)consumerContext.getRpcService(PacketProcessingService.class);

        this.hostService = new HostService();
        // create packetHandleer
//        PacketHandler packetHandler = new PacketHandler(dataService,
//                packetProcessingService,
//                flowWriterService,
//                connectorService,
//                this.hostService);
        PacketHandlerLastNodeTag packetHandler = new PacketHandlerLastNodeTag(dataService,
                  packetProcessingService,
                  flowWriterService,
                  connectorService,
                  this.hostService);
        // regist the packet in information
        this.listenerRegistration = notificationService.registerNotificationListener(packetHandler);

        // packetHandler
        packetHandler.start();
        // regist the connector listener
        this.connectorDataChangeHandler = new ConnectorDataChangeHandler(dataService,connectorService);
        //this.connectorDataChangeHandler.registerAsDataChangeListener();
        this._logger.info("Fabric ARP start success!");
    }

    /**
     * Cleanup the
     * @throws Exception  occurs when the NotificationListener is closed
     */
    @Override
    public void close() throws Exception {
        if (listenerRegistration != null){
            listenerRegistration.close();
        }
        this._logger.info("Fabric ARP listenerRegistration closed!");
    }

    @Override
    protected void stopImpl(BundleContext context) {
        // NOOP
        this.hostService.clear();
        _logger.info("Fabric ARP stop success!");
    }
}
