package org.blendee.plugin.popup.actions;

import org.blendee.internal.U;
import org.blendee.plugin.BlendeePlugin;
import org.blendee.plugin.Constants;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class ChangeProjectHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String id = event.getCommand().getId();
		if ("org.blendee.plugin.restartGenerator".equals(id)) {
			Starter.start("org.blendee.plugin.classBuilderView");
		} else {
			throw new IllegalStateException(id);
		}

		ISelection selection = HandlerUtil.getActiveMenuSelection(event);

		if (selection == null) return null;

		IStructuredSelection structured = (IStructuredSelection) selection;
		Object element = structured.getFirstElement();
		if (element == null) return null;

		if (!(element instanceof IJavaProject)) return null;

		IJavaProject project = (IJavaProject) element;

		try {
			BlendeePlugin.getDefault().setProjectAndRefresh(project);
		} catch (Throwable t) {
			t = BlendeePlugin.strip(t);
			t.printStackTrace();
			MessageDialog.openError(
				null,
				Constants.TITLE,
				//設定に問題があります
				"An unexpected error has occurred." + U.LINE_SEPARATOR + t.getMessage());
		}

		return null;
	}
}
