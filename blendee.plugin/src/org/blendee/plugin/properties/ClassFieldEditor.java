package org.blendee.plugin.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionDialog;

class ClassFieldEditor extends StringButtonFieldEditor {

	private final IProject project;

	ClassFieldEditor(
		String name,
		String labelText,
		IProject project,
		Composite parent) {
		this.project = project;
		init(name, labelText);
		setChangeButtonText(JFaceResources.getString("openBrowse"));
		setValidateStrategy(VALIDATE_ON_FOCUS_LOST);
		createControl(parent);
	}

	@Override
	protected String changePressed() {
		String fqcn = getTextControl().getText();
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(
			new IJavaProject[] { JavaCore.create(project) });
		SelectionDialog dialog;
		Shell shell = getShell();
		try {
			dialog = JavaUI.createTypeDialog(
				shell,
				new ProgressMonitorDialog(shell),
				scope,
				IJavaElementSearchConstants.CONSIDER_CLASSES,
				false,
				fqcn);
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
		dialog.open();
		Object[] results = dialog.getResult();
		if (results == null) return null;
		return ((IType) results[0]).getFullyQualifiedName('.');
	}
}
