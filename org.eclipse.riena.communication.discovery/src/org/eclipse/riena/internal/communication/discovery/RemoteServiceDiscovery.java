/*******************************************************************************
 * Copyright (c) 2007, 2009 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    compeople AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.riena.internal.communication.discovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.caucho.hessian.client.HessianRuntimeException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

import org.eclipse.equinox.log.Logger;

import org.eclipse.riena.communication.core.IRemoteServiceReference;
import org.eclipse.riena.communication.core.IRemoteServiceRegistration;
import org.eclipse.riena.communication.core.IRemoteServiceRegistry;
import org.eclipse.riena.communication.core.RemoteServiceDescription;
import org.eclipse.riena.communication.core.factory.RemoteServiceFactory;
import org.eclipse.riena.communication.core.publisher.IServicePublishEventDispatcher;
import org.eclipse.riena.core.Log4r;

/**
 * TODO: JavaDoc
 */
public class RemoteServiceDiscovery {
	// private static final String HOST_ID = RemoteServiceDiscovery.class.getName();
	private static final RemoteServiceDescription[] EMPTY_SERVICE_ENTRY_ARRAY = new RemoteServiceDescription[0];
	private IRemoteServiceRegistry registry;
	private RemoteServiceFactory rsFactory;
	private Map<String, RemoteServiceDescription> unpublishedServices = new HashMap<String, RemoteServiceDescription>();
	private BundleContext context;

	private final static Logger LOGGER = Log4r.getLogger(Activator.getDefault(), RemoteServiceDiscovery.class);

	RemoteServiceDiscovery(BundleContext context) {
		super();
		this.context = context;
	}

	/**
	 * Returns all services this publisher has
	 * 
	 * @return
	 */
	private synchronized RemoteServiceDescription[] getAllServices() {
		ServiceReference refPublisher = context.getServiceReference(IServicePublishEventDispatcher.class.getName());
		if (refPublisher == null) {
			LOGGER.log(LogService.LOG_WARNING, "no IServicePublishEventDispatcher service available [" //$NON-NLS-1$
					+ IServicePublishEventDispatcher.class.getName() + "]"); //$NON-NLS-1$
			return EMPTY_SERVICE_ENTRY_ARRAY;
		}
		IServicePublishEventDispatcher servicePublisher = (IServicePublishEventDispatcher) context
				.getService(refPublisher);
		try {
			RemoteServiceDescription[] rsDescriptions = servicePublisher.getAllServices();
			if (rsDescriptions == null) {
				return EMPTY_SERVICE_ENTRY_ARRAY;
			}
			return rsDescriptions;
		} finally {
			context.ungetService(refPublisher);
		}
	}

	private synchronized void updateInRegistry(RemoteServiceDescription[] rsDescriptions) {
		// get all services not just the service created with discovery
		List<IRemoteServiceRegistration> registeredServices = registry.registeredServices(null);
		Map<String, IRemoteServiceRegistration> existingPathsMap = new HashMap<String, IRemoteServiceRegistration>();
		// copy array to Map
		for (IRemoteServiceRegistration registeredService : registeredServices) {
			String url = registeredService.getReference().getURL();
			existingPathsMap.put(url, registeredService);
		}

		// newServices contains those that did not exist previously
		List<RemoteServiceDescription> newServices = new ArrayList<RemoteServiceDescription>();
		// remove those that still exist in the new list from the list of
		// existing paths
		for (RemoteServiceDescription rsDescription : rsDescriptions) {
			if (existingPathsMap.get(rsDescription.getURL()) != null) {
				existingPathsMap.remove(rsDescription.getURL());
			} else {
				newServices.add(rsDescription);
			}
		}

		// all other paths are now longer in the current list rsDescriptions and
		// have to be unpublished if they were created with my HOST_ID
		for (IRemoteServiceRegistration serviceReg : existingPathsMap.values()) {
			if (serviceReg.getReference().getContext().equals(Activator.getDefault().getContext())) {
				serviceReg.unregister();
			}
		}

		// go through the newService Descriptions and create a new proxy for
		// each of them
		List<IRemoteServiceReference> rsReferences = new ArrayList<IRemoteServiceReference>();
		for (RemoteServiceDescription rsDesc : newServices) {
			Class<?> interfaceClass = loadClass(rsDesc);
			rsDesc.setServiceInterfaceClass(interfaceClass);

			LOGGER.log(LogService.LOG_DEBUG, "creating service with uri=" + rsDesc.getURL()); //$NON-NLS-1$
			IRemoteServiceReference rsRef = createReference(rsDesc);
			if (rsRef != null) {
				rsReferences.add(rsRef);
			} else {
				LOGGER.log(LogService.LOG_DEBUG, "*****************"); //$NON-NLS-1$
				addAsUnpublished(rsDesc);
			}

		}
		for (IRemoteServiceReference rsRef : rsReferences) {
			// publish the new remote service references in the registry
			registry.registerService(rsRef, Activator.getDefault().getContext());
		}
	}

	private IRemoteServiceReference createReference(RemoteServiceDescription rsDesc) {
		IRemoteServiceReference rsRef = rsFactory.createProxy(rsDesc);
		rsRef.setContext(Activator.getDefault().getContext());
		return rsRef;
	}

	private void addAsUnpublished(RemoteServiceDescription rsDesc) {
		unpublishedServices.put(rsDesc.getURL(), rsDesc);
	}

	protected void checkForUnpublishedServices(String protocol) {
		Set<Entry<String, RemoteServiceDescription>> set = unpublishedServices.entrySet();
		for (Map.Entry<String, RemoteServiceDescription> entry : set) {
			RemoteServiceDescription rsDesc = entry.getValue();
			if (rsDesc.getProtocol().equals(protocol)) {
				IRemoteServiceReference rsRef = createReference(rsDesc);

				if (rsRef != null) {
					registry.registerService(rsRef, Activator.getDefault().getContext());
					set.remove(entry); // also removes the entry from the map
				}
			}
		}

	}

	private Class<?> loadClass(RemoteServiceDescription endpoint) {
		if (endpoint.getServiceInterfaceClass() != null) {
			// Maybe the interface was transfered from server
			return endpoint.getServiceInterfaceClass();
		}
		String interfaceClassName = endpoint.getServiceInterfaceClassName();

		try {
			// Class loading: Maybe the host bundle of the interface is a buddy
			// friend of the service registry
			return new RemoteServiceFactory().loadClass(interfaceClassName);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not create web service interface for [" + interfaceClassName + "]", e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void bind(IRemoteServiceRegistry registry) {
		this.registry = registry;
	}

	public void unbind(IRemoteServiceRegistry registry) {
		this.registry = registry;
	}

	void start() {
		update();
	}

	void update() {
		if (registry != null) {
			try {
				RemoteServiceDescription[] serviceDescriptions = getAllServices();
				if (serviceDescriptions.length > 0) {
					updateInRegistry(serviceDescriptions);
				}
			} catch (HessianRuntimeException ex) {
				LOGGER.log(LogService.LOG_ERROR, "update of services from server failed. " + ex.getLocalizedMessage()); //$NON-NLS-1$
			}
		}
	}

	void stop() {
		if (registry != null) {
			// unregister all services I registered
			List<IRemoteServiceRegistration> registeredServices = registry.registeredServices(Activator.getDefault()
					.getContext());
			for (IRemoteServiceRegistration rsReg : registeredServices) {
				rsReg.unregister();
			}
		}
	}

	void setRemoteServiceFactory(RemoteServiceFactory remoteServiceFactory) {
		this.rsFactory = remoteServiceFactory;
	}
}
