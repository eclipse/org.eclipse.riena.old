/*******************************************************************************
 * Copyright (c) 2007, 2008 compeople AG and others.
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

import org.eclipse.riena.core.RienaActivator;
import org.eclipse.riena.core.exception.IExceptionHandler;
import org.eclipse.riena.core.exception.IExceptionHandlerManager;
import org.eclipse.riena.core.injector.Inject;
import org.eclipse.riena.core.service.ServiceInjector;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator extends RienaActivator {

	private ServiceInjector handlerManagerInjector;
	private ServiceRegistration handlerManagerReg;

	// The shared instance
	private static Activator plugin;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		Activator.plugin = this;
		registerExceptionHandlerManager();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		unregisterExceptionHandlerManager();
		Activator.plugin = null;
		super.stop(context);
	}

	/**
	 * Get the plugin instance.
	 * 
	 * @return
	 */
	public static Activator getDefault() {
		return plugin;
	}

	private void registerExceptionHandlerManager() {
		ExceptionHandlerManagerDefault handlerManager = new ExceptionHandlerManagerDefault();
		String handlerId = IExceptionHandler.class.getName();

		handlerManagerInjector = Inject.service(handlerId).into(handlerManager).andStart(getContext());

		Hashtable<String, String> properties = new Hashtable<String, String>(0);
		handlerManagerReg = getContext().registerService(IExceptionHandlerManager.class.getName(), handlerManager,
				properties);
	}

	private void unregisterExceptionHandlerManager() {

		handlerManagerReg.unregister();
		handlerManagerReg = null;
		handlerManagerInjector.stop();
		handlerManagerInjector = null;
	}
}
