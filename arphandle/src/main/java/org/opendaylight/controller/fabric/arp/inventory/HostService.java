package org.opendaylight.controller.fabric.arp.inventory;

import java.util.Hashtable;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;

public class HostService {
    private Hashtable<IpAddress,MacAddress> ipMacAddressMaps;
    private Hashtable<MacAddress,NodeConnectorRef> macNodeConnectorRefMaps;
    /**
     * constructor
     */
    public HostService(){
        this.ipMacAddressMaps = new Hashtable<IpAddress,MacAddress>();
        this.macNodeConnectorRefMaps = new Hashtable<MacAddress,NodeConnectorRef>();
        return;
    }
    public void clear(){
        this.ipMacAddressMaps.clear();
        this.macNodeConnectorRefMaps.clear();
    }
    /*
     * MacAddress & NodeConnectorRef maps function
     */
    public boolean addAddressNodeConnector(MacAddress mac,NodeConnectorRef ncr){
        NodeConnectorRef lncr = this.macNodeConnectorRefMaps.put(mac, ncr);
        return ncr.equals(lncr);
    }
    public NodeConnectorRef getNodeConnectorRefByMac(MacAddress mac){
        return this.macNodeConnectorRefMaps.get(mac);
    }
    public void removeNodeConnectorRefByMac(MacAddress mac){
        this.macNodeConnectorRefMaps.remove(mac);
        return;
    }

    /*
     * MacAddress & IpAddress maps function
     */
    public boolean addAddress(IpAddress ip,MacAddress mac){
        MacAddress lMac = this.ipMacAddressMaps.put(ip,mac);
        return mac.equals(lMac);
    }
    public MacAddress getMacByIp(IpAddress ip){
        return this.ipMacAddressMaps.get(ip);
    }
    public void removeByMac(MacAddress mac){
        this.ipMacAddressMaps.remove(mac);
        return;
    }
    public void removeByIp(IpAddress ip){
        this.ipMacAddressMaps.remove(ip);
        return;
    }
}
