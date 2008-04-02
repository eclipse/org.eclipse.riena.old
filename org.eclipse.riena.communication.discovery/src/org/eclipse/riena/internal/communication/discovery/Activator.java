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
package org.eclipse.riena.internal.communication.discovery;

import org.eclipse.riena.communication.core.IRemoteServiceRegistration;
import org.eclipse.riena.communication.core.IRemoteServiceRegistry;
import org.eclipse.riena.communication.core.factory.IRemoteServiceFactory;
import org.eclipse.riena.communication.core.factory.RemoteServiceFactory;
import org.eclipse.riena.communication.core.publisher.IServicePublishEventDispatcher;
import org.eclipse.riena.core.RienaActivator;
import org.eclipse.riena.core.injector.Inject;
import org.eclipse.riena.core.service.ServiceInjector;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

public class Activator extends RienaActivator {

	private RemoteServiceDiscovery discovery;
	private ServiceInjector registryInjector;
	private String HOST_ID = Activator.class.getName();
	private IRemoteServiceRegistration servicePublisherReg;

	private static Activator plugin;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		RemoteServiceFactory factory = new RemoteServiceFactory();

		discovery = new RemoteServiceDiscovery(context);
		discovery.setRemoteServiceFactory(factory);

		registryInjector = Inject.service(IRemoteServiceRegistry.class.getName()).useRanking().into(discovery)
				.andStart(context);
		discovery.start();

		servicePublisherReg = factory.createAndRegisterProxy(IServicePublishEventDispatcher.class,
				"http://${hostname}/hessian/ServicePublisherWS", "hessian", null, HOST_ID);

		ProtocolNotifier protNotifier = new ProtocolNotifier();
		context.addServiceListener(protNotifier, "(objectClass=" + IRemoteServiceFactory.class.getName() + ")");

		// ToDo Service Update Listener

		discovery.update();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		discovery.stop();
		registryInjector.stop();
		if (servicePublisherReg != null) {
			servicePublisherReg.unregister();
		}
		discovery = null;
		registryInjector = null;
		servicePublisherReg = null;
		plugin = null;

		super.stop(context);
	}

	class ProtocolNotifier implements ServiceListener {
		public void serviceChanged(ServiceEvent event) {
			if (event.getType() == ServiceEvent.REGISTERED) {
				String protocol = (String) event.getServiceReference().getProperty(IRemoteServiceFactory.PROP_PROTOCOL);
				discovery.checkForUnpublishedServices(protocol);
			}
		}

	}

	/**
	 * @return
	 */
	public static Activator getDefault() {
		return plugin;
	}
}
