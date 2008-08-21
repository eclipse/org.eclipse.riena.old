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
package org.eclipse.riena.security.common.bootpolicy;

import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;

import sun.security.provider.PolicyFile;

/**
 * 
 */
public class BootPolicy extends Policy {

	// private static Policy realPolicy;
	//
	// public static void setRealPolicy(Policy parmPolicy) {
	// System.out.println("proxy: setRealPolicy");
	// realPolicy = parmPolicy;
	// }
	private Policy defaultPolicy = new PolicyFile();

	/**
	 * 
	 */
	public BootPolicy() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.security.Policy#getPermissions(java.security.CodeSource)
	 */
	@Override
	public PermissionCollection getPermissions(CodeSource codesource) {
		System.out.println("defaultPolicy: getPermissions"); //$NON-NLS-1$
		// if (realPolicy != null) {
		// return realPolicy.getPermissions(codesource);
		// }
		// // TODO Auto-generated method stub
		return defaultPolicy.getPermissions(codesource);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.security.Policy#refresh()
	 */
	@Override
	public void refresh() {
		System.out.println("defaultPolicy: refresh"); //$NON-NLS-1$
		// if (realPolicy != null) {
		// realPolicy.refresh();
		// }
		defaultPolicy.refresh();
	}

	@Override
	public PermissionCollection getPermissions(ProtectionDomain domain) {
		System.out.println("defaultPolicy: getPermissions"); //$NON-NLS-1$
		// if (realPolicy != null) {
		// return realPolicy.getPermissions(domain);
		// }
		return defaultPolicy.getPermissions(domain);
	}

	@Override
	public boolean implies(ProtectionDomain domain, Permission permission) {
		System.out.print("(X)"); //$NON-NLS-1$
		// if (realPolicy != null) {
		// return realPolicy.implies(domain, permission);
		// }
		boolean perm = defaultPolicy.implies(domain, permission);
		if (!perm) {
			if (permission.getClass().getName().contains("ApplicationAdminPermission")) { //$NON-NLS-1$
				System.err.println("allow ApplicationAdminPermission"); //$NON-NLS-1$
				return true;
			}
			System.out.println("not allowed to " + permission); //$NON-NLS-1$
		}
		return perm;
		// return true; // TODO return true while there is no REAL POLICY
		// return super.implies(domain, permission);
	}

}
