package org.blendee.plugin.properties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Driver;
import java.util.Properties;

import org.blendee.develop.CodeFormatter;
import org.blendee.internal.U;
import org.blendee.jdbc.MetadataFactory;
import org.blendee.jdbc.TransactionFactory;
import org.blendee.plugin.BlendeePlugin;
import org.blendee.plugin.BlendeePlugin.JavaProjectException;
import org.blendee.plugin.Constants;
import org.blendee.selector.ColumnRepositoryFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPropertyPage;

public class BlendeePropertyPage
	extends FieldEditorPreferencePage
	implements IWorkbenchPropertyPage {

	private IJavaProject element;

	public BlendeePropertyPage() {
		super(GRID);
		setDescription(Constants.TITLE + " 設定");
	}

	@Override
	public IAdaptable getElement() {
		return element;
	}

	@Override
	public void setElement(IAdaptable element) {
		if (element instanceof IProject) {
			try {
				this.element = (IJavaProject) ((IProject) element).getNature(JavaCore.NATURE_ID);
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
		} else {
			this.element = (IJavaProject) element;
		}

		setPreferenceStore(createPreferenceStore());
		initializeDefaults();
	}

	@Override
	public void createFieldEditors() {
		final StringFieldEditor[] packagesEditorContainer = { null };

		/*---------------------------------------------------*/
		final StringFieldEditor schemaNamesEditor = new StringFieldEditor(
			Constants.SCHEMA_NAMES,
			"スキーマ名 (スペースで区切って複数入力可)",
			getFieldEditorParent()) {};

		schemaNamesEditor.setEmptyStringAllowed(true);
		addField(schemaNamesEditor);

		/*---------------------------------------------------*/
		StringFieldEditor packagesEditor = new StringFieldEditor(
			Constants.OUTPUT_PACKAGE_NAME,
			"生成する Data Object の出力パッケージ名",
			getFieldEditorParent()) {};

		packagesEditorContainer[0] = packagesEditor;
		packagesEditor.setValidateStrategy(StringFieldEditor.VALIDATE_ON_FOCUS_LOST);
		packagesEditor.setEmptyStringAllowed(true);
		addField(packagesEditor);

		/*---------------------------------------------------*/
		ClassFieldEditor jdbcDriverClassEditor = new ClassFieldEditor(
			Constants.JDBC_DRIVER_CLASS,
			"JDBC Driver クラス",
			element.getProject(),
			getFieldEditorParent()) {

			@Override
			protected boolean doCheckState() {
				if (!U.presents(getStringValue())) return true;

				try {
					BlendeePlugin.checkRequiredClass(
						true,
						element,
						Driver.class,
						getStringValue());
				} catch (JavaProjectException e) {
					setErrorMessage(e.getMessage());
					return false;
				}

				return true;
			}
		};

		jdbcDriverClassEditor.setChangeButtonText("参照...");
		jdbcDriverClassEditor.setEmptyStringAllowed(true);
		addField(jdbcDriverClassEditor);

		/*---------------------------------------------------*/
		StringFieldEditor jdbcURLEditor = new StringFieldEditor(
			Constants.JDBC_URL,
			"JDBC URL",
			getFieldEditorParent());

		jdbcURLEditor.setValidateStrategy(StringFieldEditor.VALIDATE_ON_FOCUS_LOST);
		jdbcURLEditor.setEmptyStringAllowed(true);
		addField(jdbcURLEditor);

		/*---------------------------------------------------*/
		StringFieldEditor jdbcUserEditor = new StringFieldEditor(
			Constants.JDBC_USER,
			"JDBC ユーザー名",
			getFieldEditorParent());

		jdbcUserEditor.setValidateStrategy(StringFieldEditor.VALIDATE_ON_FOCUS_LOST);
		jdbcUserEditor.setEmptyStringAllowed(true);
		addField(jdbcUserEditor);

		/*---------------------------------------------------*/
		StringFieldEditor jdbcPasswordEditor = new StringFieldEditor(
			Constants.JDBC_PASSWORD,
			"JDBC パスワード",
			getFieldEditorParent());

		jdbcPasswordEditor.setValidateStrategy(StringFieldEditor.VALIDATE_ON_FOCUS_LOST);
		jdbcPasswordEditor.setEmptyStringAllowed(true);
		addField(jdbcPasswordEditor);

		/*---------------------------------------------------*/
		File root = element.getProject().getLocation().toFile();
		FileFieldEditor columnRepositoryFileEditor = new FileFieldEditor(
			Constants.COLUMN_REPOSITORY_FILE,
			"ColumnRepository ファイル",
			false,
			StringFieldEditor.VALIDATE_ON_FOCUS_LOST,
			getFieldEditorParent()) {

			@Override
			protected String changePressed() {
				String path = super.changePressed();
				if (!U.presents(path)) return path;
				return BlendeePlugin.regularizeColumnRepositoryFilePath(element, path);
			}

			@Override
			protected boolean checkState() {
				clearErrorMessage();

				String path = getTextControl().getText();
				if (path != null) {
					path = path.trim();
				} else {
					path = "";
				}

				if (path.length() == 0) {
					if (!isEmptyStringAllowed()) {
						return true;
					}
				}

				boolean result = new File(
					BlendeePlugin.generalizeColumnRepositoryFilePath(element, path)).exists();

				if (!result) showErrorMessage("ファイルが存在しません");

				return result;
			}
		};

		columnRepositoryFileEditor.setFilterPath(root);
		columnRepositoryFileEditor.setChangeButtonText("参照...");
		columnRepositoryFileEditor.setValidateStrategy(StringFieldEditor.VALIDATE_ON_FOCUS_LOST);
		columnRepositoryFileEditor.setEmptyStringAllowed(true);
		addField(columnRepositoryFileEditor);

		/*---------------------------------------------------*/
		Label label = new Label(getFieldEditorParent(), SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));

		/*---------------------------------------------------*/
		ClassFieldEditor columnRepositoryClassEditor = new ClassFieldEditor(
			Constants.COLUMN_REPOSITORY_FACTORY_CLASS,
			"&ColumnRepositoryFactory 実装クラス",
			element.getProject(),
			getFieldEditorParent()) {

			@Override
			protected boolean doCheckState() {
				if (!U.presents(getStringValue())) return true;

				try {
					BlendeePlugin.checkRequiredClass(
						true,
						element,
						ColumnRepositoryFactory.class,
						getStringValue());
				} catch (JavaProjectException e) {
					setErrorMessage(e.getMessage());
					return false;
				}

				return true;
			}
		};

		columnRepositoryClassEditor.setChangeButtonText("参照...");
		columnRepositoryClassEditor.setEmptyStringAllowed(true);
		addField(columnRepositoryClassEditor);

		/*---------------------------------------------------*/
		ClassFieldEditor transactionFactoryClassEditor = new ClassFieldEditor(
			Constants.TRANSACTION_FACTORY_CLASS,
			"TransactionFactory 実装クラス",
			element.getProject(),
			getFieldEditorParent()) {

			@Override
			protected boolean doCheckState() {
				if (!U.presents(getStringValue())) return true;

				try {
					BlendeePlugin.checkRequiredClass(
						true,
						element,
						TransactionFactory.class,
						getStringValue());
				} catch (JavaProjectException e) {
					setErrorMessage(e.getMessage());
					return false;
				}

				return true;
			}
		};

		transactionFactoryClassEditor.setChangeButtonText("参照...");
		transactionFactoryClassEditor.setEmptyStringAllowed(true);
		addField(transactionFactoryClassEditor);

		/*---------------------------------------------------*/
		ClassFieldEditor metadataFactoryClassEditor = new ClassFieldEditor(
			Constants.METADATA_FACTORY_CLASS,
			"MetadataFactory 実装クラス",
			element.getProject(),
			getFieldEditorParent()) {

			@Override
			protected boolean doCheckState() {
				if (!U.presents(getStringValue())) return true;

				try {
					BlendeePlugin.checkRequiredClass(
						false,
						element,
						MetadataFactory.class,
						getStringValue());
				} catch (JavaProjectException e) {
					setErrorMessage(e.getMessage());
					return false;
				}

				return true;
			}
		};

		metadataFactoryClassEditor.setChangeButtonText("参照...");
		metadataFactoryClassEditor.setEmptyStringAllowed(true);
		addField(metadataFactoryClassEditor);

		/*---------------------------------------------------*/
		ClassFieldEditor managerParentClassEditor = new ClassFieldEditor(
			Constants.TABLE_FACADE_PARENT_CLASS,
			"自動生成 TableFacade の親クラス",
			element.getProject(),
			getFieldEditorParent()) {

			@Override
			protected boolean doCheckState() {
				String typeName = getStringValue();
				if (U.presents(typeName) && findType(typeName) == null) {
					setErrorMessage("プロジェクト内にクラス " + typeName + " が見つかりません");
					return false;
				}

				return true;
			}
		};

		managerParentClassEditor.setChangeButtonText("参照...");
		managerParentClassEditor.setEmptyStringAllowed(true);
		addField(managerParentClassEditor);

		/*---------------------------------------------------*/
		ClassFieldEditor rowParentClassEditor = new ClassFieldEditor(
			Constants.ROW_PARENT_CLASS,
			"自動生成 Row の親クラス",
			element.getProject(),
			getFieldEditorParent()) {

			@Override
			protected boolean doCheckState() {
				String typeName = getStringValue();
				if (U.presents(typeName) && findType(typeName) == null) {
					setErrorMessage("プロジェクト内にクラス " + typeName + " が見つかりません");
					return false;
				}

				return true;
			}
		};

		rowParentClassEditor.setChangeButtonText("参照...");
		rowParentClassEditor.setEmptyStringAllowed(true);
		addField(rowParentClassEditor);

		/*---------------------------------------------------*/
		ClassFieldEditor codeFormatterClassEditor = new ClassFieldEditor(
			Constants.CODE_FORMATTER_CLASS,
			"CodeFormatter 実装クラス",
			element.getProject(),
			getFieldEditorParent()) {

			@Override
			protected boolean doCheckState() {
				if (!U.presents(getStringValue())) return true;

				try {
					BlendeePlugin.checkRequiredClass(
						false,
						element,
						CodeFormatter.class,
						getStringValue());
				} catch (JavaProjectException e) {
					setErrorMessage(e.getMessage());
					return false;
				}

				return true;
			}
		};

		codeFormatterClassEditor.setChangeButtonText("参照...");
		codeFormatterClassEditor.setEmptyStringAllowed(true);
		addField(codeFormatterClassEditor);

		/*---------------------------------------------------*/
		BooleanFieldEditor useNumberClassEditor = new BooleanFieldEditor(
			Constants.USE_NUMBER_CLASS,
			"Row の数値項目を Number に統一",
			BooleanFieldEditor.SEPARATE_LABEL,
			getFieldEditorParent());
		addField(useNumberClassEditor);

		/*---------------------------------------------------*/
		BooleanFieldEditor notUseNullGuardEditor = new BooleanFieldEditor(
			Constants.NOT_USE_NULL_GUARD,
			"Row の項目で null 保護機能を使用しない",
			BooleanFieldEditor.SEPARATE_LABEL,
			getFieldEditorParent());
		addField(notUseNullGuardEditor);
	}

	@Override
	public boolean performOk() {
		boolean ok = super.performOk();
		IPreferenceStore store = getPreferenceStore();

		boolean changed = false;

		Properties properties = BlendeePlugin.getPersistentProperties(element);

		changed |= checkAndSetValue(store, properties, Constants.SCHEMA_NAMES);

		BlendeePlugin.getDefault().setSchemaNames(
			BlendeePlugin.splitByBlankAndRemoveEmptyString(
				properties.getProperty(Constants.SCHEMA_NAMES)));

		changed |= checkAndSetValue(store, properties, Constants.OUTPUT_PACKAGE_NAME);

		changed |= checkAndSetValue(store, properties, Constants.JDBC_DRIVER_CLASS);

		BlendeePlugin.save(element, Constants.JDBC_URL, store.getString(Constants.JDBC_URL));

		BlendeePlugin.save(element, Constants.JDBC_USER, store.getString(Constants.JDBC_USER));

		BlendeePlugin.save(element, Constants.JDBC_PASSWORD, store.getString(Constants.JDBC_PASSWORD));

		changed |= checkAndSetValue(store, properties, Constants.COLUMN_REPOSITORY_FILE);

		BlendeePlugin.storeColumnRepositoryFileToHome(
			element.getElementName(),
			BlendeePlugin.generalizeColumnRepositoryFilePath(
				element,
				properties.getProperty(Constants.COLUMN_REPOSITORY_FILE, "")));

		changed |= checkAndSetValue(store, properties, Constants.COLUMN_REPOSITORY_FACTORY_CLASS);

		changed |= checkAndSetValue(store, properties, Constants.TRANSACTION_FACTORY_CLASS);

		changed |= checkAndSetValue(store, properties, Constants.METADATA_FACTORY_CLASS);

		changed |= checkAndSetValue(store, properties, Constants.TABLE_FACADE_PARENT_CLASS);

		changed |= checkAndSetValue(store, properties, Constants.ROW_PARENT_CLASS);

		changed |= checkAndSetValue(store, properties, Constants.QUERY_PARENT_CLASS);

		changed |= checkAndSetValue(store, properties, Constants.CODE_FORMATTER_CLASS);

		changed |= checkAndSetValue(store, properties, Constants.USE_NUMBER_CLASS);

		changed |= checkAndSetValue(store, properties, Constants.NOT_USE_NULL_GUARD);

		initializeDefaults();

		try {
			if (changed) BlendeePlugin.getDefault().storePersistentProperties(element, properties);
		} catch (Throwable t) {
			t = BlendeePlugin.strip(t);
			t.printStackTrace();
			MessageDialog.openError(null, Constants.TITLE, t.getMessage());
			return false;
		}

		return ok;
	}

	private static boolean checkAndSetValue(
		IPreferenceStore store,
		Properties properties,
		String key) {
		String oldValue = properties.getProperty(key);
		String newValue = store.getString(key);
		if (newValue.equals(oldValue)) return false;
		properties.setProperty(key, newValue);
		return true;
	}

	private IType findType(String typeName) {
		try {
			IType type = element.findType(typeName);
			if (type == null
				|| type.isInterface()) return null;

			return type;
		} catch (JavaModelException e) {
			throw new IllegalStateException(e);
		}
	}

	private void initializeDefaults() {
		IPreferenceStore store = getPreferenceStore();

		store.setDefault(
			Constants.SCHEMA_NAMES,
			store.getString(Constants.SCHEMA_NAMES));

		store.setDefault(
			Constants.OUTPUT_PACKAGE_NAME,
			store.getString(Constants.OUTPUT_PACKAGE_NAME));

		store.setDefault(
			Constants.JDBC_DRIVER_CLASS,
			store.getString(Constants.JDBC_DRIVER_CLASS));

		store.setDefault(
			Constants.JDBC_URL,
			BlendeePlugin.load(element, Constants.JDBC_URL));

		store.setDefault(
			Constants.JDBC_USER,
			BlendeePlugin.load(element, Constants.JDBC_USER));

		store.setDefault(
			Constants.JDBC_PASSWORD,
			BlendeePlugin.load(element, Constants.JDBC_PASSWORD));

		store.setDefault(
			Constants.COLUMN_REPOSITORY_FILE,
			store.getString(Constants.COLUMN_REPOSITORY_FILE));

		store.setDefault(
			Constants.COLUMN_REPOSITORY_FACTORY_CLASS,
			store.getString(Constants.COLUMN_REPOSITORY_FACTORY_CLASS));

		store.setDefault(
			Constants.TRANSACTION_FACTORY_CLASS,
			store.getString(Constants.TRANSACTION_FACTORY_CLASS));

		store.setDefault(
			Constants.METADATA_FACTORY_CLASS,
			store.getString(Constants.METADATA_FACTORY_CLASS));

		store.setDefault(
			Constants.TABLE_FACADE_PARENT_CLASS,
			store.getString(Constants.TABLE_FACADE_PARENT_CLASS));

		store.setDefault(
			Constants.ROW_PARENT_CLASS,
			store.getString(Constants.ROW_PARENT_CLASS));

		store.setDefault(
			Constants.QUERY_PARENT_CLASS,
			store.getString(Constants.QUERY_PARENT_CLASS));

		store.setDefault(
			Constants.CODE_FORMATTER_CLASS,
			store.getString(Constants.CODE_FORMATTER_CLASS));

		store.setDefault(
			Constants.USE_NUMBER_CLASS,
			store.getString(Constants.USE_NUMBER_CLASS));

		store.setDefault(
			Constants.NOT_USE_NULL_GUARD,
			store.getString(Constants.NOT_USE_NULL_GUARD));
	}

	private PreferenceStore createPreferenceStore() {
		Properties properties = BlendeePlugin.getPersistentProperties(element);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PreferenceStore store = new PreferenceStore() {

			@Override
			public void save() {}
		};

		try {
			properties.store(out, null);
			store.load(new ByteArrayInputStream(out.toByteArray()));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return store;
	}
}
