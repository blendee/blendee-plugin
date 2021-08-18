package org.blendee.plugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.regex.Pattern;

import org.blendee.assist.Query;
import org.blendee.codegen.CodeFormatter;
import org.blendee.internal.U;
import org.blendee.jdbc.MetadataFactory;
import org.blendee.jdbc.OptionKey;
import org.blendee.jdbc.TransactionFactory;
import org.blendee.plugin.views.ClassBuilderView;
import org.blendee.util.BlendeeConstants;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class BlendeePlugin extends AbstractUIPlugin {

	private static final String pluginId = "org.blendee.plugin";

	private static final String currentProjectKeyName = "project-name";

	private static final Pattern typeSignaturePattern = Pattern.compile("[Q|L]([^;]+);");

	private static final Pattern queryClassPattern = Pattern.compile(Query.class.getSimpleName());

	private static BlendeePlugin plugin;

	private IJavaProject currentProject;

	private ClassBuilderView classBuilderView;

	private String[] schemaNames = {};

	private String outputPackage;

	private Class<?> tableFacadeParentClass;

	private Class<?> rowParentClass;

	private CodeFormatter codeFormatter;

	private boolean useNumberClass;

	private boolean notUseNullGuard;

	public static BlendeePlugin getDefault() {
		return plugin;
	}

	public static void checkRequiredClass(
		boolean required,
		IJavaProject project,
		Class<?> superInterface,
		String className)
		throws JavaProjectException {
		if (className == null || className.length() == 0) {
			if (!required) return;
			//superInterface.getName() + " の実装クラス名が空です"
			throw new JavaProjectException("Subclass of " + superInterface.getName() + " is empty.");
		}

		try {
			var target = project.findType(className);
			if (target == null) {
				//存在するクラスを指定する必要があります
				throw new JavaProjectException("Specify an existing class.");
			}
			IType factoryType = project.findType(superInterface.getName());
			if (factoryType == null) {
				//"プロジェクト内に " + superInterface.getName() + " が見つかりません"
				throw new JavaProjectException(superInterface.getName() + " not found in project.");
			}

			if (target.isInterface()) {
				//インターフェイスは指定できません
				throw new JavaProjectException("Interface can not be specified.");
			}

			if (Flags.isAbstract(target.getFlags())) {
				//抽象クラスは指定できません
				throw new JavaProjectException("Abstract class can not be specified.");
			}

			String superclass;
			while (true) {
				var types = target.getSuperInterfaceNames();
				for (var type : types) {
					var resolved = resolveType(project, target, type);
					for (var itype : resolved)
						if (factoryType.equals(itype)) return;
				}

				superclass = target.getSuperclassName();
				if (superclass == null) break;

				var resolved = resolveType(project, target, superclass);
				if (resolved.length == 0)
					//target.getFullyQualifiedName() + " が見つかりません"
					throw new JavaProjectException(target.getFullyQualifiedName() + " not found");

				target = resolved[0];
			}

			throw new JavaProjectException(
				//"指定されたクラスは " + superInterface.getName() + " を実装していません"
				"The specified class does not implement " + superInterface.getName() + ".");
		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new JavaProjectException(e);
		}
	}

	public BlendeePlugin() {
		super();
		plugin = this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		var projectName = InstanceScope.INSTANCE.getNode(pluginId).get(currentProjectKeyName, null);
		if (projectName == null || projectName.equals("")) return;
		var project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
			try {
				setProject((IJavaProject) project.getNature(JavaCore.NATURE_ID));
			} catch (Throwable t) {
				t = strip(t);
				t.printStackTrace();
				currentProject = null;
				MessageDialog.openError(null, Constants.TITLE, cause(t).getMessage());
			}
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (currentProject != null) {
			InstanceScope.INSTANCE.getNode(pluginId)
				.put(
					currentProjectKeyName,
					currentProject.getElementName());
		}

		super.stop(context);
	}

	public IJavaProject getProject() {
		return currentProject;
	}

	public void setProject(IJavaProject project) throws JavaProjectException {
		if (currentProject != null && currentProject.equals(project)) return;
		setProjectAndRefresh(project);
	}

	public void setProjectAndRefresh(IJavaProject project) throws JavaProjectException {
		checkProject(project);
		currentProject = project;
		refresh();
		if (classBuilderView != null) classBuilderView.reset();
	}

	public Class<?> getTableFacadeParentClass() {
		return tableFacadeParentClass;
	}

	public Class<?> getRowParentClass() {
		return rowParentClass;
	}

	public CodeFormatter getCodeFormatter() {
		return codeFormatter;
	}

	public boolean useNumberClass() {
		return useNumberClass;
	}

	public boolean notUseNullGuard() {
		return notUseNullGuard;
	}

	public void storePersistentProperties(
		IJavaProject project,
		Properties properties)
		throws JavaProjectException {
		currentProject = project;
		try (var output = new BufferedOutputStream(
			new FileOutputStream(getPropertiesFile(project)))) {
			properties.store(output, "");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		refresh();
	}

	public static final class JavaProjectException extends Exception {

		private static final long serialVersionUID = 1825520626876129705L;

		private JavaProjectException(String message) {
			super(message);
		}

		private JavaProjectException(Exception e) {
			super(e);
			e.printStackTrace();
		}
	}

	public static Properties getPersistentProperties(IJavaProject project) {
		var properties = new Properties();
		try {
			var input = new BufferedInputStream(
				new FileInputStream(getPropertiesFile(project)));
			try {
				properties.load(input);
			} finally {
				input.close();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return properties;
	}

	public static boolean save(
		IJavaProject project,
		String key,
		String value) {
		try {
			project.getProject().setPersistentProperty(new QualifiedName("", key), value);
		} catch (CoreException e) {
			return false;
		}

		return true;
	}

	public static String load(IJavaProject project, String key) {
		try {
			var value = project.getProject().getPersistentProperty(new QualifiedName("", key));
			return value == null ? "" : value;
		} catch (CoreException e) {
			return "";
		}
	}

	public void setSchemaNames(String[] names) {
		schemaNames = names;
	}

	public String[] getSchemaNames() {
		return schemaNames;
	}

	public ClassBuilderView getClassBuilderView() {
		return classBuilderView;
	}

	public void setClassBuilderView(ClassBuilderView view) {
		this.classBuilderView = view;
	}

	public String getOutputPackage() {
		return outputPackage;
	}

	public static String regularizeColumnRepositoryFilePath(IJavaProject project, String path) {
		return path.substring(project.getProject().getLocation().toFile().getAbsolutePath().length() + 1);
	}

	public static String generalizeColumnRepositoryFilePath(IJavaProject project, String path) {
		return project.getProject().getLocation().toFile().getAbsolutePath() + U.FILE_SEPARATOR + path;
	}

	public static String[] splitByBlankAndRemoveEmptyString(String target) {
		var regex = " +";
		var values = (target == null ? "" : target).split(regex);
		var list = new LinkedList<String>();
		for (var value : values) {
			if (U.presents(value)) list.add(value);
		}

		return list.toArray(new String[list.size()]);
	}

	public void refresh() throws JavaProjectException {
		var init = new HashMap<OptionKey<?>, Object>();

		var properties = getPersistentProperties(currentProject);

		outputPackage = properties.getProperty(Constants.OUTPUT_PACKAGE_NAME);

		init.put(BlendeeConstants.TABLE_FACADE_PACKAGE, outputPackage.trim());

		var schemaNameArray = splitByBlankAndRemoveEmptyString(
			properties.getProperty(Constants.SCHEMA_NAMES));

		setSchemaNames(schemaNameArray);

		init.put(BlendeeConstants.SCHEMA_NAMES, schemaNameArray);

		JavaProjectClassLoader loader;
		try {
			loader = new JavaProjectClassLoader(
				getClass().getClassLoader(),
				currentProject);
		} catch (JavaModelException e) {
			throw new JavaProjectException(e);
		}

		var jdbcDriverClass = properties.getProperty(Constants.JDBC_DRIVER_CLASS);
		if (U.presents(jdbcDriverClass)) {
			init.put(BlendeeConstants.JDBC_DRIVER_CLASS_NAME, jdbcDriverClass);
		}

		var jdbcURL = BlendeePlugin.load(currentProject, Constants.JDBC_URL);
		if (U.presents(jdbcURL)) {
			init.put(BlendeeConstants.JDBC_URL, jdbcURL);
			init.put(BlendeeConstants.JDBC_USER, BlendeePlugin.load(currentProject, Constants.JDBC_USER));
			init.put(BlendeeConstants.JDBC_PASSWORD, BlendeePlugin.load(currentProject, Constants.JDBC_PASSWORD));
		}

		try {
			{
				var classString = properties.getProperty(Constants.TRANSACTION_FACTORY_CLASS);
				if (U.presents(classString)) {
					init.put(BlendeeConstants.TRANSACTION_FACTORY_CLASS, Class.forName(classString, false, loader));
				} else {
					PluginDriverTransactionFactory.setClassLoader(loader);
					init.put(BlendeeConstants.TRANSACTION_FACTORY_CLASS, PluginDriverTransactionFactory.class);
				}
			}

			{
				var classString = properties.getProperty(Constants.METADATA_FACTORY_CLASS);
				if (U.presents(classString)) {
					init.put(BlendeeConstants.METADATA_FACTORY_CLASS, Class.forName(classString, false, loader));
				} else {
					PluginAnnotationMetadataFactory.setClassLoader(loader);
					init.put(BlendeeConstants.METADATA_FACTORY_CLASS, PluginAnnotationMetadataFactory.class);
				}
			}
		} catch (ClassNotFoundException e) {
			throw new JavaProjectException(e);
		}

		init.put(BlendeeConstants.USE_METADATA_CACHE, true);

		try {
			BlendeeStarter.start(loader, init);

			var tableFacadeParentClassName = properties.getProperty(Constants.TABLE_FACADE_PARENT_CLASS);
			if (U.presents(tableFacadeParentClassName)) {
				tableFacadeParentClass = Class.forName(tableFacadeParentClassName, false, loader);
			} else {
				tableFacadeParentClass = null;
			}

			var rowParentClassName = properties.getProperty(Constants.ROW_PARENT_CLASS);
			if (U.presents(rowParentClassName)) {
				rowParentClass = Class.forName(rowParentClassName, false, loader);
			} else {
				rowParentClass = null;
			}

			var codeFormatterClassName = properties.getProperty(Constants.CODE_FORMATTER_CLASS);
			if (U.presents(codeFormatterClassName)) {
				var pluginLoader = new JavaProjectClassLoader(
					getClass().getClassLoader(),
					currentProject);
				codeFormatter = (CodeFormatter) Class.forName(codeFormatterClassName, false, pluginLoader).getDeclaredConstructor().newInstance();
			} else {
				codeFormatter = null;
			}

			var useNumberClassString = properties.getProperty(Constants.USE_NUMBER_CLASS);
			if (U.presents(useNumberClassString)) {
				useNumberClass = Boolean.parseBoolean(useNumberClassString);
			} else {
				useNumberClass = false;
			}

			var notUseNullGuardString = properties.getProperty(Constants.NOT_USE_NULL_GUARD);
			if (U.presents(notUseNullGuardString)) {
				notUseNullGuard = Boolean.parseBoolean(notUseNullGuardString);
			} else {
				notUseNullGuard = false;
			}
		} catch (Exception e) {
			throw new JavaProjectException(e);
		}
	}

	public static IPackageFragment findPackage(String packageName) {
		var packagePath = packageName.replace('.', '/');
		try {
			var element = BlendeePlugin.getDefault().getProject().findElement(new Path(packagePath));
			if (element instanceof IPackageFragment) return (IPackageFragment) element;
		} catch (JavaModelException e) {
			throw new IllegalStateException(e);
		}

		return null;
	}

	public static void refreshOutputPackage() {
		var plugin = BlendeePlugin.getDefault();

		var packageName = plugin.getOutputPackage();

		var baseFragment = findPackage(packageName);

		try {
			baseFragment.getResource().refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Throwable cause(Throwable t) {
		var cause = t.getCause();
		if (cause == null) return t;
		return cause(cause);
	}

	private static void checkProject(IJavaProject project) throws JavaProjectException {
		if (project == null) return;
		var properties = getPersistentProperties(project);
		checkRequiredClass(
			false,
			project,
			TransactionFactory.class,
			properties.getProperty(Constants.TRANSACTION_FACTORY_CLASS));
		checkRequiredClass(
			false,
			project,
			MetadataFactory.class,
			properties.getProperty(Constants.METADATA_FACTORY_CLASS));
	}

	public static IType findFiledType(IField field) throws JavaModelException {
		var matcher = typeSignaturePattern.matcher(field.getTypeSignature());
		matcher.matches();
		var resolved = field.getDeclaringType().resolveType(matcher.group(1));

		return field.getJavaProject().findType(resolved[0][0], resolved[0][1]);
	}

	public static boolean checkQueryClass(IType type) throws JavaModelException {
		var interfaces = type.getSuperInterfaceNames();
		for (var name : interfaces) {
			if (queryClassPattern.matcher(name).matches()) return true;
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Throwable> T strip(Throwable t) {
		var cause = t.getCause();
		if (cause == null) return (T) t;

		return strip(cause);
	}

	private static File getPropertiesFile(IJavaProject project) throws IOException {
		var file = new File(project.getProject().getLocation().toFile(), ".blendee.plugin");
		if (!file.exists()) {
			file.createNewFile();

			try (var output = new BufferedOutputStream(new FileOutputStream(file))) {
				new Properties().store(output, "");
			}
		}

		return file;
	}

	private static IType[] resolveType(
		IJavaProject project,
		IType base,
		String className)
		throws JavaModelException {
		if (base.isBinary()) {
			return new IType[] { project.findType(className) };
		}

		var resolved = base.resolveType(className);
		if (resolved == null) return new IType[0];
		var types = new LinkedList<IType>();
		for (var element : resolved)
			types.add(project.findType(String.join(".", element)));

		return types.toArray(new IType[types.size()]);
	}
}
