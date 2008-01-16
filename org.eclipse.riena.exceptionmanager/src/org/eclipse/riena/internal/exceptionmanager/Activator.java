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
package org.eclipse.riena.internal.exceptionmanager;

import java.util.Hashtable;

import org.eclipse.riena.core.exception.IExceptionHandler;
import org.eclipse.riena.core.exception.IExceptionHandlerManager;
import org.eclipse.riena.core.service.ServiceInjector;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
	private static Activator plugin;

	private BundleContext context;

	private ServiceInjector handlerManagerInjector;

	private ServiceRegistration handlerManagerReg;

	public static Activator getPlugin() {
		return plugin;
	}

	public BundleContext getContext() {
		return context;
	}

	private void registerExceptionHandlerManager() {
		ExceptionHandlerManagerDefault handlerManager = new ExceptionHandlerManagerDefault();
		String handlerId = IExceptionHandler.ID;
		String bindMethod = "addHandler";
		String unbindMethod = "removeHandler";

		handlerManagerInjector = new ServiceInjector(context, handlerId, handlerManager, bindMethod, unbindMethod);
		handlerManagerInjector.start();

		Hashtable<String, String> properties = new Hashtable<String, String>(0);
		handlerManagerReg = context.registerService(IExceptionHandlerManager.ID, handlerManager, properties);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		plugin = this;
		this.context = context;

		registerExceptionHandlerManager();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {

		unregisterExceptionHandlerManager();

		this.context = null;
		plugin = null;
	}

	private void unregisterExceptionHandlerManager() {

		handlerManagerReg.unregister();
		handlerManagerReg = null;
		handlerManagerInjector.dispose();
		handlerManagerInjector = null;
	}
}
