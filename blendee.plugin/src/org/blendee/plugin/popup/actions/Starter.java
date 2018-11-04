package org.blendee.plugin.popup.actions;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

class Starter {

	static void start(String viewId) {
		IWorkbenchPage page = PlatformUI.getWorkbench()
			.getActiveWorkbenchWindow()
			.getActivePage();
		try {
			page.showView(viewId);
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}
	}
}
