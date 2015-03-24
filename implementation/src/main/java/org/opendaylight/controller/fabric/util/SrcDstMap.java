package org.opendaylight.controller.fabric.util;
import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;

import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

public class SrcDstMap implements Serializable{
    private static final long serialVersionUID = 1L;
    @XmlElement
    private NodeId src;
    @XmlElement
    private NodeId dst;
    @XmlElement
    private Long tag;

    public SrcDstMap(){
        return;
    }

    public SrcDstMap(NodeId src, NodeId dst, Long tag){
        this.src = src;
        this.dst = dst;
        this.tag = tag;
        return;
    }

    public void setSrc(NodeId src){
        this.src = src;
        return;
    }

    public void setDst(NodeId dst){
        this.dst = dst;
        return;
    }

    public void setTag(Long tag){
        this.tag = tag;
        return;
    }

    public NodeId getSrc(){
        return this.src;
    }
    public NodeId getDst(){
        return this.dst;
    }
    public Long getTag(){
        return this.tag;
    }
    public String getSrcString(){
        return HexEncode.longToHexString(Long.parseLong(this.src.getValue().substring(this.src.getValue().indexOf(":")+1)));
        //return this.src.getValue();
    }
    public String getDstString(){
        return HexEncode.longToHexString(Long.parseLong(this.dst.getValue().substring(this.dst.getValue().indexOf(":")+1)));
    }
    @Override
    public String toString() {
        return "SrcDstMap [Src=" + this.src.getValue() + ",Dst=" + this.dst.getValue() + ",Tag=" + this.tag.toString()+"]";
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((src == null) ? 0 : src.hashCode());
        result = prime * result + ((dst == null) ? 0 : dst.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj){
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SrcDstMap other = (SrcDstMap) obj;
        if(!this.src.getValue().equals(other.src.getValue())){
            return false;
        }
        if(!this.dst.getValue().equals(other.dst.getValue())){
            return false;
        }
        return true;
    }

}
