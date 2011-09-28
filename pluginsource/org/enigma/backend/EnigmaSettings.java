/*
 * Copyright (C) 2010, 2011 IsmAvatar <IsmAvatar@gmail.com>
 * 
 * This file is part of Enigma Plugin.
 * Enigma Plugin is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */

package org.enigma.backend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.enigma.EnigmaRunner;
import org.enigma.SettingsHandler;
import org.enigma.TargetHandler;
import org.enigma.SettingsHandler.ExtensionSetting;
import org.enigma.SettingsHandler.OptionGroupSetting;
import org.enigma.SettingsHandler.OptionSetting;
import org.enigma.TargetHandler.TargetSelection;
import org.enigma.backend.EnigmaDriver.SyntaxError;
import org.lateralgm.resources.Resource;
import org.lateralgm.resources.ResourceReference;
import org.lateralgm.util.PropertyMap;

public class EnigmaSettings extends Resource<EnigmaSettings,EnigmaSettings.PEnigmaSettings>
	{
	public String definitions = "", globalLocals = ""; //$NON-NLS-1$ //$NON-NLS-2$
	public String initialization = "", cleanup = ""; //$NON-NLS-1$ //$NON-NLS-2$

	public Map<String,String> options = new HashMap<String,String>();
	//Strings one of: "compiler","windowing","graphics","audio","collision","widget"
	public Map<String,TargetSelection> targets = new HashMap<String,TargetSelection>();
	public Set<String> extensions = new HashSet<String>();

	public enum PEnigmaSettings
		{
		}

	public EnigmaSettings()
		{
		this(true);
		}

	private EnigmaSettings(boolean load)
		{
		if (!load) return;

		loadDefinitions();
		targets.putAll(TargetHandler.defaults);

		for (OptionGroupSetting ogs : SettingsHandler.optionGroups)
			for (OptionSetting os : ogs.opts)
				options.put(os.id,os.def);

		for (ExtensionSetting es : SettingsHandler.extensions)
			if (es.def) extensions.add(es.path);
		}

	void loadDefinitions()
		{
		definitions = fileToString(new File(EnigmaRunner.WORKDIR,"definitions.h")); //$NON-NLS-1$
		}

	public void saveDefinitions()
		{
		writeString(new File(EnigmaRunner.WORKDIR,"definitions.h"),definitions); //$NON-NLS-1$
		}

	static String fileToString(File f)
		{
		StringBuffer sb = new StringBuffer(1024);
		try
			{
			FileReader in = new FileReader(f);
			char[] cbuf = new char[1024];
			int size = 0;
			while ((size = in.read(cbuf)) > -1)
				sb.append(cbuf,0,size);
			in.close();
			}
		catch (IOException e)
			{
			}
		return sb.toString();
		}

	static void writeString(File f, String s)
		{
		try
			{
			PrintStream ps = new PrintStream(f);
			ps.print(s);
			ps.close();
			}
		catch (FileNotFoundException e)
			{
			}
		}

	private String toYaml()
		{
		StringBuilder yaml = new StringBuilder("%e-yaml\n---\n"); //$NON-NLS-1$
		for (Entry<String,String> entry : options.entrySet())
			yaml.append(entry.getKey()).append(": ").append(entry.getValue()).append('\n'); //$NON-NLS-1$

		yaml.append('\n');

		for (Entry<String,TargetSelection> entry : targets.entrySet())
			{
			if (entry.getValue() == null) continue;
			yaml.append("target-").append(entry.getKey()).append(": ").append(entry.getValue().id).append('\n'); //$NON-NLS-1$ //$NON-NLS-2$
			}
		yaml.append("target-networking: None\n"); //$NON-NLS-1$

		if (extensions.size() > 0)
			{
			yaml.append('\n');
			yaml.append("extensions: "); //$NON-NLS-1$

			String[] exts = extensions.toArray(new String[0]);
			yaml.append(exts[0]);
			for (int i = 1; i < exts.length; i++)
				yaml.append(',').append(exts[i]);

			yaml.append('\n');
			}

		System.out.println();
		System.out.println(yaml);

		return yaml.toString();
		}

	public SyntaxError commitToDriver(EnigmaDriver driver)
		{
		return driver.definitionsModified(definitions,toYaml());
		}

	public EnigmaSettings copy()
		{
		EnigmaSettings es = new EnigmaSettings(false);
		copyInto(es);
		return es;
		}

	public void copyInto(EnigmaSettings es)
		{
		es.definitions = definitions;
		es.globalLocals = globalLocals;
		es.initialization = initialization;
		es.cleanup = cleanup;

		es.options.clear();
		es.options.putAll(options);
		es.targets.clear();
		es.targets.putAll(targets);
		es.extensions.clear();
		es.extensions.addAll(extensions);
		}

	@Override
	public EnigmaSettings makeInstance(ResourceReference<EnigmaSettings> ref)
		{
		return new EnigmaSettings(false);
		}

	@Override
	protected PropertyMap<PEnigmaSettings> makePropertyMap()
		{
		return new PropertyMap<PEnigmaSettings>(PEnigmaSettings.class,this,null);
		}

	@Override
	protected void postCopy(EnigmaSettings dest)
		{
		copyInto(dest);
		}
	}
