package org.opendaylight.controller.networkMap;

//TODO: Use jaxb for easy xml
// OpenDaylight imports
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.networkComponents.OFSwitch;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Java imports
/**
 * Leverage protocols_plugins.openflow to: 1. Build a DB of all switch node
 * properties 2. Build a topology map
 */

public class NetworkMap implements IInventoryListener, IListenDataPacket {

    // Our own hashmap of nodes using our own switch class
    private ConcurrentHashMap<String, OFSwitch> _networkNodes;

    private IPluginInInventoryService _openFlowInventory; // complete inventory
    private IPluginInReadService _openFlowReadService; // provides OF statistics
                                                       // access
    private IDataPacketService _dataPacketService; // intercept packets above
                                                   // SAL
    private IContainer _container; // container reference for service access
    private ISwitchManager _switchManager; // access switch and port details

    private ConcurrentMap<String, NodeConnector> _connectorData;
    private static final Logger log = LoggerFactory.getLogger(NetworkMap.class);

    /* Function called once the bundle is loaded into the OSGI registry */
    /* Let's get references to all the services we need */
    void started() {

        _openFlowInventory = (IPluginInInventoryService) ServiceHelper
                .getInstance(IPluginInInventoryService.class,
                        _container.getName(), this);

        _openFlowReadService = (IPluginInReadService) ServiceHelper
                .getInstance(IPluginInReadService.class, _container.getName(),
                        this);

        _switchManager = (ISwitchManager) ServiceHelper.getInstance(
                ISwitchManager.class, _container.getName(), this);

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

                Map<String, Property> switchProps = GetSingleNodeProps(node);
                OFSwitch newSwitch = new OFSwitch(nodeId);
                newSwitch.MapSwitchProperties(switchProps);
                _networkNodes.put(nodeId, newSwitch);
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

    /* Get the properties of a single node */
    Map<String, Property> GetSingleNodeProps(Node node) {

        return (_switchManager == null) ? null : _switchManager
                .getNodeProps(node);
    }

    /* Get the properties for all nodes */
    private ConcurrentMap<Node, Map<String, Property>> GetAllNodeProps() {
        return (_openFlowInventory == null) ? null : _openFlowInventory
                .getNodeProps();
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