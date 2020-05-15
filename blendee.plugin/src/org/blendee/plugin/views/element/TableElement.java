package org.blendee.plugin.views.element;

import java.nio.file.Path;

import org.blendee.assist.TableFacadePackageRule;
import org.blendee.codegen.TableFacadeGenerator;
import org.blendee.codegen.TableFacadeGeneratorHandler;
import org.blendee.jdbc.BlendeeManager;
import org.blendee.jdbc.TablePath;
import org.blendee.plugin.BlendeePlugin;
import org.blendee.plugin.Constants;
import org.blendee.plugin.PluginTableFacadeGenerator;
import org.blendee.util.Blendee;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.swt.graphics.Image;

public class TableElement extends PropertySourceElement {

	private static final Image builtIcon = Constants.TABLE_ICON.createImage();

	private static final Image unbuiltIcon = Constants.UNBUILT_TABLE_ICON.createImage();

	private static final TableAction action = new TableAction();

	private final SchemaElement parent;

	private final TablePath path;

	TableElement(SchemaElement parent, TablePath path) {
		this.parent = parent;
		this.path = path;
	}

	@Override
	public int getCategory() {
		return 0;
	}

	@Override
	public String getName() {
		return path.getTableName();
	}

	@Override
	public String getPath() {
		return path.toString();
	}

	@Override
	public Image getIcon() {
		if (isAvailable()) return builtIcon;
		return unbuiltIcon;
	}

	@Override
	public Element getParent() {
		return parent;
	}

	@Override
	public Element[] getChildren() {
		return EMPTY_ARRAY;
	}

	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	public void doubleClick() {
		try {
			BlendeePlugin.getDefault().refresh();
			Blendee.execute(t -> {
				build();
			});
		} catch (Throwable t) {
			throw new IllegalStateException(t);
		} finally {
			BlendeePlugin.refreshOutputPackage();
		}
	}

	@Override
	public void addActionToContextMenu(IMenuManager manager) {
		if (isAvailable()) return;
		action.element = this;
		manager.add(action);
	}

	@Override
	String getType() {
		return "Table";
	}

	void build() throws Exception {
		BlendeePlugin plugin = BlendeePlugin.getDefault();

		String packageName = plugin.getOutputPackage();

		IPackageFragment baseFragment = BlendeePlugin.findPackage(packageName);
		if (baseFragment == null)
			//"パッケージ " + packageName + " が存在しません"
			throw new IllegalStateException("Package " + packageName + " not found");

		IPackageFragmentRoot fragmentRoot = findPackageRoot(baseFragment);

		TableFacadeGenerator generator = new PluginTableFacadeGenerator(
			BlendeeManager.get().getMetadata(),
			baseFragment.getElementName(),
			plugin.getTableFacadeParentClass(),
			plugin.getRowParentClass(),
			plugin.getCodeFormatter(),
			plugin.useNumberClass(),
			!plugin.notUseNullGuard());

		CodeFormatter formatter = ToolFactory.createCodeFormatter(
			BlendeePlugin.getDefault().getProject().getOptions(true));

		PluginTableFacadeGeneratorHandler handler = new PluginTableFacadeGeneratorHandler(packageName, fragmentRoot, formatter);

		//自身をセット
		handler.add(path);

		handler.execute(generator);
	}

	private class PluginTableFacadeGeneratorHandler extends TableFacadeGeneratorHandler {

		private final String packageName;

		private final IPackageFragmentRoot fragmentRoot;

		private final CodeFormatter formatter;

		private TablePath current;

		private IPackageFragment schemaPackage;

		private ICompilationUnit currentUnit;

		private PluginTableFacadeGeneratorHandler(
			String packageName,
			IPackageFragmentRoot fragmentRoot,
			CodeFormatter formatter) {
			this.packageName = packageName;
			this.fragmentRoot = fragmentRoot;
			this.formatter = formatter;
		}

		@Override
		protected boolean exists(TablePath path) {
			return isAvailable(path);
		}

		@Override
		protected void start(TablePath path) {
			current = path;
			schemaPackage = getPackage(fragmentRoot, packageName + "." + TableFacadePackageRule.care(path.getSchemaName()));
			currentUnit = schemaPackage.getCompilationUnit(TableFacadeGenerator.createCompilationUnitName(path.getTableName()));
		}

		@Override
		protected boolean exists() {
			return currentUnit.exists();
		}

		@Override
		protected void infoSkip() {
		}

		@Override
		protected String format(String source) {
			return TableElement.format(formatter, source);
		}

		@Override
		protected String loadSource() {
			try {
				return currentUnit.getSource();
			} catch (JavaModelException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		protected void writeSource(String source) {
			try {
				schemaPackage.createCompilationUnit(currentUnit.getElementName(), source, true, null);
			} catch (JavaModelException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		protected void end() {
			parent.refresh(current);
			Thread.yield();

			current = null;
			schemaPackage = null;
			currentUnit = null;
		}

		@Override
		protected Path getOutputRoot() {
			return fragmentRoot.getResource().getLocation().toFile().toPath();
		}
	}

	boolean isAvailable() {
		return isAvailable(path);
	}

	private IPackageFragmentRoot findPackageRoot(IPackageFragment fragment) {
		IJavaElement e;
		while (!((e = fragment.getParent()) instanceof IPackageFragmentRoot)) {
			return findPackageRoot((IPackageFragment) e);
		}

		return (IPackageFragmentRoot) e;
	}

	private IPackageFragment getPackage(IPackageFragmentRoot fragmentRoot, String packageName) {
		IPackageFragment fragment = BlendeePlugin.findPackage(packageName);
		if (fragment != null) return fragment;

		try {
			return fragmentRoot.createPackageFragment(packageName, false, null);
		} catch (JavaModelException e) {
			throw new IllegalStateException(e);
		}
	}

	private boolean isAvailable(TablePath path) {
		String typeName = String.join(
			".",
			new String[] {
				BlendeePlugin.getDefault().getOutputPackage(),
				TableFacadePackageRule.care(path.getSchemaName()),
				path.getTableName() });
		try {
			if (BlendeePlugin.getDefault().getProject().findType(typeName) != null) return true;
			return false;
		} catch (JavaModelException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String format(CodeFormatter formatter, String source) {
		Document document = new Document(source);
		try {
			formatter.format(
				CodeFormatter.K_COMPILATION_UNIT,
				source,
				0,
				source.length(),
				0,
				null).apply(document);
		} catch (BadLocationException e) {
			throw new IllegalStateException(e);
		}
		return document.get();
	}

	private static class TableAction extends Action {

		private TableElement element;

		private TableAction() {
			//Table クラスを生成する
			String text = "Generate TableFacade";
			setText(text);
			setToolTipText(text);
			setImageDescriptor(Constants.TABLE_ICON);
		}

		@Override
		public void run() {
			try {
				BlendeePlugin.getDefault().refresh();
				Blendee.execute(t -> {
					element.build();
				});
			} catch (Throwable t) {
				throw new IllegalStateException(t);
			} finally {
				BlendeePlugin.refreshOutputPackage();
			}
		}
	}
}
