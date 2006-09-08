//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2003 Jan 31: Added the ability to imbed RRA information in poller packages.
// 2003 Jan 31: Cleaned up some unused imports.
// 2003 Jan 29: Added response times to certain monitors.
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
// Tab Size = 8
//

package org.opennms.netmgt.poller.monitors;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetAddress;
import java.util.Map;
import java.util.HashMap;

import org.apache.log4j.Level;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.netmgt.config.PollerConfig;
import org.opennms.netmgt.config.SnmpPeerFactory;
import org.opennms.netmgt.model.PollStatus;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.NetworkInterface;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.netmgt.snmp.SnmpInstId;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpUtils;
import org.opennms.netmgt.snmp.SnmpValue;
import org.opennms.netmgt.utils.ParameterMap;

/**
 * <P>
 * This class is designed to be used by the service poller framework to test the
 * status of PERC raid controllers on Dell Servers running Dell OpenManage Storage Manager. 
 * The class implements
 * the ServiceMonitor interface that allows it to be used along with other
 * plug-ins by the service poller framework.
 * </P>
 * 
 * @author <A HREF="mailto:tarus@opennms.org">Tarus Balog </A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 * 
 */
final public class OmsaStorageMonitor extends SnmpMonitorStrategy {
    /**
     * Name of monitored service.
     */
    private static final String SERVICE_NAME = "OMSAStorageMonitor";

    /**
     * The base OID for the logical device status information
     */
    private static final String LOGICAL_BASE_OID = ".1.3.6.1.4.1.674.10893.1.20.140.1.1.19"; 


    /**
     * The base OID for the physical device status information
     */
    private static final String PHYSICAL_BASE_OID = ".1.3.6.1.4.1.674.10893.1.20.130.4.1.23";

    /**
     * Interface attribute key used to store the interface's SnmpAgentConfig
     * object.
     */
    static final String SNMP_AGENTCONFIG_KEY = "org.opennms.netmgt.snmp.SnmpAgentConfig";

    /**
     * <P>
     * Returns the name of the service that the plug-in monitors ("OMSAStorageMonitor").
     * </P>
     * 
     * @return The service that the plug-in monitors.
     */
    public String serviceName() {
        return SERVICE_NAME;
    }

