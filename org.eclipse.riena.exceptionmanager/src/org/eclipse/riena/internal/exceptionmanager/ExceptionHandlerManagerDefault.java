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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.log.Logger;
import org.eclipse.riena.core.exception.IExceptionHandler;
import org.eclipse.riena.core.exception.IExceptionHandlerManager;

/**
 * 
 */
public class ExceptionHandlerManagerDefault implements IExceptionHandlerManager {

    private List<IExceptionHandler> handlers;

    public ExceptionHandlerManagerDefault() {
        handlers = new ArrayList<IExceptionHandler>();
    }

    public void addHandler(IExceptionHandler handler) {
        handlers = sort(handlers, handler);
    }

    private List<IExceptionHandler> sort(List<IExceptionHandler> existsHandler, IExceptionHandler handler) {
        List<TopologicalNode<IExceptionHandler>> nodes = new ArrayList<TopologicalNode<IExceptionHandler>>(existsHandler.size() + 1);
        TopologicalNode<IExceptionHandler> node = new TopologicalNode<IExceptionHandler>(handler.getName(), handler.getBefore(), handler);
        nodes.add(node);
        for (IExceptionHandler nextHandler : existsHandler) {
            node = new TopologicalNode<IExceptionHandler>(nextHandler.getName(), nextHandler.getBefore(), nextHandler);
            nodes.add(node);
        }
        return TopologicalSort.sort(nodes);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.riena.core.exception.IExceptionHandlerManager#handleCaught(java.lang.Throwable)
     */
    public Action handleCaught(Throwable t) {
        return handleCaught(t, null, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.riena.core.exception.IExceptionHandlerManager#handleCaught(java.lang.Throwable,
     *      org.eclipse.equinox.log.Logger)
     */
    public Action handleCaught(Throwable t, Logger logger) {
        return handleCaught(t, null, logger);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.riena.core.exception.IExceptionHandlerManager#handleCaught(java.lang.Throwable, java.lang.String)
     */
    public Action handleCaught(Throwable t, String msg) {
        return handleCaught(t, msg, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.riena.core.exception.IExceptionHandlerManager#handleCaught(java.lang.Throwable, java.lang.String,
     *      org.eclipse.equinox.log.Logger)
     */
    public Action handleCaught(Throwable t, String msg, Logger logger) {
        Action action = Action.NotHandled;
        for (IExceptionHandler handler : handlers) {
            action = handler.handleCaught(t, msg, logger);
            if (action != Action.NotHandled) {
                break;
            }
        }
        return action;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.riena.core.exception.IExceptionHandlerManager#handleUncaught(java.lang.Throwable,
     *      java.lang.Object, org.eclipse.equinox.log.Logger)
     */
    public Action handleUncaught(Throwable t, String msg, Logger logger) {
        Action action = Action.NotHandled;
        for (IExceptionHandler handler : handlers) {
            action = handler.handleUncaught(t, msg, logger);
            if (action != Action.NotHandled) {
                break;
            }
        }
        return action;
    }

    public void removeHandler(IExceptionHandler handler) {
        handlers.remove(handler);
    }
}