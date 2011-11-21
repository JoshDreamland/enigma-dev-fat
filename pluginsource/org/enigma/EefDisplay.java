package org.enigma;

import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;

import org.enigma.file.EEFReader;
import org.enigma.file.EEFReader.EEFNode;

public class EefDisplay extends JSplitPane implements TreeSelectionListener
	{
	private static final long serialVersionUID = 1L;
	JTree t;
	JTextArea ta;

	public EefDisplay(EEFNode ef)
		{
		super();

		ta = new JTextArea(20,40);
		ta.setFont(new Font(Font.MONOSPACED,0,12));
		//		ta.setEditable(false);

		t = new JTree(new EEFTreeNode(ef,null));
		//		t.setVisibleRowCount(0);
		//		;t.setPreferredSize(new Dimension(150,100));
		t.setRootVisible(false);
		t.setShowsRootHandles(true);
		t.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		t.addTreeSelectionListener(this);

		JScrollPane p = new JScrollPane(t);
		p.setPreferredSize(new Dimension(170,p.getPreferredSize().height));

		setLeftComponent(p);
		setRightComponent(new JScrollPane(ta));
		}

	abstract class AbstractTreeNode implements TreeNode
		{
		TreeNode parent;

		public AbstractTreeNode(TreeNode parent)
			{
			this.parent = parent;
			}

		@Override
		public TreeNode getParent()
			{
			return parent;
			}

		@Override
		public int getIndex(TreeNode node)
			{
			for (int i = 0; i < getChildCount(); i++)
				if (getChildAt(i).equals(node)) return i;
			return -1;
			}

		@Override
		public boolean getAllowsChildren()
			{
			return !isLeaf();
			}

		@Override
		public boolean isLeaf()
			{
			return getChildCount() == 0;
			}

		@Override
		public Enumeration<TreeNode> children()
			{
			return new Enumeration<TreeNode>()
				{
					int cursor;

					@Override
					public boolean hasMoreElements()
						{
						return cursor < getChildCount();
						}

					@Override
					public TreeNode nextElement()
						{
						return getChildAt(cursor);
						}
				};
			}
		}

	class EEFTreeNode extends AbstractTreeNode
		{
		EEFNode obj;
		String name;
		TreeNode children[];

		EEFTreeNode(EEFNode n, TreeNode parent)
			{
			super(parent);
			obj = n;
			name = getName();
			if (name == null || name.isEmpty()) name = "<unnamed>";

			children = new TreeNode[n.children.size()];
			for (int i = 0; i < children.length; i++)
				children[i] = new EEFTreeNode(n.children.get(i),this);
			}

		@Override
		public TreeNode getChildAt(int childIndex)
			{
			return children[childIndex];
			}

		@Override
		public int getChildCount()
			{
			return children.length;
			}

		public String getName()
			{
			if (obj.id == null || obj.id.length == 0) return obj.blockName;
			String name = "(" + implode(obj.id) + ")";
			if (obj.blockName != null && !obj.blockName.isEmpty()) return obj.blockName + ": " + name;
			return name;
			}

		public String toString()
			{
			return name;
			}
		}

	@Override
	public void valueChanged(TreeSelectionEvent e)
		{
		Object o = t.getLastSelectedPathComponent();
		if (o == null || !(o instanceof EEFTreeNode)) return;
		ta.setText(nodeInfo(((EEFTreeNode) o).obj));
		}

	static String nodeInfo(EEFNode node)
		{
		if (node.lineAttribs.isEmpty()) return new String();
		StringBuilder sb = new StringBuilder(node.lineAttribs.get(0).replace(' ','·'));
		for (int i = 1; i < node.lineAttribs.size(); i++)
			sb.append("¶\n").append(node.lineAttribs.get(i).replace(' ','·'));
		return sb.toString();
		}

	private static String implode(String...args)
		{
		return implode((Object[]) args);
		}

	private static String implode(Object...args)
		{
		if (args.length == 0) return null;
		StringBuilder sb = new StringBuilder(args[0].toString());
		for (int i = 1; i < args.length; i++)
			sb.append(",").append(args[i]);
		return sb.toString();
		}

	public static void main(String[] args) throws IOException
		{
		File fi = new File(System.getProperty("user.home"),"rm_0.rme");
		EEFNode ef = EEFReader.parse(new FileInputStream(fi));

		JFrame f = new JFrame("Eef Viewer");
		f.setContentPane(new EefDisplay(ef));
		f.pack();
		f.setLocationRelativeTo(null);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
		}
	}
