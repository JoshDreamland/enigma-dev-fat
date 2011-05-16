/*
 * Copyright (C) 2008-2011 IsmAvatar <IsmAvatar@gmail.com>
 * 
 * This file is part of Enigma Plugin.
 * 
 * Enigma Plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Enigma Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License (COPYING) for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.enigma;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.enigma.TargetHandler.TargetSelection;
import org.enigma.YamlParser.YamlElement;
import org.enigma.YamlParser.YamlNode;
import org.enigma.backend.EnigmaSettings;
import org.lateralgm.compare.CollectionComparator;
import org.lateralgm.compare.MapComparator;
import org.lateralgm.compare.ObjectComparator;
import org.lateralgm.compare.ReflectionComparator;
import org.lateralgm.compare.SimpleCasesComparator;
import org.lateralgm.components.CustomFileChooser;
import org.lateralgm.components.impl.CustomFileFilter;
import org.lateralgm.components.impl.IndexButtonGroup;
import org.lateralgm.components.mdi.MDIFrame;
import org.lateralgm.file.GmStreamDecoder;
import org.lateralgm.main.LGM;
import org.lateralgm.messages.Messages;
import org.lateralgm.subframes.CodeFrame;
import org.lateralgm.subframes.CodeFrame.CodeHolder;

public class EnigmaSettingsFrame extends MDIFrame implements ActionListener
	{
	private static final long serialVersionUID = 1L;
	private static final ImageIcon CODE_ICON = LGM.getIconForKey("Resource.SCRIPT"); //$NON-NLS-1$

	private final EnigmaSettings oldEs;
	private EnigmaSettings es;

	protected JToolBar toolbar;
	protected JButton save, saveFile, loadFile;

	private Map<String,Option> options;
	//	private IndexButtonGroup strings, increment, equal, literal, escape;
	//	private IndexButtonGroup instance, storage;
	private JButton bDef, bGlobLoc;
	private JButton bInit, bClean;
	private CodeFrame cfDef, cfGlobLoc, cfInit, cfClean;
	private CodeHolder sDef, sGlobLoc, sInit, sClean;
	private CustomFileChooser fc;

	private JComboBox targComp, targPlat, targGfx, targAudio, targColl, targWidg;
	private JTextField tfAuth;
	private JTextArea taDesc;

	class SimpleCodeHolder implements CodeHolder
		{
		private String code;

		public SimpleCodeHolder(String s)
			{
			code = s;
			}

		@Override
		public String getCode()
			{
			return code;
			}

		@Override
		public void setCode(String s)
			{
			code = s;
			}
		}

	class YamlException extends Exception
		{
		private static final long serialVersionUID = 1L;

		YamlException(String s)
			{
			super(s);
			}
		}

	class Option
		{
		JComponent[] cChoices;

		IndexButtonGroup ibg = null;
		JComboBox cb = null;
		String other = null;

		Option(String name, String type, int cnum, String choices)
		//				throws IllegalArgumentException
			{
			if (name != null && name.isEmpty()) name = null;
			if (type.equalsIgnoreCase("Label"))
				populateLabel(type,name,choices);
			else if (type.equalsIgnoreCase("Radio-1"))
				populateRadio1(name,choices == null ? null : choices.split(","));
			else if (type.equalsIgnoreCase("Combobox"))
				populateCombo(name,choices == null ? null : choices.split(","));
			else
				populateLabel(type,name,choices);
			//				throw new IllegalArgumentException(type);

			JComponent[] bc = cChoices;
			cChoices = new JComponent[cnum];
			System.arraycopy(bc,0,cChoices,0,bc.length);
			for (int i = bc.length; i < cnum; i++)
				cChoices[i] = new JLabel();
			}

		void populateLabel(String type, String name, String choices)
			{
			cChoices = new JComponent[1];
			String fmt = "Unsupported option: [%s] [%s]";
			JLabel lab = new JLabel(String.format(fmt,type,name));
			lab.setToolTipText(other = choices);
			cChoices[0] = lab;
			}

		void populateRadio1(String name, String...choices)
			{
			cChoices = new JComponent[choices.length + (name == null ? 0 : 1)];
			if (name != null) cChoices[0] = new JLabel(name);
			ibg = new IndexButtonGroup(2,true,false);
			for (int i = 0; i < choices.length; i++)
				{
				JRadioButton but = new JRadioButton(choices[i]);
				cChoices[i + (name == null ? 0 : 1)] = but;
				ibg.add(but);
				}
			}

		void populateCombo(String name, String...choices)
			{
			cChoices = new JComponent[name == null ? 1 : 2];
			if (name != null) cChoices[0] = new JLabel(name);
			cChoices[name == null ? 0 : 1] = cb = new JComboBox(choices);
			}

		void setValue(String val)
			{
			other = val;
			if (val == null) return;
			try
				{
				setValue(Integer.parseInt(val));
				}
			catch (NumberFormatException e)
				{
				return;
				}
			}

		void setValue(int val)
			{
			if (ibg != null)
				ibg.setValue(val);
			else if (cb != null) cb.setSelectedIndex(val);
			}

		String getValue()
			{
			if (ibg != null) return Integer.toString(ibg.getValue());
			if (cb != null) return Integer.toString(cb.getSelectedIndex());
			return other;
			}
		}

	public EnigmaSettingsFrame(EnigmaSettings es)
		{
		super("Enigma Settings",false,true,true,true); //$NON-NLS-1$
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setFrameIcon(LGM.findIcon("restree/gm.png")); //$NON-NLS-1$
		this.oldEs = es;
		this.es = es.copy();

		fc = new CustomFileChooser("/org/enigma","LAST_SETTINGS_DIR"); //$NON-NLS-1$ //$NON-NLS-2$
		fc.setFileFilter(new CustomFileFilter(".esf","Enigma Settings File (.esf)")); //$NON-NLS-1$

		toolbar = makeToolBar();
		add(toolbar,BorderLayout.NORTH);
		JTabbedPane tabs = new JTabbedPane();
		tabs.add("General",makeSettings());
		tabs.add("API",makeAPI());
		add(tabs,BorderLayout.CENTER);
		pack();
		}

	private JToolBar makeToolBar()
		{
		JToolBar tool = new JToolBar();
		tool.setFloatable(false);

		// Setup the buttons
		save = new JButton(LGM.getIconForKey("ResourceFrame.SAVE")); //$NON-NLS-1$
		save.setRequestFocusEnabled(false);
		save.addActionListener(this);
		tool.add(save);
		tool.addSeparator();

		loadFile = new JButton(LGM.getIconForKey("LGM.OPEN"));
		loadFile.setToolTipText("Load from file");
		loadFile.setRequestFocusEnabled(false);
		loadFile.addActionListener(this);
		tool.add(loadFile);
		saveFile = new JButton(LGM.getIconForKey("LGM.SAVEAS"));
		saveFile.setToolTipText("Save to file");
		saveFile.setRequestFocusEnabled(false);
		saveFile.addActionListener(this);
		tool.add(saveFile);

		tool.addSeparator();
		return tool;
		}

	private JPanel makePane(String name, int choiceCount, Option...opts)
		{
		JPanel pane = new JPanel();
		pane.setBorder(BorderFactory.createTitledBorder(name));
		GroupLayout layout = new GroupLayout(pane);
		pane.setLayout(layout);

		Group g = layout.createSequentialGroup();
		Group g1 = layout.createParallelGroup();
		for (int i = 0; i < choiceCount; i++)
			{
			g1 = layout.createParallelGroup();
			for (Option o : opts)
				g1.addComponent(o.cChoices[i]);
			g.addGroup(g1);
			}
		layout.setHorizontalGroup(g);

		g = layout.createSequentialGroup();
		for (Option o : opts)
			{
			g1 = layout.createParallelGroup(Alignment.BASELINE);
			for (int i = 0; i < choiceCount; i++)
				g1.addComponent(o.cChoices[i]);
			g.addGroup(g1);
			}
		layout.setVerticalGroup(g);

		return pane;
		}

	private List<JPanel> parsePanels() throws YamlException
		{
		options = new HashMap<String,Option>();
		ArrayList<JPanel> panels = new ArrayList<JPanel>();
		File settings = new File(EnigmaRunner.WORKDIR,"settings.ey");
		YamlNode n;
		try
			{
			n = YamlParser.parse(settings);
			}
		catch (FileNotFoundException fnfe)
			{
			throw new YamlException(settings.getPath() + ": File not found");
			}
		if (n.chronos.isEmpty()) throw new YamlException(settings.getName() + ": Empty");

		//loop through panels
		for (YamlElement ge : n.chronos)
			{
			String name = ge.name;
			String error = settings.getName() + ": " + name + ": ";

			if (ge.isScalar) throw new YamlException(error + "Scalar");

			n = (YamlNode) ge;
			if (!n.getMC("Layout","Grid").equalsIgnoreCase("Grid"))
				throw new YamlException(error + "Layout: Grid only supported");

			int columns;
			try
				{
				columns = Integer.parseInt(n.getMC("Columns","null"));
				}
			catch (NumberFormatException nfe)
				{
				throw new YamlException(error + "Must be a positive integer");
				}

			//loop through options in panel
			ArrayList<Option> opts = new ArrayList<Option>(n.chronos.size());
			for (YamlElement oe : n.chronos)
				{
				if (oe.name.equalsIgnoreCase("Layout") || oe.name.equalsIgnoreCase("Columns")) continue;

				if (oe.isScalar)
					{
					System.err.println(oe.name + ": Scalar");
					continue;
					}
				YamlNode on = (YamlNode) oe;

				Option opt;
				try
					{
					opt = new Option(on.getMC("Label",null),on.getMC("Type","<no type>"),columns,on.getMC(
							"Options",null));
					opt.setValue(on.getMC("Default","0"));
					}
				catch (IllegalArgumentException e)
					{
					System.err.println(error + on.name + ": Type: " + e.getMessage() + ": Unsupported");
					continue;
					}
				opts.add(opt);
				options.put(on.name,opt);
				} //each option in panel

			panels.add(makePane(name,columns,opts.toArray(new Option[0])));
			}
		return panels;
		}

	private JPanel makeSettings()
		{
		JPanel p = new JPanel();
		GroupLayout layout = new GroupLayout(p);
		p.setLayout(layout);

		List<JPanel> panels = null;
		try
			{
			panels = parsePanels();
			}
		catch (YamlException e)
			{
			// TODO Auto-generated catch block
			e.printStackTrace();
			}

		sDef = new SimpleCodeHolder(es.definitions);
		sGlobLoc = new SimpleCodeHolder(es.globalLocals);
		sInit = new SimpleCodeHolder(es.initialization);
		sClean = new SimpleCodeHolder(es.cleanup);

		bDef = new JButton("Definitions",CODE_ICON);
		bGlobLoc = new JButton("Global Locals",CODE_ICON);
		bInit = new JButton("Initialization",CODE_ICON);
		bClean = new JButton("Cleanup",CODE_ICON);

		bDef.addActionListener(this);
		bGlobLoc.addActionListener(this);
		bInit.addActionListener(this);
		bClean.addActionListener(this);

		Group gh = layout.createParallelGroup();
		SequentialGroup gv = layout.createSequentialGroup();
		if (panels != null) for (JPanel pp : panels)
			{
			gh.addComponent(pp);
			gv.addComponent(pp);
			}

		layout.setHorizontalGroup(gh
		/**/.addGroup(layout.createSequentialGroup()
		/*	*/.addComponent(bDef)
		/*	*/.addComponent(bGlobLoc))
		/**/.addGroup(layout.createSequentialGroup()
		/*	*/.addComponent(bInit)
		/*	*/.addComponent(bClean)));
		layout.setVerticalGroup(gv
		/**/.addGroup(layout.createParallelGroup()
		/*	*/.addComponent(bDef)
		/*	*/.addComponent(bGlobLoc))
		/**/.addGroup(layout.createParallelGroup()
		/*	*/.addComponent(bInit)
		/*	*/.addComponent(bClean))
		/**/.addContainerGap());

		return p;
		}

	private JPanel makeAPI()
		{
		JPanel p = new JPanel();
		GroupLayout layout = new GroupLayout(p);
		p.setLayout(layout);

		JPanel platPane = new JPanel(new GridLayout(4,3));
		platPane.setBorder(BorderFactory.createTitledBorder("Platform"));
		GroupLayout plat = new GroupLayout(platPane);
		platPane.setLayout(plat);

		targComp = new JComboBox(TargetHandler.getTargetCompilersArray());
		targPlat = new JComboBox(TargetHandler.getTargetPlatformsArray());
		targGfx = new JComboBox(TargetHandler.getTargetGraphicsArray());
		targAudio = new JComboBox(TargetHandler.getTargetAudiosArray());
		targColl = new JComboBox(TargetHandler.getTargetCollisionsArray());
		targWidg = new JComboBox(TargetHandler.getTargetWidgetsArray());
		targComp.setSelectedItem(es.selCompiler);
		targPlat.setSelectedItem(es.selPlatform);
		targGfx.setSelectedItem(es.selGraphics);
		targAudio.setSelectedItem(es.selAudio);
		targColl.setSelectedItem(es.selCollision);
		targWidg.setSelectedItem(es.selWidgets);
		targComp.addActionListener(this);
		targPlat.addActionListener(this);
		targGfx.addActionListener(this);
		targAudio.addActionListener(this);
		targColl.addActionListener(this);
		targWidg.addActionListener(this);

		tfAuth = new JTextField(es.selCompiler == null ? null : es.selCompiler.auth);
		taDesc = new JTextArea(es.selCompiler == null ? null : es.selCompiler.desc);
		tfAuth.setEditable(false);
		taDesc.setEditable(false);
		taDesc.setLineWrap(true);
		taDesc.setWrapStyleWord(true);

		JLabel lComp = new JLabel("Compiler: ");
		JLabel lPlat = new JLabel("Platform: ");
		JLabel lGfx = new JLabel("Graphics: ");
		JLabel lSound = new JLabel("Audio: ");
		JLabel lColl = new JLabel("Collision: ");
		JLabel lWidg = new JLabel("Widgets: ");
		JLabel lAuth = new JLabel("Author: ");
		JScrollPane desc = new JScrollPane(taDesc);

		int pref = GroupLayout.PREFERRED_SIZE;
		layout.setHorizontalGroup(layout.createParallelGroup()
		/**/.addGroup(layout.createSequentialGroup()
		/*	*/.addGroup(layout.createParallelGroup()
		/*		*/.addComponent(lComp)
		/*		*/.addComponent(lPlat)
		/*		*/.addComponent(lGfx)
		/*		*/.addComponent(lSound)
		/*		*/.addComponent(lColl)
		/*		*/.addComponent(lWidg))
		/*	*/.addPreferredGap(ComponentPlacement.RELATED)
		/*	*/.addGroup(layout.createParallelGroup()
		/*		*/.addComponent(targComp)
		/*		*/.addComponent(targPlat)
		/*		*/.addComponent(targGfx)
		/*		*/.addComponent(targAudio)
		/*		*/.addComponent(targColl)
		/*		*/.addComponent(targWidg)))
		/**/.addGroup(layout.createSequentialGroup()
		/*	*/.addComponent(lAuth)
		/*	*/.addPreferredGap(ComponentPlacement.RELATED)
		/*	*/.addComponent(tfAuth))
		/**/.addComponent(desc));
		layout.setVerticalGroup(layout.createSequentialGroup()
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*	*/.addComponent(lComp)
		/*	*/.addComponent(targComp,pref,pref,pref))
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*	*/.addComponent(lPlat)
		/*	*/.addComponent(targPlat,pref,pref,pref))
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*	*/.addComponent(lGfx)
		/*	*/.addComponent(targGfx,pref,pref,pref))
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*	*/.addComponent(lSound)
		/*	*/.addComponent(targAudio,pref,pref,pref))
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*	*/.addComponent(lColl)
		/*	*/.addComponent(targColl,pref,pref,pref))
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*	*/.addComponent(lWidg)
		/*	*/.addComponent(targWidg,pref,pref,pref))
		/**/.addPreferredGap(ComponentPlacement.RELATED)
		/**/.addGroup(layout.createParallelGroup(Alignment.BASELINE)
		/*	*/.addComponent(lAuth)
		/*	*/.addComponent(tfAuth))
		/**/.addPreferredGap(ComponentPlacement.RELATED)
		/**/.addComponent(desc,0,0,Integer.MAX_VALUE));

		return p;
		}

	//TODO: use yaml instead
	public void loadFromFile()
		{
		fc.setDialogTitle("File to load information from");
		while (true)
			{
			if (fc.showOpenDialog(LGM.frame) != JFileChooser.APPROVE_OPTION) return;
			if (fc.getSelectedFile().exists()) break;
			JOptionPane.showMessageDialog(null,fc.getSelectedFile().getName()
					+ Messages.getString("SoundFrame.FILE_MISSING"), //$NON-NLS-1$
					"File to load information from",JOptionPane.WARNING_MESSAGE);
			}
		try
			{
			FileInputStream i = new FileInputStream(fc.getSelectedFile());
			int ver = i.read();
			if (ver != 1) throw new IOException("Incorrect version: " + ver);
			//			es.cppStrings = i.read();
			//			es.cppOperators = i.read();
			//			es.cppEquals = i.read();
			//			es.literalHandling = i.read();
			//			es.cppEscapes = i.read();
			//
			//			es.instanceTypes = i.read();
			//			es.storageClass = i.read();
			i.skip(7);

			es.definitions = readStr(i);
			if (!es.definitions.equals(sDef.getCode()))
				{
				es.definitions = sDef.getCode();
				es.saveDefinitions();
				if (EnigmaRunner.ENIGMA_READY) es.commitToDriver(EnigmaRunner.DRIVER);
				}
			es.globalLocals = readStr(i);
			es.initialization = readStr(i);
			es.cleanup = readStr(i);
			setComponents(es);
			i.close();
			}
		catch (IOException e)
			{
			e.printStackTrace();
			}
		}

	public void saveToFile()
		{
		commitChanges();
		fc.setDialogTitle("File to write information to"); //$NON-NLS-1$
		if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
		String name = fc.getSelectedFile().getPath();
		if (CustomFileFilter.getExtension(name) == null) name += ".esf"; //$NON-NLS-1$
		try
			{
			FileOutputStream i = new FileOutputStream(new File(name));

			i.write(1);

			for (int j = 0; j < 7; j++)
				i.write(0);
			//			i.write(es.cppStrings);
			//			i.write(es.cppOperators);
			//			i.write(es.cppEquals);
			//			i.write(es.literalHandling);
			//			i.write(es.cppEscapes);
			//
			//			i.write(es.instanceTypes);
			//			i.write(es.storageClass);

			writeStr(i,es.definitions);
			writeStr(i,es.globalLocals);
			writeStr(i,es.initialization);
			writeStr(i,es.cleanup);
			i.close();

			i.close();
			}
		catch (Exception e)
			{
			e.printStackTrace();
			}
		}

	String readStr(InputStream is) throws IOException
		{
		int a = is.read();
		int b = is.read();
		int c = is.read();
		int d = is.read();
		int size = (a | (b << 8) | (c << 16) | (d << 24));

		byte data[] = new byte[size];
		is.read(data);
		return new String(data,GmStreamDecoder.CHARSET);
		}

	void writeStr(OutputStream os, String str) throws IOException
		{
		int val = str.length();
		os.write(val & 255);
		os.write((val >>> 8) & 255);
		os.write((val >>> 16) & 255);
		os.write((val >>> 24) & 255);
		os.write(str.getBytes(GmStreamDecoder.CHARSET));
		}

	public void updateResource()
		{
		commitChanges();
		es.copyInto(oldEs);
		}

	public void commitChanges()
		{
		es.options.clear();
		for (Entry<String,Option> entry : options.entrySet())
			es.options.put(entry.getKey(),entry.getValue() == null ? null : entry.getValue().getValue());

		es.definitions = sDef.getCode();
		es.globalLocals = sGlobLoc.getCode();
		es.initialization = sInit.getCode();
		es.cleanup = sClean.getCode();

		es.selCompiler = (TargetSelection) targComp.getSelectedItem();
		es.selPlatform = (TargetSelection) targPlat.getSelectedItem();
		es.selGraphics = (TargetSelection) targGfx.getSelectedItem();
		es.selAudio = (TargetSelection) targAudio.getSelectedItem();
		es.selCollision = (TargetSelection) targColl.getSelectedItem();
		es.selWidgets = (TargetSelection) targWidg.getSelectedItem();
		}

	public void setComponents(EnigmaSettings es)
		{
		this.es = es;

		for (Entry<String,String> entry : es.options.entrySet())
			{
			Option opt = options.get(entry.getKey());
			if (opt != null) opt.setValue(entry.getValue());
			}

		sDef.setCode(es.definitions);
		sGlobLoc.setCode(es.globalLocals);
		sInit.setCode(es.initialization);
		sClean.setCode(es.cleanup);
		}

	public void revertResource()
		{
		setComponents(oldEs);
		}

	public boolean resourceChanged()
		{
		commitChanges();
		ReflectionComparator rc = new SimpleCasesComparator(new CollectionComparator(new MapComparator(
				new ObjectComparator(null))));
		return !rc.areEqual(oldEs,es);
		}

	@Override
	public void actionPerformed(ActionEvent e)
		{
		Object s = e.getSource();

		if (s == save)
			{
			updateResource();
			setVisible(false);
			return;
			}

		if (s == loadFile)
			{
			loadFromFile();
			return;
			}

		if (s == saveFile)
			{
			saveToFile();
			return;
			}

		if (s == bDef || s == bGlobLoc || s == bInit || s == bClean)
			{
			if (s == bDef)
				{
				if (cfDef == null) cfDef = newCodeFrame(sDef,"Enigma Definitions");
				cfDef.toTop();
				}
			if (s == bGlobLoc)
				{
				if (cfDef == null) cfGlobLoc = newCodeFrame(sGlobLoc,"Enigma Global Locals");
				cfGlobLoc.toTop();
				}
			if (s == bInit)
				{
				if (cfInit == null) cfInit = newCodeFrame(sInit,"Enigma Initialization");
				cfInit.toTop();
				}
			if (s == bClean)
				{
				if (cfClean == null) cfClean = newCodeFrame(sClean,"Enigma Cleanup");
				cfClean.toTop();
				}
			return;
			}

		if (s == targComp || s == targPlat || s == targGfx || s == targAudio || s == targColl
				|| s == targWidg)
			{
			TargetSelection ts = (TargetSelection) ((JComboBox) s).getSelectedItem();
			if (s == targPlat)
				{
				es.selPlatform = ts;
				targGfx.setModel(new DefaultComboBoxModel(TargetHandler.getTargetGraphicsArray()));
				targAudio.setModel(new DefaultComboBoxModel(TargetHandler.getTargetAudiosArray()));
				targColl.setModel(new DefaultComboBoxModel(TargetHandler.getTargetCollisionsArray()));
				targWidg.setModel(new DefaultComboBoxModel(TargetHandler.getTargetWidgetsArray()));
				}
			tfAuth.setText(ts == null ? null : ts.auth);
			taDesc.setText(ts == null ? null : ts.desc);
			return;
			}
		}

	protected void fireInternalFrameEvent(int id)
		{
		if (id == InternalFrameEvent.INTERNAL_FRAME_CLOSING)
			{
			if (resourceChanged())
				{
				int ret = JOptionPane.showConfirmDialog(LGM.frame,Messages.format(
						"ResourceFrame.KEEPCHANGES","Enigma Settings"), //$NON-NLS-1$
						Messages.getString("ResourceFrame.KEEPCHANGES_TITLE"),JOptionPane.YES_NO_CANCEL_OPTION); //$NON-NLS-1$
				if (ret == JOptionPane.YES_OPTION)
					{
					updateResource();
					setVisible(false);
					}
				else if (ret == JOptionPane.NO_OPTION)
					{
					revertResource();
					setVisible(false);
					}
				}
			else
				{
				updateResource();
				setVisible(false);
				}
			}
		super.fireInternalFrameEvent(id);
		}

	private final InternalFrameListener ifl = new CodeFrameListener();

	private class CodeFrameListener extends InternalFrameAdapter
		{
		public void internalFrameClosed(InternalFrameEvent e)
			{
			CodeFrame cf = (CodeFrame) e.getSource();
			if (cf == cfDef)
				{
				if (!es.definitions.equals(sDef.getCode()))
					{
					es.definitions = sDef.getCode();
					es.saveDefinitions();
					if (EnigmaRunner.ENIGMA_READY) es.commitToDriver(EnigmaRunner.DRIVER);
					}
				cfDef = null;
				}
			if (cf == cfGlobLoc) cfGlobLoc = null;
			if (cf == cfInit) cfInit = null;
			if (cf == cfClean) cfClean = null;
			}
		}

	private CodeFrame newCodeFrame(CodeHolder ch, String title)
		{
		CodeFrame cf = new CodeFrame(ch,"{0}",title);
		cf.addInternalFrameListener(ifl);
		LGM.mdi.add(cf);
		LGM.mdi.addZChild(this,cf);
		return cf;
		}
	}
