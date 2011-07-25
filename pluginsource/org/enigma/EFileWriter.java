/*
 * Copyright (C) 2011 IsmAvatar <IsmAvatar@gmail.com>
 * 
 * This file is part of Enigma Plugin.
 * Enigma Plugin is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */

package org.enigma;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.enigma.messages.Messages;
import org.enigma.utility.APNGExperiments;
import org.lateralgm.components.impl.ResNode;
import org.lateralgm.file.GmFileReader;
import org.lateralgm.file.GmFormatException;
import org.lateralgm.file.GmStreamEncoder;
import org.lateralgm.main.Util;
import org.lateralgm.resources.Background;
import org.lateralgm.resources.GmObject;
import org.lateralgm.resources.Path;
import org.lateralgm.resources.Resource;
import org.lateralgm.resources.ResourceReference;
import org.lateralgm.resources.Room;
import org.lateralgm.resources.Script;
import org.lateralgm.resources.Sound;
import org.lateralgm.resources.Sprite;
import org.lateralgm.resources.Resource.Kind;
import org.lateralgm.resources.Sound.PSound;
import org.lateralgm.resources.library.LibAction;
import org.lateralgm.resources.library.LibManager;
import org.lateralgm.resources.sub.Action;
import org.lateralgm.resources.sub.Argument;
import org.lateralgm.resources.sub.BackgroundDef;
import org.lateralgm.resources.sub.Event;
import org.lateralgm.resources.sub.Instance;
import org.lateralgm.resources.sub.MainEvent;
import org.lateralgm.resources.sub.PathPoint;
import org.lateralgm.resources.sub.Tile;
import org.lateralgm.resources.sub.View;
import org.lateralgm.resources.sub.BackgroundDef.PBackgroundDef;
import org.lateralgm.resources.sub.Instance.PInstance;
import org.lateralgm.resources.sub.Tile.PTile;
import org.lateralgm.resources.sub.View.PView;
import org.lateralgm.util.PropertyMap;

