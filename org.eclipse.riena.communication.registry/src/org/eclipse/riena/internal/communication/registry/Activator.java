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

import java.util.Hashtable;

import org.eclipse.riena.communication.core.IRemoteServiceRegistry;
import org.eclipse.riena.core.RienaPlugin;
import org.eclipse.riena.core.logging.LogUtil;
import org.eclipse.riena.core.service.ServiceId;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Alexander Ziegler
 * @author Christian Campo
 * 
 */
public class Activator extends RienaPlugin {

	private static Activator plugin;
	private RemoteServiceRegistry serviceRegistry;
	private ServiceRegistration regServiceRegistry;
	private LogUtil logUtil;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		serviceRegistry = new RemoteServiceRegistry();
		serviceRegistry.start();

		Hashtable<String, Object> properties = ServiceId.newDefaultServiceProperties();
		regServiceRegistry = context.registerService(IRemoteServiceRegistry.ID, serviceRegistry, properties);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		regServiceRegistry.unregister();
		regServiceRegistry = null;

		serviceRegistry.stop();
		serviceRegistry = null;
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return plugin;
	}

}
