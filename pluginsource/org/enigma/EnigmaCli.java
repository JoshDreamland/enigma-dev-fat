/*
 * Copyright (C) 2011 IsmAvatar <IsmAvatar@gmail.com>
 * 
 * This file is part of Enigma Plugin.
 * Enigma Plugin is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */

package org.enigma;

import java.io.File;
import java.io.FileNotFoundException;

import org.enigma.TargetHandler.TargetSelection;
import org.enigma.backend.EnigmaCallbacks;
import org.enigma.backend.EnigmaDriver;
import org.enigma.backend.EnigmaSettings;
import org.enigma.backend.EnigmaStruct;
import org.enigma.backend.EnigmaCallbacks.OutputHandler;
import org.enigma.backend.EnigmaDriver.SyntaxError;
import org.lateralgm.components.impl.ResNode;
import org.lateralgm.file.GmFile;
import org.lateralgm.file.GmFileReader;
import org.lateralgm.file.GmFormatException;
import org.lateralgm.main.LGM;
import org.lateralgm.resources.Script;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.StringArray;

public class EnigmaCli
	{
	public static final String prog = "enigma"; //$NON-NLS-1$
	public static EnigmaDriver DRIVER;

	public static boolean syntax = false;

	public static void error(String err)
		{
		System.err.println(prog + ": " + err); //$NON-NLS-1$
		System.exit(-1);
		}

	public static void main(String[] args)
		{
		if (args.length != 1) error("no input file");

		if (args[0].equals("-?") || args[0].equals("--help"))
			{
			System.out.println("Fuck you too");
			System.exit(-1);
			}

		if (args[0].equals("-s") || args[0].equals("--syntax")) syntax = true;

		try
			{
			InitReturn r = initailize(args[0],null);
			EnigmaSettings es = initSettings();
			if (syntax)
				syntaxChecker(r.f,r.root,es);
			else
				compile(r.f,r.root,es);
			}
		catch (FileNotFoundException e)
			{
			error(e.getMessage());
			}
		catch (GmFormatException e)
			{
			error(e.getMessage());
			}
		}

	//	public static void compile(String fn) throws FileNotFoundException,GmFormatException
	//		{
	//		compile(fn,null);
	//		}
	//
	//	public static void compile(File file) throws FileNotFoundException,GmFormatException
	//		{
	//		compile(file.getPath(),null);
	//		}

	static class InitReturn
		{
		GmFile f;
		ResNode root;
		String error; //contains String on toolchain failure during libInit
		}

	public static InitReturn initailize(String fn, ResNode root) throws FileNotFoundException,
			GmFormatException
		{
		if (!new File(fn).exists()) throw new FileNotFoundException(fn);
		InitReturn r = new InitReturn();
		r.root = root == null ? new ResNode("Root",(byte) 0,null,null) : root; //$NON-NLS-1$;
		r.f = GmFileReader.readGmFile(fn,r.root);
		try
			{
			attemptLib();
			}
		catch (UnsatisfiedLinkError e)
			{
			error("Unable to communicate with the library,\n"
					+ "either because it could not be found or uses methods different from those expected.\n"
					+ "The exact error is:\n" + e.getMessage());
			}
		r.error = DRIVER.libInit(new EnigmaCallbacks(new CliOutputHandler())); //returns String on toolchain failure
		return r;
		}

	//TODO: Handle custom settings
	public static EnigmaSettings initSettings()
		{
		return new EnigmaSettings();
		}

	public static void syntaxChecker(GmFile f, ResNode root, EnigmaSettings ess)
		{
		SyntaxError se = syntaxCheck(f,root,ess);
		if (se == null || se.absoluteIndex == -1)
			System.out.println("No syntax errors found.");
		else
			error(se.line + ":" + se.position + "::" + se.errorString);
		}

	public static SyntaxError syntaxCheck(GmFile f, ResNode root, EnigmaSettings ess)
		{
		SyntaxError err = ess.commitToDriver(DRIVER); //returns SyntaxError
		if (err.absoluteIndex != -1) return err;

		int scrNum = LGM.currentFile.scripts.size();
		String osl[] = new String[scrNum];
		Script isl[] = LGM.currentFile.scripts.toArray(new Script[0]);
		for (int i = 0; i < scrNum; i++)
			osl[i] = isl[i].getName();
		StringArray scripts = new StringArray(osl);

		for (Script s : isl)
			if ((err = DRIVER.syntaxCheck(scrNum,scripts,s.getCode())).absoluteIndex != -1) return err;

		return null;
		}

	public static void compile(GmFile f, ResNode root, EnigmaSettings ess)
			throws FileNotFoundException,GmFormatException
		{
		ess.commitToDriver(DRIVER); //returns SyntaxError

		//Generate arguments for compile
		EnigmaStruct es = EnigmaWriter.prepareStruct(f,root);

		//XXX: Handle custom modes?
		int mode = EnigmaRunner.MODE_COMPILE;

		//TODO: Handle custom outname
		//FIXME: Make compliant with spec2
		File outname = new File(f.filename.substring(0,f.filename.lastIndexOf('.'))
				+ ess.targets.get("windowing").ext);
		TargetSelection compiler = ess.targets.get("compiler");
		if (!compiler.outputexe.equals("$tempfile")) //$NON-NLS-1$
			outname = new File(compiler.outputexe);

		DRIVER.compileEGMf(es,outname.getAbsolutePath(),mode);
		}

	private static UnsatisfiedLinkError attemptLib()
		{
		try
			{
			String lib = "compileEGMf"; //$NON-NLS-1$
			NativeLibrary.addSearchPath(lib,"."); //$NON-NLS-1$
			NativeLibrary.addSearchPath(lib,LGM.workDir.getParent());
			DRIVER = (EnigmaDriver) Native.loadLibrary(lib,EnigmaDriver.class);
			return null;
			}
		catch (UnsatisfiedLinkError e)
			{
			return e;
			}
		}

	public static class CliOutputHandler implements OutputHandler
		{
		@Override
		public void append(String s)
			{
			System.out.print(s);
			}

		@Override
		public void clear()
			{
			//clearing the terminal doesn't make sense
			}

		@Override
		public void open()
			{
			//terminal already open
			}

		@Override
		public void progress(int i)
			{
			//progress/tip not handled at this time
			}

		@Override
		public void tip(String s)
			{
			//progress/tip not handled at this time
			}
		}
	}