    /**
     * <P>
     * Initialize the service monitor.
     * </P>
     * 
     * @param parameters
     *            Not currently used.
     * 
     * @exception RuntimeException
     *                Thrown if an unrecoverable error occurs that prevents the
     *                plug-in from functioning.
     * 
     */
    public void initialize(PollerConfig pollerConfig, Map parameters) {
        // Initialize the SnmpPeerFactory
        //
        try {
            SnmpPeerFactory.init();
        } catch (MarshalException ex) {
        	log().fatal("initialize: Failed to load SNMP configuration", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (ValidationException ex) {
        	log().fatal("initialize: Failed to load SNMP configuration", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (IOException ex) {
        	log().fatal("initialize: Failed to load SNMP configuration", ex);
            throw new UndeclaredThrowableException(ex);
        }

        return;
    }

    /**
     * <P>
     * Called by the poller framework when an interface is being added to the
     * scheduler. Here we perform any necessary initialization to prepare the
     * NetworkInterface object for polling.
     * </P>
     * 
     * @exception RuntimeException
     *                Thrown if an unrecoverable error occurs that prevents the
     *                interface from being monitored.
     */
    public void initialize(MonitoredService svc) {
        NetworkInterface iface = svc.getNetInterface();
        // Log4j category
        //
        // Get interface address from NetworkInterface
        //
        super.initialize(svc);

        InetAddress ipAddr = (InetAddress) iface.getAddress();

        SnmpAgentConfig agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(ipAddr);
        if (log().isDebugEnabled()) {
            log().debug("initialize: SnmpAgentConfig address: " + agentConfig);
        }

        // Add the snmp config object as an attribute of the interface
        //
        if (log().isDebugEnabled())
            log().debug("initialize: setting SNMP peer attribute for interface " + ipAddr.getHostAddress());

        iface.setAttribute(SNMP_AGENTCONFIG_KEY, agentConfig);

        log().debug("initialize: interface: " + agentConfig.getAddress() + " initialized.");

        return;
    }

    /**
     * <P>
     * The poll() method is responsible for polling the specified address for
     * SNMP service availability.
     * </P>
     * @param parameters
     *            The package parameters (timeout, retry, etc...) to be used for
     *            this poll.
     * @param iface
     *            The network interface to test the service on.
     * 
     * @return The availability of the interface and if a transition event
     *         should be supressed.
     * 
     * @exception RuntimeException
     *                Thrown for any uncrecoverable errors.
     */
    public PollStatus poll(MonitoredService svc, Map parameters, org.opennms.netmgt.config.poller.Package pkg) {
        NetworkInterface iface = svc.getNetInterface();

        PollStatus status = PollStatus.unavailable();
        InetAddress ipaddr = (InetAddress) iface.getAddress();

        // Retrieve this interface's SNMP peer object
        //
        SnmpAgentConfig agentConfig = (SnmpAgentConfig) iface.getAttribute(SNMP_AGENTCONFIG_KEY);
        if (agentConfig == null) throw new RuntimeException("SnmpAgentConfig object not available for interface " + ipaddr);

        // Get configuration parameters
        //
        // set timeout and retries on SNMP peer object
        //
        agentConfig.setTimeout(ParameterMap.getKeyedInteger(parameters, "timeout", agentConfig.getTimeout()));
        agentConfig.setRetries(ParameterMap.getKeyedInteger(parameters, "retries", agentConfig.getRetries()));
        agentConfig.setPort(ParameterMap.getKeyedInteger(parameters, "port", agentConfig.getPort()));

        if (log().isDebugEnabled()) log().debug("poll: service= SNMP address= " + agentConfig);


        // Create a hash map of available statuses for both physical and
        // logical disks
        HashMap percStatusMap = new HashMap();

        percStatusMap.put(1,"Other");
        percStatusMap.put(2,"Unknown");
        percStatusMap.put(3,"OK");
        percStatusMap.put(4,"Non-Critical");
        percStatusMap.put(5,"Critical");
        percStatusMap.put(6,"Non-Recoverable");

        // Create a variable to hold the state
        String state = null;

        
        // Establish SNMP session with interface
        //
        try {
            if (log().isDebugEnabled()) {
                log().debug("OmsaStorageMonitor.poll: SnmpAgentConfig address: " +agentConfig);
            }
            SnmpObjId snmpObjectId = new SnmpObjId(PHYSICAL_BASE_OID);

            // First walk the physical OID Tree and check the returned values 

            Map<SnmpInstId, SnmpValue> results = SnmpUtils.getOidValues(agentConfig, "percPoller", snmpObjectId);

            if(results.size() == 0) {
                log().debug("SNMP poll failed: no results, addr=" + ipaddr.getHostAddress() + " oid=" + snmpObjectId);
                return status;
            }

            for (Map.Entry<SnmpInstId, SnmpValue> e : results.entrySet()) { 

                state = percStatusMap.get(e.getValue());
                log().debug("poll: SNMPwalk poll succeeded, addr=" + ipaddr.getHostAddress() + " oid=" + snmpObjectId + " instance=" + e.getKey() + " value=" + state);
		/*
		Name
			arrayDiskRollUpStatus
		Object ID
			1.3.6.1.4.1.674.10893.1.20.130.4.1.23
		Description
			Severity of the array disk state. This is the combined status of the array disk and its components. Possible values:
			1: Other
			2: Unknown
			3: OK
			4: Non-critical
			5: Critical
			6: Non-recoverable
		*/
                
                
		// If disk is not "OK" based on the above generate an error
                if (meetsCriteria(e.getValue(), "=", "3")) {
                    status = PollStatus.available();
                    state = percStatusMap.get(e.getValue());
                    log().debug("poll: SNMP physical poll succeeded, addr=" + ipaddr.getHostAddress() + " oid=" + snmpObjectId + " instance=" + e.getKey() + " value=" + state);
                } else {
                    state = percStatusMap.get(e.getValue());
                    status = logDown(Level.DEBUG, "SNMP physical poll failed, addr=" + ipaddr.getHostAddress() + " oid=" + snmpObjectId + " instance=" + e.getKey() + " value=" + state);
                    return status;
                }
            }

            // If we get here, that means all of the physical drives returned a value not equal to "5"
            // Now we need to check logical drives

            SnmpObjId snmpLogObjectId = new SnmpObjId(LOGICAL_BASE_OID);

            // Next walk the physical OID Tree and check the returned values 

            Map<SnmpInstId, SnmpValue> lresults = SnmpUtils.getOidValues(agentConfig, "percPoller", snmpLogObjectId);

            if(lresults.size() == 0) {
                log().debug("SNMP poll failed: no logical results, addr=" + ipaddr.getHostAddress() + " oid=" + snmpLogObjectId);
                return status;
            }

            for (Map.Entry<SnmpInstId, SnmpValue> e : lresults.entrySet()) { 
                state = percStatusMap.get(e.getValue());
                log().debug("poll: SNMPwalk poll succeeded, addr=" + ipaddr.getHostAddress() + " oid=" + snmpLogObjectId + " instance=" + e.getKey() + " value=" + state);
		
		/*
		Name
			virtualDiskRollUpStatus
		Object ID
			1.3.6.1.4.1.674.10893.1.20.140.1.1.19
		Description
			Severity of the virtual disk state. This is the combined status of the virtual disk and its components. Possible values:
			1: Other
			2: Unknown
			3: OK
			4: Non-critical
			5: Critical
			6: Non-recoverable
		*/

                if (meetsCriteria(e.getValue(), "=", "3")) {
                    status = PollStatus.available();
                    state = percStatusMap.get(e.getValue());
                    log().debug("poll: SNMP physical poll succeeded, addr=" + ipaddr.getHostAddress() + " oid=" + snmpObjectId + " instance=" + e.getKey() + " value=" + state);
                } else {

                    state = percStatusMap.get(e.getValue());
                    status = logDown(Level.DEBUG, "SNMP logical disk poll failed, addr=" + ipaddr.getHostAddress() + " oid=" + snmpLogObjectId + " instance=" + e.getKey() + " value=" + state);
                    return status;
                }
            }

        } catch (NumberFormatException e) {
            status = logDown(Level.ERROR, "Number operator used on a non-number " + e.getMessage());
        } catch (IllegalArgumentException e) {
            status = logDown(Level.ERROR, "Invalid Snmp Criteria: " + e.getMessage());
        } catch (Throwable t) {
            status = logDown(Level.WARN, "Unexpected exception during SNMP poll of interface " + ipaddr.getHostAddress(), t);
        }

        return status;
    }

}
