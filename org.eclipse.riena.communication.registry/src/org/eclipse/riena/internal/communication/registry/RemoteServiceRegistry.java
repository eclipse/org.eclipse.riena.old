/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    compeople AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.riena.internal.communication.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.equinox.log.Logger;
import org.eclipse.riena.communication.core.IRemoteServiceReference;
import org.eclipse.riena.communication.core.IRemoteServiceRegistration;
import org.eclipse.riena.communication.core.IRemoteServiceRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;

/**
 * @author Alexander Ziegler
 * @author Christian Campo
 * 
 */
public class RemoteServiceRegistry implements IRemoteServiceRegistry {
	private Map<String, IRemoteServiceRegistration> registeredServices;
	private BundleContext context;
	private Logger LOGGER = Activator.getDefault().getLogger(RemoteServiceRegistry.class.getName());

	synchronized void start() {
		registeredServices = new HashMap<String, IRemoteServiceRegistration>();
		this.context = Activator.getContext();
	}

	synchronized void stop() {
		// @TODO unregisterService changes the registeredServices collection,
		// the for loop collapses with ConcurrentModificationException
		IRemoteServiceRegistration[] arrayRS = registeredServices.values().toArray(
				new IRemoteServiceRegistration[registeredServices.values().size()]);
		for (IRemoteServiceRegistration serviceReg : arrayRS) {
			// unregisters all services for this registry
			unregisterService(serviceReg.getReference());
		}
		registeredServices.clear();
		registeredServices = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.riena.communication.core.IRemoteServiceRegistry#registerService(org.eclipse.riena.communication.core.IRemoteServiceReference)
	 */
	public IRemoteServiceRegistration registerService(IRemoteServiceReference reference) {

		String pid = reference.getConfigServicePID();
		if (pid != null) {
			ServiceReference[] refs;
			try {
				refs = Activator.getContext().getServiceReferences(ManagedService.class.getName(),
						"(" + Constants.SERVICE_PID + "=" + pid + ")");
				if (refs != null && refs.length > 0) {
					LOGGER.log(LogService.LOG_ERROR, "duplicate configuration " + Constants.SERVICE_PID + " = "
							+ reference.getConfigServicePID() + " for service "
							+ reference.getServiceInterfaceClassName() + " service is set to NOT configurable");
					pid = null;
				}
			} catch (InvalidSyntaxException e) {
				e.printStackTrace();
			}
		}

		String url = reference.getDescription().getURL();
		synchronized (registeredServices) {
			IRemoteServiceRegistration foundRemoteServiceReg = registeredServices.get(url);
			if (foundRemoteServiceReg == null) {
				// it is a new entry, set properties to null since we have no
				Properties props = new Properties();
				props.put("service.url", url);
				props.put("service.protocol", reference.getDescription().getProtocol());
				ServiceRegistration serviceRegistration = context.registerService(reference
						.getServiceInterfaceClassName(), reference.getServiceInstance(), props);
				reference.setServiceRegistration(serviceRegistration);

				if (reference.getConfigServiceInstance() != null && pid != null) {
					Hashtable<String, String> ht = new Hashtable<String, String>();
					ht.put(Constants.SERVICE_PID, pid);
					reference.setConfigServiceRegistration(context.registerService(ManagedService.class.getName(),
							reference.getConfigServiceInstance(), ht));
				}

				RemoteServiceRegistration remoteServiceReg = new RemoteServiceRegistration(reference, this);
				registeredServices.put(url, remoteServiceReg);

				System.out.println("Riena::RemoteServiceRegistry:: DEBUG: OSGi NEW service registered id: "
						+ reference.getServiceInterfaceClassName());
				return remoteServiceReg;
			} else {
				// for existing services copy over the service registration
				// @TODO review this logic
				reference.setServiceRegistration(foundRemoteServiceReg.getReference().getServiceRegistration());
				foundRemoteServiceReg.setReference(reference);
				return foundRemoteServiceReg;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.riena.communication.core.IRemoteServiceRegistry#unregisterService(org.eclipse.riena.communication.core.IRemoteServiceReference)
	 */
	public void unregisterService(IRemoteServiceReference reference) {
		assert reference != null : "RemoteServiceReference must not be null";
		synchronized (registeredServices) {
			ServiceRegistration serviceRegistration = reference.getServiceRegistration();
			serviceRegistration.unregister();
			if (reference.getConfigServiceRegistration() != null) {
				reference.getConfigServiceRegistration().unregister();
			}
			String id = reference.getServiceInterfaceClassName();
			registeredServices.remove(reference.getURL());
			reference.dispose();
			System.out.println("Riena::RemoteServiceRegistry:: DEBUG: OSGi service removed id: " + id);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.riena.communication.core.IRemoteServiceRegistry#registeredServices(java.lang.String)
	 */
	public List<IRemoteServiceRegistration> registeredServices(String hostId) {
		synchronized (registeredServices) {
			Collection<IRemoteServiceRegistration> values = registeredServices.values();
			// Answers all service registrations when hostId is null or "*"
			if (hostId == null || hostId.equals("*")) {
				return new ArrayList<IRemoteServiceRegistration>(values);
			}

			List<IRemoteServiceRegistration> result = new ArrayList<IRemoteServiceRegistration>();
			for (IRemoteServiceRegistration serviceReg : values) {
				String registeredHostId = serviceReg.getReference().getHostId();
				if (hostId.equals(registeredHostId)) {
					result.add(serviceReg);
				}
			}
			return result;
		}
	}
}
