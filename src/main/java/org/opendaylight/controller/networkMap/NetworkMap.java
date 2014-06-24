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
    private ConcurrentHashMap<String, OFSwitch> _networkSwitches;

    private IPluginInInventoryService _openFlowInventory; // complete inventory
    private IPluginInReadService _openFlowReadService; // provides OF stats
    private IDataPacketService _dataPacketService; // packets above SAL
    private IContainer _container; // container reference for manager access
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

        _networkSwitches = new ConcurrentHashMap<String, OFSwitch>();
        /*
         * Get nodes that already exist, retrieve properties, add OFSwitch to
         * local map
         */
        ConcurrentMap<Node, Map<String, Property>> allNodeProps = _openFlowInventory
                .getNodeProps();

        for (Node n : allNodeProps.keySet()) {
            AddSwitch(n);
        }

        // TODO: existing nodeConnectors

    }

    /* When loaded out of OSGI registry */
    void stopped() {

    }

    /* Called on update to node inventory */
    public void notifyNode(Node node, UpdateType type,
            Map<String, Property> propMap) {

        if (_switchManager == null) {
            log.trace("No switch manager service attached!");
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
            String nodeId = node.getNodeIDString();

            switch (type) {
            /*
             * Get switch properties, create an instance of OFSwitch add to our
             * hashmap
             */
            case ADDED:
                this.AddSwitch(node);
                log.trace("Added a new switch : " + nodeId);
                break;

            /* Look up the switch in our map and update the properties */
            case CHANGED:
                this.UpdateSwitch(node);
                log.trace("Update switch : " + nodeId);
                break;

            /* Remove the switch from our map */
            case REMOVED:
                _networkSwitches.remove(nodeId);
                log.trace("Removed switch : " + nodeId);
                // TODO:Handle switch doesn't exist, possibly mishandled "ADDED"
                // message earlier
                break;

            default:
                log.trace("Uknown notifyNode update type: '" + type.toString()
                        + "'");
                break;
            }
        }

    }

    // add switch to local map, get it's properties and ports
    private void AddSwitch(Node node) {
        String nodeId = node.toString();
        Map<String, Property> switchProps = GetSingleNodeProps(node);
        OFSwitch newSwitch = new OFSwitch(nodeId);
        newSwitch.MapSwitchProperties(switchProps);
        _networkSwitches.put(nodeId, newSwitch);

        // TODO: Handle switch already exsists, possible duplicate
        // nodeId
        Set<NodeConnector> connectors = _switchManager.getNodeConnectors(node);

        for (NodeConnector connector : connectors) {
            Map<String, Property> portProps = _switchManager
                    .getNodeConnectorProps(connector);
            Propert a = portProps.get)
        }

    }

    private void UpdateSwitch(Node node) {
        OFSwitch updatedSwitch = _networkSwitches.get(node.getNodeIDString());
        updatedSwitch.MapSwitchProperties(GetSingleNodeProps(node));
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