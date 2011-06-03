package org.enigma;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.lateralgm.components.impl.ResNode;
import org.lateralgm.main.LGM;
import org.lateralgm.main.Util;
import org.lateralgm.resources.Resource;
import org.lateralgm.resources.ResourceReference;
import org.lateralgm.resources.Script;
import org.lateralgm.resources.Resource.Kind;

public class EFileWriter
	{
	public static final String EY = ".ey";

	//Modularity Classes
	/**
	 * Convenience wrapper to allow writing buffered text to a zip.
	 * Always invoke "next" prior to attempting to write any data.
	 */
	public static class ZipOutputWriter extends BufferedWriter
		{
		protected ZipOutputStream os;

		public ZipOutputWriter(OutputStream os)
			{
			this(new ZipOutputStream(os));
			}

		public ZipOutputWriter(ZipOutputStream os)
			{
			super(new OutputStreamWriter(os));
			this.os = os;
			}

		public void next(String next) throws IOException
			{
			flush();
			os.putNextEntry(new ZipEntry(next));
			}
		}

	/**
	 * Interface for registering a method for writing for a kind of resource.
	 * An inheritor would get mapped to a Kind via the <code>writers</code> map.
	 */
	public static interface ResourceWriter
		{
		public void write(ZipOutputWriter os, ResNode child, String dir) throws IOException;
		}

	/**
	 * Convenience wrapper module manager for standard resources.
	 * A standard resource will have an ey properties file which
	 * points to a data file with a designated extension.
	 * This eliminates the need to deal with ResNodes and Zip technicalities
	 * and just focus on writing the respective files.
	 */
	public static abstract class DataResourceWriter implements ResourceWriter
		{
		public void write(ZipOutputWriter os, ResNode child, String dir) throws IOException
			{
			String name = (String) child.getUserObject();
			Resource<?,?> r = (Resource<?,?>) Util.deRef((ResourceReference<?>) child.getRes());

			os.next(dir + name + EY);
			os.write("Data: " + name + getExt());
			writeProperties(os,r);

			os.next(dir + name + getExt());
			writeData(os,r);
			}

		public abstract String getExt();

		public abstract void writeProperties(ZipOutputWriter os, Resource<?,?> r) throws IOException;

		public abstract void writeData(ZipOutputWriter os, Resource<?,?> r) throws IOException;
		}

	//Module maps
	/** Used to register writers with their resource kinds. */
	static Map<Kind,ResourceWriter> writers = new HashMap<Kind,ResourceWriter>();
	static Map<Kind,String> typestrs = new HashMap<Kind,String>();
	static
		{
		writers.put(Kind.SCRIPT,new ScriptIO());

		typestrs.put(Kind.SPRITE,"spr");
		typestrs.put(Kind.SOUND,"snd");
		typestrs.put(Kind.BACKGROUND,"bkg");
		typestrs.put(Kind.PATH,"pth");
		typestrs.put(Kind.SCRIPT,"scr");
		typestrs.put(Kind.FONT,"fnt");
		typestrs.put(Kind.TIMELINE,"tml");
		typestrs.put(Kind.OBJECT,"obj");
		typestrs.put(Kind.ROOM,"rom");
		typestrs.put(Kind.GAMEINFO,"inf");
		typestrs.put(Kind.GAMESETTINGS,"set");
		typestrs.put(Kind.EXTENSIONS,"ext");
		}

	//Constructors
	public void writeEgmFile(File loc)
		{
		try
			{
			FileOutputStream fos = new FileOutputStream(loc);
			writeEgmFile(fos);
			fos.close();
			}
		catch (IOException e)
			{
			// TODO Auto-generated catch block
			e.printStackTrace();
			}
		}

	public void writeEgmFile(OutputStream os) throws IOException
		{
		writeEgmFile(new ZipOutputWriter(os));
		}

	public void writeEgmFile(ZipOutputWriter os) throws IOException
		{
		writeNodeChildren(os,LGM.root,"");
		os.flush();
		}

	//Workhorse methods
	/** Recursively writes out the tree nodes into the zip. */
	public void writeNodeChildren(ZipOutputWriter os, ResNode node, String dir) throws IOException
		{
		os.next(dir + "toc.txt");

		int children = node.getChildCount();
		for (int i = 0; i < children; i++)
			{
			ResNode child = (ResNode) node.getChildAt(i);
			os.write((String) child.getUserObject());
			os.newLine();
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
				String cdir = dir + child.getUserObject() + "/";
				os.next(cdir);
				writeNodeChildren(os,child,cdir);
				}
			}
		}

	/** Looks up the registered writer for this resource and invokes the write method */
	public void writeResource(ZipOutputWriter os, ResNode child, String dir) throws IOException
		{
		ResourceWriter writer = writers.get(child.kind);
		if (writer == null)
			{
			System.err.println("No registered writers for resource of kind " + child.kind);
			return;
			}
		writer.write(os,child,dir);
		}

	//Modules
	static class ScriptIO extends DataResourceWriter
		{
		@Override
		public String getExt()
			{
			return ".scr";
			}

		@Override
		public void writeData(ZipOutputWriter os, Resource<?,?> r) throws IOException
			{
			os.write(((Script) r).getCode());
			}

		@Override
		public void writeProperties(ZipOutputWriter os, Resource<?,?> r) throws IOException
			{
			}
		}

	/////////////////////////////////////////////////
	// Below are experimental APNG support methods //
	/////////////////////////////////////////////////

	APNG readApng(File f) throws IOException
		{
		return readApng(new FileInputStream(f));
		}

	static final long PNG_MAGIC = 0x89504E470D0A1A0AL;
	static final int IHDR_TYPE = 0x49484452;
	static final int IEND_TYPE = 0x49454E44;
	static final int IDAT_TYPE = 0x49444154;

	static final int acTL_TYPE = 0x6163544C;
	static final int fcTL_TYPE = 0x6663544C;
	static final int fdAT_TYPE = 0x66644154;

	APNG readApng(InputStream is) throws IOException
		{
		return null;
		}

	/**
	 * This method attempts to read an animated PNG by disassembling and then repeatedly gluing for ImageIO.
	 * Basically, we pull out the aCTL chunk, and every other DAT chunk except the one we're parsing.
	 * We then repeat this process for each DAT chunk.
	 * @param is
	 * @throws IOException
	 */
	APNG readApngHack(InputStream is) throws IOException
		{
		DataInputStream in = new DataInputStream(is);
		long magic = in.readLong();
		if (magic != PNG_MAGIC) throw new IOException(String.format("%016X",magic) + " != png");

		Chunk c = readChunk(in);
		if (c.id != IHDR_TYPE) throw new IOException(String.format("%08X",c.id) + " != IHDR");

		IHDR h = new IHDR();
		DataInputStream d = new DataInputStream(c.data);
		h.width = d.readInt();
		h.height = d.readInt();
		h.bitDepth = d.readByte();
		h.colType = d.readByte();
		h.compression = d.readByte();
		h.filter = d.readByte();
		h.interlace = d.readByte();
		d.reset();

		APNG r = new APNG(h);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeLong(PNG_MAGIC);
		byte[] buf = new byte[d.available()];
		d.read(buf);
		dos.write(buf);

		while ((c = readChunk(in)).id != IEND_TYPE)
			{
			switch (c.id)
				{
				//			case
				}
			}

		return r;
		}

	Chunk readChunk(DataInputStream in) throws IOException
		{
		int size = in.readInt();
		byte[] buf = new byte[size + 4];
		in.read(buf);
		int crc1 = crc(buf), crc2 = in.readInt();
		if (crc1 != crc2) throw new IOException("CRC " + crc1 + " != " + crc2);
		ByteArrayInputStream data = new ByteArrayInputStream(buf);

		byte[] bid = new byte[4];
		data.read(bid);
		int id = (bid[0] << 24) | (bid[1] << 16) | (bid[2] << 8) | bid[3];
		return new Chunk(size,id,data);
		}

	class APNG extends ArrayList<BufferedImage>
		{
		private static final long serialVersionUID = 1L;
		IHDR header;

		public APNG(IHDR h)
			{
			header = h;
			}
		}

	class Chunk
		{
		int size, id;
		InputStream data;

		Chunk(int s, int i, InputStream d)
			{
			size = s;
			id = i;
			data = d;
			}
		}

	class IHDR
		{
		int width, height;
		byte bitDepth, colType, compression, filter, interlace;
		}

	static int crc(byte[] b)
		{
		CRC32 crc = new CRC32();
		crc.update(b);
		return (int) crc.getValue();
		}
	}
