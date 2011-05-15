/*
 * Copyright (C) 2010 IsmAvatar <IsmAvatar@gmail.com>
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
import java.util.Scanner;

import org.enigma.EnigmaRunner;
import org.enigma.TargetHandler;
import org.enigma.YamlParser;
import org.enigma.TargetHandler.TargetSelection;
import org.enigma.YamlParser.YamlElement;
import org.enigma.YamlParser.YamlNode;
import org.enigma.backend.EnigmaDriver.SyntaxError;

public class EnigmaSettings
	{
	//Compatibility / Progress options
	public int cppStrings = 0; // Defines what language strings are inherited from.               0 = GML,               1 = C
	public int cppOperators = 0; // Defines what language operators ++ and -- are inherited from. 0 = GML,               1 = C
	public int cppEquals = 0; // Defines whether = should be exclusively treated as a setter.     0 = GML (= or ==)      1 = C (= only)
	public int cppEscapes = 0; // Defines what language string escapes are inherited from.        0 = GML (#),           1 = ISO C (\n)
	public int literalHandling = 0; // Determines how literals are treated.                       0 = enigma::variant,   1 = C-scalar

	//Advanced options
	public int instanceTypes = 0; // Defines how to represent instances.           0 = Integer, 1 = Pointer
	public int storageClass = 0; // Determines how instances are stored in memory. 0 = Map,     1 = List,    2 = Array

	public String definitions = "", globalLocals = "";
	public String initialization = "", cleanup = "";

	public TargetSelection selCompiler, selPlatform, selGraphics, selAudio, selCollision, selWidgets;

	public EnigmaSettings()
		{
		this(true);
		}

	private EnigmaSettings(boolean load)
		{
		if (!load) return;

		loadDefinitions();

		selCompiler = TargetHandler.defCompiler;
		selPlatform = TargetHandler.defPlatform;
		selGraphics = TargetHandler.defGraphics;
		selAudio = TargetHandler.defAudio;
		selCollision = TargetHandler.defCollision;
		selWidgets = TargetHandler.defWidgets;
		}

	void loadDefinitions()
		{
		definitions = fileToString(new File(EnigmaRunner.WORKDIR,"definitions.h"));
		}

	public void saveDefinitions()
		{
		writeString(new File(EnigmaRunner.WORKDIR,"definitions.h"),definitions);
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

	private String toTargetYaml()
		{
		int[] setvals = { cppStrings,cppOperators,cppEscapes,cppEquals,literalHandling,instanceTypes,
				storageClass };
		String[] setnames = { "inherit-strings-from","inherit-increment-from","inherit-escapes-from",
				"inherit-equivalence-from","treat-literals-as","instance-types","storage-class" };

		StringBuilder general = new StringBuilder();
		for (int i = 0; i < setvals.length; i++)
			general.append(setnames[i]).append(": ").append(setvals[i]).append("\n");

		return "%e-yaml\n---\n"//
				+ general.toString() + "\n"//
				+ "target-compiler: " + (selCompiler == null ? "" : selCompiler.id) + "\n"//
				+ "target-windowing: " + (selPlatform == null ? "" : selPlatform.id) + "\n"//
				+ "target-graphics: " + (selGraphics == null ? "" : selGraphics.id) + "\n"//
				+ "target-audio: " + (selAudio == null ? "" : selAudio.id) + "\n"//
				+ "target-collision: " + (selCollision == null ? "" : selCollision.id) + "\n"//
				+ "target-widget: " + (selWidgets == null ? "" : selWidgets.id) + "\n"//
				+ "target-networking: " + "None" + "\n";//
		}

	public SyntaxError commitToDriver(EnigmaDriver driver)
		{
		return driver.definitionsModified(definitions,toTargetYaml());
		}

	public EnigmaSettings copy()
		{
		EnigmaSettings es = new EnigmaSettings(false);
		copyInto(es);
		return es;
		}

	public void copyInto(EnigmaSettings es)
		{
		es.cppStrings = cppStrings;
		es.cppOperators = cppOperators;
		es.cppEquals = cppEquals;
		es.cppEscapes = cppEscapes;
		es.literalHandling = literalHandling;

		es.instanceTypes = instanceTypes;
		es.storageClass = storageClass;

		es.definitions = definitions;
		es.globalLocals = globalLocals;
		es.initialization = initialization;
		es.cleanup = cleanup;

		es.selCompiler = selCompiler;
		es.selPlatform = selPlatform;
		es.selGraphics = selGraphics;
		es.selAudio = selAudio;
		es.selCollision = selCollision;
		es.selWidgets = selWidgets;
		}

	public static void main(String[] args) throws FileNotFoundException
		{
		YamlNode n = YamlParser.parse(new Scanner(new File("/home/ismavatar/test/settings.ey")));
		for (YamlElement e : n.chronos)
			System.out.println(e);
		}
	}
