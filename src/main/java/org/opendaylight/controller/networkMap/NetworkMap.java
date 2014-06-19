package org.opendaylight.controller.networkMap;

//TODO: Use jaxb for easy xml
// OpenDaylight imports
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.core.IContainer;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.IPv4;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.reader.IPluginInReadService;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Switch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Java imports
/**
 * Leverage protocols_plugins.openflow to: 1. Build a DB of all switch node
 * properties 2. Build a topology map
 */

public class NetworkMap implements IInventoryListener, IListenDataPacket {

    /*
     * Unneeded because Node implements ToString() to create a unique identifier
     * for each node
     */
    private IPluginInInventoryService _openFlowInventory;
    private ConcurrentMap<Node, Map<String, Property>> _openFlowNodeProperties;

    private IPluginInReadService _openFlowReadService; // we want
                                                       // readDescription()
    private IDataPacketService _dataPacketService; // for packet decoding and
    // inspection
    private IContainer _container; // reference container so we can access
                                   // managers above SAL
    private ISwitchManager _switchManager; // reference for the switch manager

    private ConcurrentHashMap<String, Map<String, Property>> _nodeProperties = new ConcurrentHashMap<String, Map<String, Property>>();
    private ConcurrentHashMap<String, Node> _nodeData = new ConcurrentHashMap<String, Node>(); // properties
    // of
    // network
    // nodes, we'll only keep
    // switches

    private ConcurrentMap<String, NodeConnector> _connectorData;
    private static final Logger log = LoggerFactory.getLogger(NetworkMap.class); // log
                                                                                 // actions

    /* Function called once the bundle is loaded into the OSGI registry */
    /* Let's get references to all the services we need */
    void started() {

        _openFlowInventory = (IPluginInInventoryService) ServiceHelper
                .getInstance(IPluginInInventoryService.class,
                        _container.getName(), this);

        _openFlowNodeProperties = _openFlowInventory.getNodeProps();

        _openFlowReadService = (IPluginInReadService) ServiceHelper
                .getInstance(IPluginInReadService.class, _container.getName(),
                        this);

        _switchManager = (ISwitchManager) ServiceHelper.getInstance(
                ISwitchManager.class, _container.getName(), this);

        List<Switch> switchUpdate = _switchManager.getNetworkDevices(); // get
                                                                        // existing
                                                                        // devices

        // TODO:Get information existing nodes and node connectors, lest this
        // package be started after the networks
        Set<Node> nodes = _switchManager.getNodes();
    }

    /* When loaded out of OSGI registry */
    void stopped() {

    }

    /* Called on update to node inventory */
    public void notifyNode(Node node, UpdateType type,
            Map<String, Property> propMap) {

        if (_switchManager == null) {
            log.trace("No switch manager to retrieve properties!");
            return;
        }

        if (_openFlowReadService == null) {
            log.trace("No OpenFlow Reader service attached!");
            return;
        }

        if (_openFlowInventory == null) {
            log.trace("No OpenFlow Inventory service attached!");
            return;
        }

        log.trace("Received implementation agnostic node update");

        if (node.getType() == NodeIDType.OPENFLOW)// we care only for openflow
                                                  // nodes
        {
            String nodeId = node.toString(); // key for hashmap

            switch (type) {
            case ADDED:
                _nodeData.put(nodeId, node);

                // get the switch properties for a particular node, subset of OF
                // properties included
                Map<String, Property> switchProps = _switchManager
                        .getNodeProps(node);

                _nodeProperties.put(nodeId, switchProps);

                // update properties for all nodes using OF Inventory
                // _openFlowNodeProperties = _openFlowInventory.getNodeProps();

                // Map<String, Property> propsMap = _nodeProperties
                // .get("capabilities");

                log.trace("Added a new node : " + nodeId);

                break;
            case CHANGED:
                break;
            case REMOVED:
                break;

            default:
                log.trace("Uknown notifyNode update type: '" + type.toString()
                        + "'");
                break;
            }
        }

    }

    /* Called on update to nodeConnector (port) inventory */
    public void notifyNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Map<String, Property> propMap) {

        log.trace("Received implementation agnostic  connector update");
        String updateConnectorId = nodeConnector.toString();
    }

    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        // TODO Auto-generated method stub
        if (inPkt == null) {
            return PacketResult.IGNORED;
        }
        log.trace("Received data packet of ", inPkt.getPacketData().length,
                " bytes");

        Ethernet decodedPkt = (Ethernet) _dataPacketService
                .decodeDataPacket(inPkt);
        IPv4 ipv4Hdr = (IPv4) decodedPkt.getPayload();

        byte[] payload = ipv4Hdr.getRawPayload();

        return null;
    }

    /*
     * These two methods allows to attach to the data packet service above the
     * SAL
     */
    void setDataPacketService(IDataPacketService s) {
        _dataPacketService = s;
    }

    void unsetDataPacketService(IDataPacketService s) {
        if (_dataPacketService == s) {
            _dataPacketService = null;
        }
    }

    /*
     * These two methods let us attach _container to the package's container and
     * therefore access the various managers in the controller platform
     */
    void setIContainer(IContainer c) {
        _container = c;
    }

    public void unsetIContainer(IContainer s) {
        if (_container == s) {
            _container = null;
        }
    }
}