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
import org.eclipse.riena.core.service.Injector;
import org.eclipse.riena.core.service.ServiceId;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

public class Activator implements BundleActivator {

	private RemoteServiceDiscovery discovery;
	private Injector registryInjector;
	private String HOST_ID = Activator.class.getName();
	private IRemoteServiceRegistration servicePublisherReg;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		RemoteServiceFactory factory = new RemoteServiceFactory();

		discovery = new RemoteServiceDiscovery(context);
		discovery.setRemoteServiceFactory(factory);

		registryInjector = new ServiceId(IRemoteServiceRegistry.ID).injectInto(discovery).andStart(context);
		discovery.start();

		// Thread t = new Thread() {
		// public void run() {
		// boolean firstRun = true;
		// agent.start();
		// System.out.println("thread start");
		// while (true) {
		// try {
		// if (firstRun) {
		// Thread.sleep(1000);
		// } else {
		// Thread.sleep(10000);
		// }
		// firstRun = false;
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// if (agent != null) {
		// agent.update();
		// } else {
		// break;
		// }
		// }
		// System.out.println("thread end");
		// }
		// };
		// t.start();

		servicePublisherReg = factory.createAndRegisterProxy(IServicePublishEventDispatcher.class,
				"http://${hostname}/hessian/ServicePublisherWS", "hessian", null, HOST_ID);

		ProtocolNotifier protNotifier = new ProtocolNotifier();
		context.addServiceListener(protNotifier, "(objectClass=" + IRemoteServiceFactory.ID + ")");

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
	}

	class ProtocolNotifier implements ServiceListener {
		public void serviceChanged(ServiceEvent event) {
			if (event.getType() == ServiceEvent.REGISTERED) {
				String protocol = (String) event.getServiceReference().getProperty(IRemoteServiceFactory.PROP_PROTOCOL);
				discovery.checkForUnpublishedServices(protocol);
			}
		}

	}
}
