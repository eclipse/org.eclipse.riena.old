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
package org.eclipse.riena.internal.exceptionhandler;

import org.eclipse.equinox.log.Logger;
import org.eclipse.riena.core.exception.IExceptionHandler;
import org.eclipse.riena.core.exception.IExceptionHandlerManager.Action;
import org.osgi.service.log.LogService;

/**
 * 
 */
public class DefaultExceptionHandler implements IExceptionHandler {

	/**
	 * This handler name is "riena.exceptionhandler.default"
	 */
	public static final String NAME = "riena.exceptionhandler.default";

	/**
	 * this handler will not be invoked before any other handler
	 * 
	 * @see org.eclipse.riena.core.exception.IExceptionHandler#getBefore()
	 */
	public String getBefore() {
		// this handler will not be invoked before any other handler
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.riena.core.exception.IExceptionHandler#getName()
	 */
	public String getName() {
		return NAME;
	}

	public Action handleCaught(Throwable t, String msg, Logger logger) {
		if (logger != null) {
			logger.log(LogService.LOG_ERROR, msg, t);
			return Action.Ok;
		}
		return Action.NotHandled;
	}

	public Action handleUncaught(Throwable t, String msg, Logger logger) {
		if (logger != null) {
			logger.log(LogService.LOG_ERROR, msg, t);
			return Action.Ok;
		}
		return Action.NotHandled;
	}

}
