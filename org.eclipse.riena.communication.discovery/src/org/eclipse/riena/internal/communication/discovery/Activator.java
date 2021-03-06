/*******************************************************************************
 * Copyright (c) 2007, 2010 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    compeople AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.riena.internal.communication.discovery;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

import org.eclipse.riena.communication.core.IRemoteServiceRegistration;
import org.eclipse.riena.communication.core.IRemoteServiceRegistry;
import org.eclipse.riena.communication.core.factory.IRemoteServiceFactory;
import org.eclipse.riena.communication.core.factory.RemoteServiceFactory;
import org.eclipse.riena.communication.core.publisher.IServicePublishEventDispatcher;
import org.eclipse.riena.core.RienaActivator;
import org.eclipse.riena.core.injector.Inject;
import org.eclipse.riena.core.injector.service.ServiceInjector;

public class Activator extends RienaActivator {

	private RemoteServiceDiscovery discovery;
	private ServiceInjector registryInjector;
	private IRemoteServiceRegistration servicePublisherReg;

	// The shared instance
	private static Activator plugin;

	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		Activator.plugin = this;
		final RemoteServiceFactory factory = new RemoteServiceFactory();

		discovery = new RemoteServiceDiscovery(context);
		discovery.setRemoteServiceFactory(factory);

		registryInjector = Inject.service(IRemoteServiceRegistry.class).useRanking().into(discovery).andStart(context);
		discovery.start();

		servicePublisherReg = factory.createAndRegisterProxy(IServicePublishEventDispatcher.class,
				"http://${riena.hostname}/hessian/ServicePublisherWS", "hessian", context); //$NON-NLS-1$ //$NON-NLS-2$

		final ProtocolNotifier protNotifier = new ProtocolNotifier();
		context.addServiceListener(protNotifier, "(objectClass=" + IRemoteServiceFactory.class.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$

		// ToDo Service Update Listener

		discovery.update();
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		discovery.stop();
		registryInjector.stop();
		if (servicePublisherReg != null) {
			servicePublisherReg.unregister();
		}
		discovery = null;
		registryInjector = null;
		servicePublisherReg = null;

		Activator.plugin = null;
		super.stop(context);
	}

	class ProtocolNotifier implements ServiceListener {
		public void serviceChanged(final ServiceEvent event) {
			if (event.getType() == ServiceEvent.REGISTERED) {
				final String protocol = (String) event.getServiceReference().getProperty(
						IRemoteServiceFactory.PROP_PROTOCOL);
				discovery.checkForUnpublishedServices(protocol);
			}
		}

	}

	/**
	 * Get the plugin instance.
	 * 
	 * @return
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