public class EFileWriter
	{
	public static final String EY = ".ey"; //$NON-NLS-1$

	// Modularity Classes
	public static abstract class EGMOutputStream
		{
		protected OutputStream last;

		public OutputStream next(final List<String> directory, String filename) throws IOException
			{
			if (last != null) _finishLast();
			return last = _next(directory,filename);
			}

		protected abstract OutputStream _next(final List<String> directory, String filename)
				throws IOException;

		protected abstract void _finishLast() throws IOException;

		public abstract void finish() throws IOException;

		public void close() throws IOException
			{
			if (last == null) return;
			_finishLast();
			last.close();
			last = null;
			}
		}

	public static class EGMZip extends EGMOutputStream
		{
		public static final String SEPARATOR = "/"; //$NON-NLS-1$

		protected ZipOutputStream os;

		public EGMZip(OutputStream os)
			{
			this(new ZipOutputStream(os));
			}

		public EGMZip(ZipOutputStream os)
			{
			this.os = os;
			last = new PrintStream(os);
			}

		@Override
		protected void _finishLast() throws IOException
			{
			last.flush();
			}

		@Override
		protected OutputStream _next(List<String> directory, String filename) throws IOException
			{
			StringBuilder path = new StringBuilder();
			for (String s : directory)
				path.append(s).append(SEPARATOR);
			path.append(filename);
			os.putNextEntry(new ZipEntry(path.toString()));
			return last;
			}

		@Override
		public void finish() throws IOException
			{
			last.flush();
			os.finish();
			}
		}

	public static class EGMFolder extends EGMOutputStream
		{
		File root;

		public EGMFolder(File root)
			{
			this.root = root;
			if (!root.mkdir())
			/* FIXME: Unable to mkdir, what do? */;
			}

		@Override
		protected void _finishLast() throws IOException
			{
			last.close();
			}

		@Override
		protected OutputStream _next(List<String> directory, String filename) throws IOException
			{
			File f = root;
			for (String s : directory)
				f = new File(root,s);
			return new FileOutputStream(new File(f,filename));
			}

		@Override
		public void finish() throws IOException
			{
			last.close();
			}
		}

	/**
	 * Interface for registering a method for writing for a kind of resource. An
	 * inheritor would get mapped to a Kind via the <code>writers</code> map.
	 */
	public static interface ResourceWriter
		{
		public void write(EGMOutputStream os, ResNode child, List<String> dir) throws IOException;
		}

	/**
	 * Convenience wrapper module manager for standard resources. A standard
	 * resource will have an ey properties file which points to a data file with
	 * a designated extension. This eliminates the need to deal with ResNodes
	 * and Zip technicalities and just focus on writing the respective files.
	 */
	public static abstract class DataResourceWriter implements ResourceWriter
		{
		public void write(EGMOutputStream os, ResNode child, List<String> dir) throws IOException
			{
			String name = (String) child.getUserObject();
			Resource<?,?> r = (Resource<?,?>) Util.deRef((ResourceReference<?>) child.getRes());
			String fn = name + getExt(r);

			PrintStream ps = new PrintStream(os.next(dir,name + EY));
			ps.println("Data: " + fn); //$NON-NLS-1$
			writeProperties(ps,r);

			writeData(os.next(dir,fn),r);
			}

		/**
		 * The extension, which must include the preceding dot, e.g. ".ext".
		 * Resource provided in case extension is chosen on an individual
		 * resource basis.
		 */
		public abstract String getExt(Resource<?,?> r);

		public abstract void writeProperties(PrintStream os, Resource<?,?> r) throws IOException;

		public abstract void writeData(OutputStream os, Resource<?,?> r) throws IOException;
		}

	/**
	 * Convenience wrapper for automatically writing allowed properties.
	 * 
	 * @see DataResourceWriter
	 */
	static abstract class DataPropWriter extends DataResourceWriter
		{
		@Override
		public void writeProperties(PrintStream os, Resource<?,?> r) throws IOException
			{
			PropertyMap<? extends Enum<?>> p = r.properties;
			for (Entry<? extends Enum<?>,Object> e : p.entrySet())
				if (allowProperty(e.getKey().name())) os.println(e.getKey().name() + ": " + e.getValue()); //$NON-NLS-1$
			}

		/**
		 * Returns whether the following property should be allowed in the
		 * properties file
		 */
		public abstract boolean allowProperty(String name);
		}

	// Module maps
	/** Used to register writers with their resource kinds. */
	static Map<Kind,ResourceWriter> writers = new HashMap<Kind,ResourceWriter>();
	static Map<Kind,String> typestrs = new HashMap<Kind,String>();
	static
		{
		// SPRITE,SOUND,BACKGROUND,PATH,SCRIPT,FONT,TIMELINE,OBJECT,ROOM,GAMEINFO,GAMESETTINGS,EXTENSIONS
		writers.put(Kind.SPRITE,new SpriteApngIO());
		writers.put(Kind.SOUND,new SoundIO());
		writers.put(Kind.BACKGROUND,new BackgroundIO());
		writers.put(Kind.PATH,new PathTextIO());
		writers.put(Kind.SCRIPT,new ScriptIO());
		writers.put(Kind.FONT,new FontRawIO());
		// writers.put(Kind.TIMELINE,new TimelineIO());
		writers.put(Kind.OBJECT,new ObjectIO());
		writers.put(Kind.ROOM,new RoomGmDataIO());
		// TODO: MOAR

		// Unused for now
		/*
		 * typestrs.put(Kind.SPRITE,"spr"); //$NON-NLS-1$
		 * typestrs.put(Kind.SOUND,"snd"); //$NON-NLS-1$
		 * typestrs.put(Kind.BACKGROUND,"bkg"); //$NON-NLS-1$
		 * typestrs.put(Kind.PATH,"pth"); //$NON-NLS-1$
		 * typestrs.put(Kind.SCRIPT,"scr"); //$NON-NLS-1$
		 * typestrs.put(Kind.FONT,"fnt"); //$NON-NLS-1$
		 * typestrs.put(Kind.TIMELINE,"tml"); //$NON-NLS-1$
		 * typestrs.put(Kind.OBJECT,"obj"); //$NON-NLS-1$
		 * typestrs.put(Kind.ROOM,"rom"); //$NON-NLS-1$
		 * typestrs.put(Kind.GAMEINFO,"inf"); //$NON-NLS-1$
		 * typestrs.put(Kind.GAMESETTINGS,"set"); //$NON-NLS-1$
		 * typestrs.put(Kind.EXTENSIONS,"ext"); //$NON-NLS-1$
		 */
		}

	// Constructors
	public static void writeEgmFile(File loc, ResNode tree, boolean zip)
		{
		try
			{
			EGMOutputStream out;
			if (zip)
				out = new EGMZip(new FileOutputStream(loc));
			else
				out = new EGMFolder(loc);
			writeEgmFile(out,tree);
			out.close();
			}
		catch (IOException e)
			{
			// TODO Auto-generated catch block
			e.printStackTrace();
			}
		}

	public static void writeEgmFile(EGMOutputStream os, ResNode tree) throws IOException
		{
		writeNodeChildren(os,tree,new ArrayList<String>());
		}

	// Workhorse methods
	/** Recursively writes out the tree nodes into the zip. */
	public static void writeNodeChildren(EGMOutputStream os, ResNode node, List<String> dir)
			throws IOException
		{
		PrintStream ps = new PrintStream(os.next(dir,"toc.txt")); //$NON-NLS-1$

		int children = node.getChildCount();
		for (int i = 0; i < children; i++)
			{
			ResNode child = (ResNode) node.getChildAt(i);
			ps.println((String) child.getUserObject());
			}

		for (int i = 0; i < children; i++)
			{
			ResNode child = ((ResNode) node.getChildAt(i));

			if (child.status == ResNode.STATUS_SECONDARY)
				{
				writeResource(os,child,dir);
				}
			else
				{
				String cn = (String) child.getUserObject();
				// String cdir = dir + child.getUserObject() + SEPARATOR;
				ArrayList<String> newDir = new ArrayList<String>(dir);
				newDir.add(cn);
				// os.next(newDir, null);
				writeNodeChildren(os,child,newDir);
				}
			}
		}

	/**
	 * Looks up the registered writer for this resource and invokes the write
	 * method
	 */
	public static void writeResource(EGMOutputStream os, ResNode child, List<String> dir)
			throws IOException
		{
		ResourceWriter writer = writers.get(child.kind);
		if (writer == null)
			{
			System.err.println(Messages.format("EFileWriter.NO_WRITER",child.kind)); //$NON-NLS-1$
			return;
			}
		writer.write(os,child,dir);
		}

	// Modules
	// SPRITE,SOUND,BACKGROUND,PATH,SCRIPT,FONT,TIMELINE,OBJECT,ROOM,GAMEINFO,GAMESETTINGS,EXTENSIONS

	static class SpriteApngIO extends DataPropWriter
		{
		@Override
		public String getExt(Resource<?,?> r)
			{
			return ".apng"; //$NON-NLS-1$
			}

		@Override
		public void writeData(OutputStream os, Resource<?,?> r) throws IOException
			{
			ArrayList<BufferedImage> subs = ((Sprite) r).subImages;
			APNGExperiments.imagesToApng(subs,os);
			}

		@Override
		public boolean allowProperty(String name)
			{
			return true;
			}
		}

	static class SoundIO extends DataPropWriter
		{
		@Override
		public String getExt(Resource<?,?> r)
			{
			return ((Sound) r).properties.get(PSound.FILE_TYPE);
			}

		@Override
		public void writeData(OutputStream os, Resource<?,?> r) throws IOException
			{
			os.write(((Sound) r).data);
			}

		@Override
		public boolean allowProperty(String name)
			{
			return true;
			}
		}

	static class BackgroundIO extends DataPropWriter
		{
		@Override
		public String getExt(Resource<?,?> r)
			{
			return ".png"; //$NON-NLS-1$
			}

		@Override
		public void writeData(OutputStream os, Resource<?,?> r) throws IOException
			{
			ImageIO.write(((Background) r).getBackgroundImage(),"png",os); //$NON-NLS-1$
			}

		@Override
		public boolean allowProperty(String name)
			{
			return true;
			}
		}

	static class PathTextIO extends DataPropWriter
		{
		@Override
		public String getExt(Resource<?,?> r)
			{
			return ".pth"; //$NON-NLS-1$
			}

		@Override
		public void writeData(OutputStream os, Resource<?,?> r) throws IOException
			{
			PrintStream ps = new PrintStream(os);
			// space delimited, EOF terminated
			for (PathPoint p : ((Path) r).points)
				{
				ps.print(p.getX());
				ps.print(' ');
				ps.print(p.getY());
				ps.print(' ');
				ps.println(p.getSpeed());
				}
			}

		@Override
		public boolean allowProperty(String name)
			{
			return true;
			}
		}

	static class ScriptIO extends DataPropWriter
		{
		@Override
		public String getExt(Resource<?,?> r)
			{
			return ".scr"; //$NON-NLS-1$
			}

		@Override
		public void writeData(OutputStream os, Resource<?,?> r) throws IOException
			{
			os.write(((Script) r).getCode().getBytes()); // charset?
			}

		@Override
		public boolean allowProperty(String name)
			{
			return false;
			}
		}

	static class FontRawIO implements ResourceWriter
		{
		public void write(EGMOutputStream os, ResNode child, List<String> dir) throws IOException
			{
			String name = (String) child.getUserObject();
			Resource<?,?> r = (Resource<?,?>) Util.deRef((ResourceReference<?>) child.getRes());

			writeProperties(new PrintStream(os.next(dir,name + EY)),r);
			}

		public void writeProperties(PrintStream os, Resource<?,?> r) throws IOException
			{
			PropertyMap<? extends Enum<?>> p = r.properties;
			for (Entry<? extends Enum<?>,Object> e : p.entrySet())
				if (allowProperty(e.getKey().name())) os.println(e.getKey().name() + ": " + e.getValue()); //$NON-NLS-1$
			}

		public boolean allowProperty(String name)
			{
			return true;
			}
		}

	static class ObjectIO extends DataPropWriter
		{
		@Override
		public String getExt(Resource<?,?> r)
			{
			return ".obj"; //$NON-NLS-1$
			}

		@Override
		public void writeData(OutputStream os, Resource<?,?> r) throws IOException
			{
			GmObject obj = (GmObject) r;
			PrintStream ps = new PrintStream(os);
			int numEvents = 0;

			// Write the Events super-key
			for (MainEvent me : obj.mainEvents)
				numEvents += me.events.size();
			ps.println("Events{" + numEvents + "}");

			// Write the events
			for (MainEvent me : obj.mainEvents)
				for (Event ev : me.events)
					{
					ps.println("  Event (" + ev.id + "," + ev.mainId + "): " + "Actions{" + ev.actions.size()
							+ "}");
					for (Action action : ev.actions)
						{
						LibAction la = action.getLibAction();
						ps.print("    Action (" + la.id + "," + la.parentId + "): ");
						if (action.isNot()) ps.print("\"Not\" ");
						if (action.isRelative()) ps.print("\"Relative\" ");
						if (action.getAppliesTo().get() != null)
							ps.print("Applies(" + action.getAppliesTo().get() + ") ");
						if (la.interfaceKind != LibAction.INTERFACE_CODE)
							{
							ps.println("Fields[" + action.getArguments().size() + "]");
							for (Argument arg : action.getArguments())
								ps.println("      " + arg.getVal());
							}
						else
							{
							// note: check size first
							String code = action.getArguments().get(0).getVal();
							ps.println("Code[" + code.split("\r\n|\r|\n").length + " lines]");
							ps.println(code);
							}
						}
					}
			}

		@Override
		public boolean allowProperty(String name)
			{
			return true;
			}
		}

	static class RoomGmDataIO extends DataPropWriter
		{
		@Override
		public String getExt(Resource<?,?> r)
			{
			return ".rmg"; //$NON-NLS-1$
			}

		@Override
		public void writeData(OutputStream os, Resource<?,?> r) throws IOException
			{
			Room rm = (Room) r;
			GmStreamEncoder out = new GmStreamEncoder(os);
			out.write4(rm.backgroundDefs.size());
			for (BackgroundDef back : rm.backgroundDefs)
				{
				out.writeBool(back.properties,PBackgroundDef.VISIBLE,PBackgroundDef.FOREGROUND);
				out.writeId((ResourceReference<?>) back.properties.get(PBackgroundDef.BACKGROUND));
				out.write4(back.properties,PBackgroundDef.X,PBackgroundDef.Y);
				out.writeBool(back.properties,PBackgroundDef.TILE_HORIZ,PBackgroundDef.TILE_VERT);
				out.write4(back.properties,PBackgroundDef.H_SPEED,PBackgroundDef.V_SPEED);
				out.writeBool(back.properties,PBackgroundDef.STRETCH);
				}
			// out.writeBool(rm.properties,PRoom.ENABLE_VIEWS);
			out.write4(rm.views.size());
			for (View view : rm.views)
				{
				out.writeBool(view.properties,PView.VISIBLE);
				out.write4(view.properties,PView.VIEW_X,PView.VIEW_Y,PView.VIEW_W,PView.VIEW_H,
						PView.PORT_X,PView.PORT_Y,PView.PORT_W,PView.PORT_H,PView.BORDER_H,PView.BORDER_V,
						PView.SPEED_H,PView.SPEED_V);
				out.writeId((ResourceReference<?>) view.properties.get(PView.OBJECT));
				}
			out.write4(rm.instances.size());
			for (Instance in : rm.instances)
				{
				out.write4(in.getPosition().x);
				out.write4(in.getPosition().y);
				ResourceReference<GmObject> or = in.properties.get(PInstance.OBJECT);
				out.writeId(or);
				out.write4((Integer) in.properties.get(PInstance.ID));
				out.writeStr(in.getCreationCode());
				out.writeBool(in.isLocked());
				}
			out.write4(rm.tiles.size());
			for (Tile tile : rm.tiles)
				{
				out.write4(tile.getRoomPosition().x);
				out.write4(tile.getRoomPosition().y);
				ResourceReference<Background> rb = tile.properties.get(PTile.BACKGROUND);
				out.writeId(rb);
				out.write4(tile.getBackgroundPosition().x);
				out.write4(tile.getBackgroundPosition().y);
				out.write4(tile.getSize().width);
				out.write4(tile.getSize().height);
				out.write4(tile.getDepth());
				out.write4((Integer) tile.properties.get(PTile.ID));
				out.writeBool(tile.isLocked());
				}
			out.flush();
			}

		@Override
		public boolean allowProperty(String name)
			{
			return true;
			}
		}

	// TODO: MOAR MODULES

	public static void main(String[] args) throws GmFormatException
		{
		File home = new File(System.getProperty("user.home")); //$NON-NLS-1$

		File in = new File(home,"inputGmFile.gm81"); // any of gmd,gm6,gmk,gm81
		File out = new File(home,"outputEgmFile.egm"); // must be egm

		LibManager.autoLoad();
		ResNode root = new ResNode("Root",(byte) 0,null,null); //$NON-NLS-1$;
		GmFileReader.readGmFile(in.getPath(),root);

		writeEgmFile(out,root,true);
		}
	}
