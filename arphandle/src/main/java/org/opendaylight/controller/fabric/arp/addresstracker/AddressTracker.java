/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.fabric.arp.addresstracker;

import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.address.tracker.rev140402.FabricAddresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.address.tracker.rev140402.fabric.addresses.FabricAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.address.tracker.rev140402.fabric.addresses.FabricAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.address.tracker.rev140402.fabric.addresses.FabricAddressKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * AddressTracker manages the MD-SAL data tree for FabricAddress (mac, node connector pairings) information.
 */
public class AddressTracker {

//  private final static Logger _logger = LoggerFactory.getLogger(AddressTracker.class);
  private DataBrokerService dataService;

  /**
   * Construct an AddressTracker with the specified inputs
   * @param dataService  The DataBrokerService for the AddressTracker
   */
  public AddressTracker(DataBrokerService dataService) {
    this.dataService = dataService;
  }

  /**
   * Get all the Fabric Addresses in the MD-SAL data tree
   * @return    All the Fabric Addresses in the MD-SAL data tree
   */
  public FabricAddresses getAddresses() {
    return (FabricAddresses)dataService.readOperationalData(InstanceIdentifier.<FabricAddresses>builder(FabricAddresses.class).toInstance());
  }

  /**
   * Get a specific Fabric Address in the MD-SAL data tree
   * @param macAddress  A MacAddress associated with an Fabric Address object
   * @return    The Fabric Address corresponding to the specified macAddress
   */
  public FabricAddress getAddress(MacAddress macAddress) {
    return (FabricAddress) dataService.readOperationalData(createPath(macAddress));
  }

  /**
   * Add Fabric Address into the MD-SAL data tree
   * @param macAddress  The MacAddress of the new FabricAddress object
   * @param nodeConnectorRef  The NodeConnectorRef of the new FabricAddress object
   * @return  Future containing the result of the add operation
   */
  public Future<RpcResult<TransactionStatus>> addAddress(MacAddress macAddress, NodeConnectorRef nodeConnectorRef) {
    if(macAddress == null || nodeConnectorRef == null) {
      return null;
    }

    // Create FabricAddress
    final FabricAddressBuilder builder = new FabricAddressBuilder();
    builder.setKey(new FabricAddressKey(macAddress))
            .setMac(macAddress)
            .setNodeConnectorRef(nodeConnectorRef);

    // Add FabricAddress to MD-SAL data tree
    final DataModificationTransaction it = dataService.beginTransaction();
    it.putOperationalData(createPath(macAddress), builder.build());
    return it.commit();
  }

  /**
   * Remove FabricAddress from the MD-SAL data tree
   * @param macAddress  The MacAddress of an FabricAddress object
   * @return  Future containing the result of the remove operation
   */
  public Future<RpcResult<TransactionStatus>> removeHost(MacAddress macAddress) {
    final DataModificationTransaction it = dataService.beginTransaction();
    it.removeOperationalData(createPath(macAddress));
    return it.commit();
  }

  /**
   * Create InstanceIdentifier path for an FabricAddress in the MD-SAL data tree
   * @param macAddress  The MacAddress of an FabricAddress object
   * @return  InstanceIdentifier of the FabricAddress corresponding to the specified macAddress
   */
  private InstanceIdentifier<FabricAddress> createPath(MacAddress macAddress) {
    return InstanceIdentifier.<FabricAddresses>builder(FabricAddresses.class)
            .<FabricAddress, FabricAddressKey>child(FabricAddress.class, new FabricAddressKey(macAddress)).toInstance();
  }

  public Future<RpcResult<TransactionStatus>> removeAllHost() {
      final DataModificationTransaction it = dataService.beginTransaction();
      InstanceIdentifier<FabricAddresses> path = InstanceIdentifier.<FabricAddresses>builder(FabricAddresses.class).build();
      it.removeOperationalData(path);
      return it.commit();
  }
}