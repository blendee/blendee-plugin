package org.blendee.plugin.views.element;

import java.util.LinkedList;
import java.util.List;

import org.blendee.develop.TableFacadeGenerator;
import org.blendee.jdbc.BlendeeManager;
import org.blendee.jdbc.ContextManager;
import org.blendee.jdbc.TablePath;
import org.blendee.plugin.BlendeePlugin;
import org.blendee.plugin.Constants;
import org.blendee.plugin.PluginTableFacadeGenerator;
import org.blendee.sql.Relationship;
import org.blendee.sql.RelationshipFactory;
import org.blendee.support.TableFacadePackageRule;
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
		return "テーブル";
	}

	void build() throws Exception {
		BlendeePlugin plugin = BlendeePlugin.getDefault();

		String packageName = plugin.getOutputPackage();

		IPackageFragment baseFragment = BlendeePlugin.findPackage(packageName);
		if (baseFragment == null)
			throw new IllegalStateException("パッケージ " + packageName + " が存在しません");

		IPackageFragmentRoot fragmentRoot = findPackageRoot(baseFragment);

		TableFacadeGenerator generator = new PluginTableFacadeGenerator(
			BlendeeManager.get().getMetadata(),
			baseFragment.getElementName(),
			plugin.getTableFacadeParentClass(),
			plugin.getRowParentClass(),
			plugin.getCodeFormatter(),
			plugin.useNumberClass(),
			!plugin.notUseNullGuard());

		RelationshipFactory factory = ContextManager.get(RelationshipFactory.class);
		Relationship relation = factory.getInstance(path);
		LinkedList<TablePath> tables = new LinkedList<>();
		//自身をセット
		tables.add(relation.getTablePath());

		IPackageFragment schemaPackage = getPackage(fragmentRoot, packageName + "." + TableFacadePackageRule.care(parent.getName()));

		while (tables.size() > 0) {
			TablePath targetPath = tables.pop();
			Relationship target = factory.getInstance(targetPath);

			build(generator, schemaPackage, target);

			collect(tables, target);

			//大量のテーブルを一度に実行したときのための節約クリア
			//Metadataはキャッシュを使用しているので、同じテーブルを処理してもDBから再取得はしない
			factory.clearCache();
		}

		generator.writeDatabaseInfo(fragmentRoot.getResource().getLocation().toFile());
	}

	boolean isAvailable() {
		return isAvailable(path);
	}

	private void collect(List<TablePath> tables, Relationship relation) {
		for (Relationship child : relation.getRelationships()) {
			TablePath childPath = child.getTablePath();
			if (!isAvailable(childPath)) tables.add(childPath);
		}
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
				TableFacadePackageRule.care(parent.getName()),
				path.getTableName() });
		try {
			if (BlendeePlugin.getDefault().getProject().findType(typeName) != null) return true;
			return false;
		} catch (JavaModelException e) {
			throw new IllegalStateException(e);
		}
	}

	private void build(
		TableFacadeGenerator generator,
		IPackageFragment packageFragment,
		Relationship relation) {
		TablePath path = relation.getTablePath();
		String tableName = path.getTableName();
		try {
			CodeFormatter formatter = ToolFactory.createCodeFormatter(
				BlendeePlugin.getDefault().getProject().getOptions(true));

			createSource(
				TableFacadeGenerator.createCompilationUnitName(tableName),
				packageFragment,
				format(formatter, generator.build(relation)));
		} catch (JavaModelException e) {
			throw new IllegalStateException(e);
		}

		parent.refresh(path);
		Thread.yield();
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

	private static void createSource(String compilationUnitName, IPackageFragment fragment, String source)
		throws JavaModelException {
		ICompilationUnit compilationUnit = fragment.getCompilationUnit(compilationUnitName);
		if (compilationUnit.exists() && source.equals(compilationUnit.getSource())) return;

		fragment.createCompilationUnit(compilationUnitName, source, true, null);
	}

	private static class TableAction extends Action {

		private TableElement element;

		private TableAction() {
			String text = "Table クラスを生成する";
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
