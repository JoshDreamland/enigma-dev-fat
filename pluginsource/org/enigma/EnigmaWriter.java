/*
 * Copyright (C) 2008, 2009 IsmAvatar <IsmAvatar@gmail.com>
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

import static org.lateralgm.main.Util.deRef;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

import javax.swing.JOptionPane;

import org.enigma.backend.EnigmaStruct;
import org.enigma.backend.other.Constant;
import org.enigma.backend.other.Extension;
import org.enigma.backend.other.Include;
import org.enigma.backend.resources.Background;
import org.enigma.backend.resources.Font;
import org.enigma.backend.resources.GmObject;
import org.enigma.backend.resources.Path;
import org.enigma.backend.resources.Room;
import org.enigma.backend.resources.Script;
import org.enigma.backend.resources.Sound;
import org.enigma.backend.resources.Sprite;
import org.enigma.backend.resources.Timeline;
import org.enigma.backend.sub.BackgroundDef;
import org.enigma.backend.sub.Event;
import org.enigma.backend.sub.Glyph;
import org.enigma.backend.sub.Instance;
import org.enigma.backend.sub.MainEvent;
import org.enigma.backend.sub.Moment;
import org.enigma.backend.sub.PathPoint;
import org.enigma.backend.sub.SubImage;
import org.enigma.backend.sub.Tile;
import org.enigma.backend.sub.View;
import org.enigma.backend.util.Image;
import org.enigma.backend.util.Point;
import org.enigma.backend.util.Polygon;
import org.enigma.utility.Masker.Mask;
import org.lateralgm.components.impl.ResNode;
import org.lateralgm.file.GmFile;
import org.lateralgm.resources.ResourceReference;
import org.lateralgm.resources.Background.PBackground;
import org.lateralgm.resources.Font.PFont;
import org.lateralgm.resources.GmObject.PGmObject;
import org.lateralgm.resources.Path.PPath;
import org.lateralgm.resources.Room.PRoom;
import org.lateralgm.resources.Script.PScript;
import org.lateralgm.resources.Sound.PSound;
import org.lateralgm.resources.Sound.SoundKind;
import org.lateralgm.resources.Sprite.BBMode;
import org.lateralgm.resources.Sprite.MaskShape;
import org.lateralgm.resources.Sprite.PSprite;
import org.lateralgm.resources.library.LibAction;
import org.lateralgm.resources.library.LibManager;
import org.lateralgm.resources.library.Library;
import org.lateralgm.resources.sub.Action;
import org.lateralgm.resources.sub.ActionContainer;
import org.lateralgm.resources.sub.Argument;
import org.lateralgm.resources.sub.BackgroundDef.PBackgroundDef;
import org.lateralgm.resources.sub.Instance.PInstance;
import org.lateralgm.resources.sub.Tile.PTile;
import org.lateralgm.resources.sub.View.PView;

import com.sun.xml.internal.bind.marshaller.Messages;

public final class EnigmaWriter
	{
	protected GmFile i;
	protected EnigmaStruct o;
	protected ResNode root;

	private EnigmaWriter(GmFile in, EnigmaStruct out, ResNode root)
		{
		i = in;
		o = out;
		this.root = root;
		}

	public static EnigmaStruct prepareStruct(GmFile f, ResNode root)
		{
		return new EnigmaWriter(f,new EnigmaStruct(),root).populateStruct();
		}

	protected EnigmaStruct populateStruct()
		{
		o.fileVersion = i.fileVersion;
		o.filename = i.filename;

		populateSprites();
		populateSounds();
		populateBackgrounds();
		populatePaths();
		populateScripts();
		populateFonts();
		populateTimelines();
		populateObjects();
		populateRooms();

		o.triggerCount = 0;

		o.constantCount = i.constants.size();
		if (o.constantCount != 0)
			{
			o.constants = new Constant.ByReference();
			Constant[] ocl = (Constant[]) o.constants.toArray(o.constantCount);
			for (int c = 0; c < o.constantCount; c++)
				{
				ocl[c].name = i.constants.get(c).name;
				ocl[c].value = i.constants.get(c).value;
				}
			}

		o.includeCount = i.includes.size();
		if (o.includeCount != 0)
			{
			o.includes = new Include.ByReference();
			Include[] oil = (Include[]) o.includes.toArray(o.includeCount);
			for (int inc = 0; inc < o.includeCount; inc++)
				{
				oil[inc].filepath = i.includes.get(inc).filepath;
				}
			}

		o.packageCount = 0;
		// o.packageCount = packages.length;
		// o.packages = new StringArray(packages);

		o.extensionCount = 1;
		o.extensions = new Extension.ByReference();
		Extension[] oix = (Extension[]) o.extensions.toArray(o.extensionCount);
		oix[0].name = "Alarms";
		oix[0].path = "Universal_System/Extensions";

		o.lastInstanceId = i.lastInstanceId;
		o.lastTileId = i.lastTileId;

		return o;
		}

	protected static final SoundKind[] SOUND_KIND = { SoundKind.NORMAL,SoundKind.BACKGROUND,
			SoundKind.SPATIAL,SoundKind.MULTIMEDIA };
	protected static final Map<SoundKind,Integer> SOUND_CODE;
	static
		{
		EnumMap<SoundKind,Integer> m = new EnumMap<SoundKind,Integer>(SoundKind.class);
		for (int i = 0; i < SOUND_KIND.length; i++)
			m.put(SOUND_KIND[i],i);
		SOUND_CODE = Collections.unmodifiableMap(m);
		}
	protected static final BBMode[] SPRITE_BB_MODE = { BBMode.AUTO,BBMode.FULL,BBMode.MANUAL };
	protected static final Map<BBMode,Integer> SPRITE_BB_CODE;
	static
		{
		EnumMap<BBMode,Integer> m = new EnumMap<BBMode,Integer>(BBMode.class);
		for (int i = 0; i < SPRITE_BB_MODE.length; i++)
			m.put(SPRITE_BB_MODE[i],i);
		SPRITE_BB_CODE = Collections.unmodifiableMap(m);
		}
	protected static final MaskShape[] SPRITE_MASK_SHAPE = { MaskShape.PRECISE,MaskShape.RECTANGLE,
			MaskShape.DISK,MaskShape.DIAMOND };
	protected static final Map<MaskShape,Integer> SPRITE_MASK_CODE;
	static
		{
		EnumMap<MaskShape,Integer> m = new EnumMap<MaskShape,Integer>(MaskShape.class);
		for (int i = 0; i < SPRITE_MASK_SHAPE.length; i++)
			m.put(SPRITE_MASK_SHAPE[i],i);
		SPRITE_MASK_CODE = Collections.unmodifiableMap(m);
		}

	protected void populateSprites()
		{
		int size = i.sprites.size();
		o.spriteCount = size;
		if (size == 0) return;

		o.sprites = new Sprite.ByReference();
		Sprite[] osl = (Sprite[]) o.sprites.toArray(size);
		org.lateralgm.resources.Sprite[] isl = i.sprites.toArray(new org.lateralgm.resources.Sprite[0]);
		for (int s = 0; s < size; s++)
			{
			Sprite os = osl[s];
			org.lateralgm.resources.Sprite is = isl[s];

			os.name = is.getName();
			os.id = is.getId();

			os.transparent = is.get(PSprite.TRANSPARENT);
			os.shape = SPRITE_MASK_CODE.get(is.get(PSprite.SHAPE)); //0*=Precise, 1=Rectangle,  2=Disk, 3=Diamond
			os.alphaTolerance = is.get(PSprite.ALPHA_TOLERANCE);
			os.separateMask = is.get(PSprite.SEPARATE_MASK);
			os.smoothEdges = is.get(PSprite.SMOOTH_EDGES);
			os.preload = is.get(PSprite.PRELOAD);
			os.originX = is.get(PSprite.ORIGIN_X);
			os.originY = is.get(PSprite.ORIGIN_Y);
			os.bbMode = SPRITE_BB_CODE.get(is.get(PSprite.BB_MODE)); //0*=Automatic, 1=Full image, 2=Manual
			os.bbLeft = is.get(PSprite.BB_LEFT);
			os.bbRight = is.get(PSprite.BB_RIGHT);
			os.bbTop = is.get(PSprite.BB_TOP);
			os.bbBottom = is.get(PSprite.BB_BOTTOM);

			os.subImageCount = is.subImages.size();
			if (os.subImageCount == 0) continue;

			os.subImages = new SubImage.ByReference();
			SubImage[] osil = (SubImage[]) os.subImages.toArray(os.subImageCount);
			for (int i = 0; i < os.subImageCount; i++)
				{
				BufferedImage img = is.subImages.get(i);
				osil[i].image = new Image.ByReference();
				populateImage(img,osil[i].image,os.transparent);

				//for now, polygon masking is disabled
				if (true) osil[i].maskShapeCount = 0;
				if (osil[i].maskShapeCount == 0) continue;

				//Individual SubImage Polygon Masking
				int w = img.getWidth();
				int h = img.getHeight();
				int pixels[] = img.getRGB(0,0,w,h,null,0,w);
				Mask m = new Mask(pixels,w,h);

				osil[i].maskShapeCount = m.pts.size();
				if (osil[i].maskShapeCount == 0) continue;

				osil[i].maskShapes = new Polygon.ByReference();
				Polygon[] opl = (Polygon[]) osil[i].maskShapes.toArray(osil[i].maskShapeCount);

				//populate each polygon
				for (int j = 0; j < opl.length; j++)
					{
					System.out.println("Populating polygon " + j);
					List<java.awt.Point> ippl = m.getRayOutline(m.pts.get(j));
					opl[j].pointCount = ippl.size();
					if (opl[j].pointCount == 0) continue;

					System.out.println("Test1");

					opl[j].points = new Point.ByReference();
					Point[] oppl = (Point[]) opl[j].points.toArray(opl[j].pointCount);
					System.out.println("Test2");

					//populate each point
					for (int k = 0; k < oppl.length; k++)
						{
						System.out.println(" " + ippl.get(k));
						oppl[k].x = ippl.get(k).x;
						oppl[k].y = ippl.get(k).y;
						}
					}
				}
			}
		}

	protected void populateSounds()
		{
		int size = i.sounds.size();
		o.soundCount = size;
		if (size == 0) return;

		o.sounds = new Sound.ByReference();
		Sound[] osl = (Sound[]) o.sounds.toArray(size);
		org.lateralgm.resources.Sound[] isl = i.sounds.toArray(new org.lateralgm.resources.Sound[0]);
		for (int s = 0; s < size; s++)
			{
			Sound os = osl[s];
			org.lateralgm.resources.Sound is = isl[s];

			os.name = is.getName();
			os.id = is.getId();

			os.kind = SOUND_CODE.get(is.get(PSound.KIND));
			os.fileType = is.get(PSound.FILE_TYPE);
			os.fileName = is.get(PSound.FILE_NAME);
			os.chorus = is.get(PSound.CHORUS);
			os.echo = is.get(PSound.ECHO);
			os.flanger = is.get(PSound.FLANGER);
			os.gargle = is.get(PSound.GARGLE);
			os.reverb = is.get(PSound.REVERB);
			os.volume = is.get(PSound.VOLUME);
			os.pan = is.get(PSound.PAN);
			os.preload = is.get(PSound.PRELOAD);

			if (is.data == null)
				{
				os.size = 0;
				continue;
				}
			os.size = is.data.length;
			os.data = ByteBuffer.allocateDirect(os.size).put(is.data);
			}
		}

	protected void populateBackgrounds()
		{
		int size = i.backgrounds.size();
		o.backgroundCount = size;
		if (size == 0) return;

		o.backgrounds = new Background.ByReference();
		Background[] obl = (Background[]) o.backgrounds.toArray(size);
		org.lateralgm.resources.Background[] ibl = i.backgrounds.toArray(new org.lateralgm.resources.Background[0]);
		for (int s = 0; s < size; s++)
			{
			Background ob = obl[s];
			org.lateralgm.resources.Background ib = ibl[s];

			ob.name = ib.getName();
			ob.id = ib.getId();

			ob.transparent = ib.get(PBackground.TRANSPARENT);
			ob.smoothEdges = ib.get(PBackground.SMOOTH_EDGES);
			ob.preload = ib.get(PBackground.PRELOAD);
			ob.useAsTileset = ib.get(PBackground.USE_AS_TILESET);
			ob.tileWidth = ib.get(PBackground.TILE_WIDTH);
			ob.tileHeight = ib.get(PBackground.TILE_HEIGHT);
			ob.hOffset = ib.get(PBackground.H_OFFSET);
			ob.vOffset = ib.get(PBackground.V_OFFSET);
			ob.hSep = ib.get(PBackground.H_SEP);
			ob.vSep = ib.get(PBackground.V_SEP);

			ob.backgroundImage = new Image.ByReference();
			populateImage(ib.getBackgroundImage(),ob.backgroundImage,ob.transparent);
			}
		}

	protected void populatePaths()
		{
		int size = i.paths.size();
		o.pathCount = size;
		if (size == 0) return;

		o.paths = new Path.ByReference();
		Path[] opl = (Path[]) o.paths.toArray(size);
		org.lateralgm.resources.Path[] ipl = i.paths.toArray(new org.lateralgm.resources.Path[0]);
		for (int p = 0; p < size; p++)
			{
			Path op = opl[p];
			org.lateralgm.resources.Path ip = ipl[p];

			op.name = ip.getName();
			op.id = ip.getId();

			op.smooth = ip.get(PPath.SMOOTH);
			op.closed = ip.get(PPath.CLOSED);
			op.precision = ip.get(PPath.PRECISION);
			//don't need background room ID
			op.snapX = ip.get(PPath.SNAP_X);
			op.snapY = ip.get(PPath.SNAP_Y);

			op.pointCount = ip.points.size();
			if (op.pointCount == 0) continue;

			op.points = new PathPoint.ByReference();
			PathPoint[] oppl = (PathPoint[]) op.points.toArray(op.pointCount);
			for (int pp = 0; pp < oppl.length; pp++)
				{
				org.lateralgm.resources.sub.PathPoint ipp = ip.points.get(pp);
				oppl[pp].x = ipp.getX();
				oppl[pp].y = ipp.getY();
				oppl[pp].speed = ipp.getSpeed();
				}
			}
		}

	protected void populateScripts()
		{
		List<LibAction> qs = getQuestionLibActions();

		int size = i.scripts.size() + qs.size();
		o.scriptCount = size;
		if (size == 0) return;

		o.scripts = new Script.ByReference();
		Script[] osl = (Script[]) o.scripts.toArray(size);
		org.lateralgm.resources.Script[] isl = i.scripts.toArray(new org.lateralgm.resources.Script[0]);
		for (int s = 0; s < isl.length; s++)
			{
			Script oo = osl[s];
			org.lateralgm.resources.Script io = isl[s];

			oo.name = io.getName();
			oo.id = io.getId();
			oo.code = io.get(PScript.CODE);
			}

		for (int s = 0; s < qs.size(); s++)
			{
			Script oo = osl[s + isl.length];
			oo.name = "lib" + qs.get(s).parentId + "_action" + qs.get(s).id; //$NON-NLS-1$ //$NON-NLS-2$
			oo.id = -s - 2;
			oo.code = qs.get(s).execInfo;
			}
		}

	protected void populateFonts()
		{
		int size = i.fonts.size() + 1;
		o.fontCount = size;

		o.fonts = new Font.ByReference();
		Font[] ofl = (Font[]) o.fonts.toArray(size);

		// Populate the default font, called "EnigmaDefault", id -1
		java.awt.Font iF = new java.awt.Font(java.awt.Font.DIALOG,java.awt.Font.PLAIN,12);
		Font oF = ofl[0];
		oF.name = "EnigmaDefault"; //$NON-NLS-1$
		oF.id = -1;
		oF.fontName = iF.getFontName();
		oF.size = iF.getSize();
		oF.bold = false;
		oF.italic = false;
		oF.rangeMin = 32;
		oF.rangeMax = 127;
		oF.glyphs = populateGlyphs(iF,oF.rangeMin,oF.rangeMax,true);

		if (size == 1) return;

		org.lateralgm.resources.Font[] ifl = i.fonts.toArray(new org.lateralgm.resources.Font[0]);
		for (int f = 1; f < size; f++)
			{
			Font of = ofl[f];
			org.lateralgm.resources.Font ifont = ifl[f - 1];

			of.name = ifont.getName();
			of.id = ifont.getId();

			of.fontName = ifont.get(PFont.FONT_NAME);
			of.size = ifont.get(PFont.SIZE);
			of.bold = ifont.get(PFont.BOLD);
			of.italic = ifont.get(PFont.ITALIC);
			of.rangeMin = ifont.get(PFont.RANGE_MIN);
			of.rangeMax = ifont.get(PFont.RANGE_MAX);

			// Generate a Java font for glyph population
			int style = (of.italic ? java.awt.Font.ITALIC : 0) | (of.bold ? java.awt.Font.BOLD : 0);
			int screenRes = Toolkit.getDefaultToolkit().getScreenResolution();
			int fsize = (int) Math.round(of.size * screenRes / 72.0); // Java assumes 72 dps
			java.awt.Font fnt = new java.awt.Font(of.fontName,style,fsize);

			of.glyphs = populateGlyphs(fnt,of.rangeMin,of.rangeMax,
					(Integer) ifont.get(PFont.ANTIALIAS) > 0);
			}
		}

	private static Glyph.ByReference populateGlyphs(java.awt.Font fnt, int rangeMin, int rangeMax,
			boolean aa)
		{
		Glyph.ByReference glyphs = new Glyph.ByReference();
		Glyph[] ofgl = (Glyph[]) glyphs.toArray(rangeMax - rangeMin + 1);
		for (char c = (char) rangeMin; c <= rangeMax; c++)
			populateGlyph(ofgl[c - rangeMin],fnt,c,aa);
		return glyphs;
		}

	private static void populateGlyph(Glyph og, java.awt.Font fnt, char c, boolean aa)
		{
		GlyphVector gv = fnt.createGlyphVector(new FontRenderContext(null,aa,false),String.valueOf(c));
		Rectangle2D r = gv.getPixelBounds(null,0,0); //don't know why it needs coordinates
		if (r.getWidth() <= 0 || r.getHeight() <= 0) return;

		// Generate a raster of the glyph vector
		BufferedImage bi = new BufferedImage((int) Math.round(r.getWidth()),
				(int) Math.round(r.getHeight()),BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g = bi.createGraphics();
		g.setFont(fnt);
		g.setColor(Color.BLACK);
		g.fill(r);
		g.setColor(Color.WHITE);
		g.drawGlyphVector(gv,(float) -r.getX(),(float) -r.getY());

		// Produce the relevant stats
		og.origin = r.getX(); // bump image right X pixels
		og.baseline = r.getY(); // bump image down X pixels (usually negative, since baseline is at bottom)
		og.advance = gv.getGlyphPosition(gv.getNumGlyphs()).getX(); // advance X pixels from origin

		// Copy over the raster
		byte raster[] = new byte[bi.getWidth() * bi.getHeight()];
		// XXX: see if we can just get the Grayscale channel
		int rasterRGB[] = new int[raster.length];
		bi.getRGB(0,0,bi.getWidth(),bi.getHeight(),rasterRGB,0,bi.getWidth());
		for (int j = 0; j < rasterRGB.length; j++)
			raster[j] = (byte) (rasterRGB[j] & 0xFF);
		og.width = bi.getWidth();
		og.height = bi.getHeight();
		og.raster = ByteBuffer.allocateDirect(raster.length).put(raster);
		}

	protected void populateTimelines()
		{
		int size = i.timelines.size();
		o.timelineCount = size;
		if (size == 0) return;

		o.timelines = new Timeline.ByReference();
		Timeline[] otl = (Timeline[]) o.timelines.toArray(size);
		org.lateralgm.resources.Timeline[] itl = i.timelines.toArray(new org.lateralgm.resources.Timeline[0]);
		for (int t = 0; t < size; t++)
			{
			Timeline ot = otl[t];
			org.lateralgm.resources.Timeline it = itl[t];

			ot.name = it.getName();
			ot.id = it.getId();

			ot.momentCount = it.moments.size();
			if (ot.momentCount == 0) continue;

			ot.moments = new Moment.ByReference();
			Moment[] oml = (Moment[]) ot.moments.toArray(ot.momentCount);
			for (int m = 0; m < ot.momentCount; m++)
				{
				oml[m].stepNo = it.moments.get(m).stepNo;
				oml[m].code = getActionsCode(it.moments.get(m));
				}
			}
		}

	protected void populateObjects()
		{
		int size = i.gmObjects.size();
		o.gmObjectCount = size;
		if (size == 0) return;

		o.gmObjects = new GmObject.ByReference();
		GmObject[] ool = (GmObject[]) o.gmObjects.toArray(size);
		org.lateralgm.resources.GmObject[] iol = i.gmObjects.toArray(new org.lateralgm.resources.GmObject[0]);
		for (int s = 0; s < size; s++)
			{
			GmObject oo = ool[s];
			org.lateralgm.resources.GmObject io = iol[s];

			oo.name = io.getName();
			oo.id = io.getId();

			oo.spriteId = toId(io.get(PGmObject.SPRITE),-1);
			oo.solid = io.get(PGmObject.SOLID);
			oo.visible = io.get(PGmObject.VISIBLE);
			oo.depth = io.get(PGmObject.DEPTH);
			oo.persistent = io.get(PGmObject.PERSISTENT);
			oo.parentId = toId(io.get(PGmObject.PARENT),-100);
			oo.maskId = toId(io.get(PGmObject.MASK),-1);

			// Use this code instead to allow 0 main events
			// and switch GmObject.mainEvents to MainEvent.ByReference
			oo.mainEventCount = io.mainEvents.size();
			if (oo.mainEventCount == 0) continue;

			oo.mainEvents = new MainEvent.ByReference();
			MainEvent[] ooil = (MainEvent[]) oo.mainEvents.toArray(oo.mainEventCount);

			// we assume there are never 0 main events
			// oo.mainEventCount = io.mainEvents.size();
			// oo.mainEvents = new MainEvent[oo.mainEventCount];
			for (int me = 0; me < oo.mainEventCount; me++)
				{
				MainEvent ome = ooil[me];
				ArrayList<org.lateralgm.resources.sub.Event> iel = io.mainEvents.get(me).events;

				ome.id = me;
				ome.eventCount = iel.size();
				if (ome.eventCount == 0) continue;

				ome.events = new Event.ByReference();
				Event[] oel = (Event[]) ome.events.toArray(ome.eventCount);

				for (int e = 0; e < ome.eventCount; e++)
					{
					Event oe = oel[e];
					org.lateralgm.resources.sub.Event ie = iel.get(e);

					if (me == org.lateralgm.resources.sub.MainEvent.EV_COLLISION)
						oe.id = toId(ie.other,-1);
					else
						oe.id = ie.id;

					oe.code = getActionsCode(ie);
					}
				}
			}
		}

	protected void populateRooms()
		{
		ArrayList<org.lateralgm.resources.Room> irooms = new ArrayList<org.lateralgm.resources.Room>();
		Enumeration<?> e = root.preorderEnumeration();
		while (e.hasMoreElements())
			{
			ResNode node = (ResNode) e.nextElement();
			if (node.kind == org.lateralgm.resources.Resource.Kind.ROOM)
				{
				org.lateralgm.resources.Room r = (org.lateralgm.resources.Room) deRef((ResourceReference<?>) node.getRes());
				if (r != null) irooms.add(r); // is this null check even necessary?
				}
			}

		int size = irooms.size();
		o.roomCount = size;
		if (size == 0) return;

		o.rooms = new Room.ByReference();
		Room[] orly = (Room[]) o.rooms.toArray(size);
		org.lateralgm.resources.Room[] irl = irooms.toArray(new org.lateralgm.resources.Room[0]);
		for (int s = 0; s < size; s++)
			{
			Room or = orly[s];
			org.lateralgm.resources.Room is = irl[s];

			or.name = is.getName();
			or.id = is.getId();

			or.caption = is.get(PRoom.CAPTION);
			or.width = is.get(PRoom.WIDTH);
			or.height = is.get(PRoom.HEIGHT);

			// vvv may be useless vvv //
			or.snapX = is.get(PRoom.SNAP_X);
			or.snapY = is.get(PRoom.SNAP_Y);
			or.isometric = is.get(PRoom.ISOMETRIC);
			// ^^^ may be useless ^^^ //

			or.speed = is.get(PRoom.SPEED);
			or.persistent = is.get(PRoom.PERSISTENT);
			or.backgroundColor = ARGBtoRGBA(((Color) is.get(PRoom.BACKGROUND_COLOR)).getRGB());
			or.drawBackgroundColor = is.get(PRoom.DRAW_BACKGROUND_COLOR);
			or.creationCode = is.get(PRoom.CREATION_CODE);

			// vvv useless stuff vvv //
			or.rememberWindowSize = is.get(PRoom.REMEMBER_WINDOW_SIZE);
			or.editorWidth = is.get(PRoom.EDITOR_WIDTH);
			or.editorHeight = is.get(PRoom.EDITOR_HEIGHT);
			or.showGrid = is.get(PRoom.SHOW_GRID);
			or.showObjects = is.get(PRoom.SHOW_OBJECTS);
			or.showTiles = is.get(PRoom.SHOW_TILES);
			or.showBackgrounds = is.get(PRoom.SHOW_BACKGROUNDS);
			or.showViews = is.get(PRoom.SHOW_VIEWS);
			or.deleteUnderlyingObjects = is.get(PRoom.DELETE_UNDERLYING_OBJECTS);
			or.deleteUnderlyingTiles = is.get(PRoom.DELETE_UNDERLYING_TILES);
			or.currentTab = is.get(PRoom.CURRENT_TAB);
			or.scrollBarX = is.get(PRoom.SCROLL_BAR_X);
			or.scrollBarY = is.get(PRoom.SCROLL_BAR_Y);
			or.enableViews = is.get(PRoom.ENABLE_VIEWS);
			// ^^^ useless stuff ^^^ //

			or.backgroundDefCount = is.backgroundDefs.size();
			if (or.backgroundDefCount != 0)
				{
				or.backgroundDefs = new BackgroundDef.ByReference();
				BackgroundDef[] obdl = (BackgroundDef[]) or.backgroundDefs.toArray(or.backgroundDefCount);
				for (int bd = 0; bd < obdl.length; bd++)
					{
					BackgroundDef obd = obdl[bd];
					org.lateralgm.resources.sub.BackgroundDef ibd = is.backgroundDefs.get(bd);

					obd.visible = ibd.properties.get(PBackgroundDef.VISIBLE);
					obd.foreground = ibd.properties.get(PBackgroundDef.FOREGROUND);
					obd.x = ibd.properties.get(PBackgroundDef.X);
					obd.y = ibd.properties.get(PBackgroundDef.Y);
					obd.tileHoriz = ibd.properties.get(PBackgroundDef.TILE_HORIZ);
					obd.tileVert = ibd.properties.get(PBackgroundDef.TILE_VERT);
					obd.hSpeed = ibd.properties.get(PBackgroundDef.H_SPEED);
					obd.vSpeed = ibd.properties.get(PBackgroundDef.V_SPEED);
					obd.stretch = ibd.properties.get(PBackgroundDef.STRETCH);

					obd.backgroundId = toId(ibd.properties.get(PBackgroundDef.BACKGROUND),-1);
					}
				}

			or.viewCount = is.views.size();
			if (or.viewCount != 0)
				{
				or.views = new View.ByReference();
				View[] ovl = (View[]) or.views.toArray(or.viewCount);
				for (int v = 0; v < ovl.length; v++)
					{
					View ov = ovl[v];
					org.lateralgm.resources.sub.View iv = is.views.get(v);

					ov.visible = iv.properties.get(PView.VISIBLE);
					ov.viewX = iv.properties.get(PView.VIEW_X);
					ov.viewY = iv.properties.get(PView.VIEW_Y);
					ov.viewW = iv.properties.get(PView.VIEW_W);
					ov.viewH = iv.properties.get(PView.VIEW_H);
					ov.portX = iv.properties.get(PView.PORT_X);
					ov.portY = iv.properties.get(PView.PORT_Y);
					ov.portW = iv.properties.get(PView.PORT_W);
					ov.portH = iv.properties.get(PView.PORT_H);
					ov.borderH = iv.properties.get(PView.BORDER_H);
					ov.borderV = iv.properties.get(PView.BORDER_V);
					ov.speedH = iv.properties.get(PView.SPEED_H);
					ov.speedV = iv.properties.get(PView.SPEED_V);
					ov.objectId = toId(iv.properties.get(PView.OBJECT),-1);
					}
				}

			or.instanceCount = is.instances.size();
			if (or.instanceCount != 0)
				{
				or.instances = new Instance.ByReference();
				Instance[] oil = (Instance[]) or.instances.toArray(or.instanceCount);
				for (int i = 0; i < oil.length; i++)
					{
					Instance oi = oil[i];
					org.lateralgm.resources.sub.Instance ii = is.instances.get(i);

					oi.x = ii.properties.get(PInstance.X);
					oi.y = ii.properties.get(PInstance.Y);
					oi.objectId = toId(ii.properties.get(PInstance.OBJECT),-1);
					oi.id = ii.properties.get(PInstance.ID);
					oi.creationCode = ii.properties.get(PInstance.CREATION_CODE);
					oi.locked = ii.properties.get(PInstance.LOCKED);
					}
				}

			or.tileCount = is.tiles.size();
			if (or.tileCount != 0)
				{
				or.tiles = new Tile.ByReference();
				Tile[] otl = (Tile[]) or.tiles.toArray(or.tileCount);
				for (int t = 0; t < otl.length; t++)
					{
					Tile ot = otl[t];
					org.lateralgm.resources.sub.Tile it = is.tiles.get(t);

					ot.bgX = it.properties.get(PTile.BG_X);
					ot.bgY = it.properties.get(PTile.BG_Y);
					ot.roomX = it.properties.get(PTile.ROOM_X);
					ot.roomY = it.properties.get(PTile.ROOM_Y);
					ot.width = it.properties.get(PTile.WIDTH);
					ot.height = it.properties.get(PTile.HEIGHT);
					ot.depth = it.properties.get(PTile.DEPTH);
					ot.backgroundId = toId(it.properties.get(PTile.BACKGROUND),-1);
					ot.id = it.properties.get(PTile.ID);
					ot.locked = it.properties.get(PTile.LOCKED);
					} // tile
				} // tiles
			} // rooms
		} // populateRooms()

	public static void populateImage(BufferedImage i, Image o, boolean useTransp)
		{
		if (i == null || o == null) return;

		o.width = i.getWidth();
		o.height = i.getHeight();

		int pixels[] = i.getRGB(0,0,o.width,o.height,null,0,o.width);
		int trans = i.getRGB(0,o.height - 1) & 0x00FFFFFF;
		try
			{
			ByteArrayOutputStream baos = new ByteArrayOutputStream(pixels.length * 4);
			DeflaterOutputStream dos = new DeflaterOutputStream(baos);
			// is this the most efficient way? ARGB => RGBA
			for (int p = 0; p < pixels.length; p++)
				{
				dos.write(pixels[p] >>> 16 & 0xFF);
				dos.write(pixels[p] >>> 8 & 0xFF);
				dos.write(pixels[p] & 0xFF);
				if (useTransp && ((pixels[p] & 0x00FFFFFF) == trans))
					dos.write(0);
				else
					dos.write(pixels[p] >>> 24);
				}
			// pixels[p] = ARGBtoRGBA(pixels[p]);

			dos.finish();
			o.dataSize = baos.size();
			o.data = ByteBuffer.allocateDirect(baos.size()).put(baos.toByteArray());
			}
		catch (IOException e)
			{
			e.printStackTrace();
			}

		////we assume an int is 4 bytes
		//o.pixels = ByteBuffer.allocateDirect(o.width * o.height * 4).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(
		//		pixels);
		}

	public static int ARGBtoRGBA(int argb)
		{
		return ((argb & 0x00FFFFFF) << 8) | (argb >>> 24);
		}

	public static int toId(Object obj, int def)
		{
		ResourceReference<?> rr = (ResourceReference<?>) obj;
		if (deRef(rr) != null) return rr.get().getId();
		return def;
		}

	private static boolean actionDemise = false; //when unsupported actions are encountered, only report 1 error
	private static int numberOfBraces = 0; //gm ignores brace actions which are in the wrong place or missing
	private static int numberOfIfs = 0; //gm allows multipe else actions after 1 if, so its important to track the number

	public static String getActionsCode(ActionContainer ac)
		{
		final String nl = System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuilder code = new StringBuilder();

		numberOfBraces = 0;
		numberOfIfs = 0;

		for (Action act : ac.actions)
			{
			LibAction la = act.getLibAction();
			if (la == null)
				{
				if (!actionDemise)
					{
					JOptionPane.showMessageDialog(null,Messages.format(
							"EnigmaWriter.UNSUPPORTED_DND",ac.toString())); //$NON-NLS-1$
					actionDemise = true;
					}
				continue;
				}
			List<Argument> args = act.getArguments();
			switch (la.actionKind)
				{
				case Action.ACT_BEGIN:
					code.append('{');
					numberOfBraces++;
					break;
				case Action.ACT_CODE:
					//surround with brackets (e.g. for if conditions before it) and terminate dangling comments
					code.append('{').append(args.get(0).getVal()).append("/**/\n}").append(nl); //$NON-NLS-1$
					break;
				case Action.ACT_ELSE:
					{
					if (numberOfIfs > 0)
						{
						code.append("else "); //$NON-NLS-1$
						numberOfIfs--;
						}
					}
					break;
				case Action.ACT_END:
					if (numberOfBraces > 0)
						{
						code.append('}');
						numberOfBraces--;
						}
					break;
				case Action.ACT_EXIT:
					code.append("exit "); //$NON-NLS-1$
					break;
				case Action.ACT_REPEAT:
					code.append("repeat (").append(args.get(0).getVal()).append(") "); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case Action.ACT_VARIABLE:
					if (act.isRelative())
						code.append(args.get(0).getVal()).append(" += ").append(args.get(1).getVal()).append(nl); //$NON-NLS-1$
					else
						code.append(args.get(0).getVal()).append(" = ").append(args.get(1).getVal()).append(nl); //$NON-NLS-1$
					break;
				case Action.ACT_NORMAL:
					{
					if (la.execType == Action.EXEC_NONE) break;
					ResourceReference<org.lateralgm.resources.GmObject> apto = act.getAppliesTo();
					if (apto != org.lateralgm.resources.GmObject.OBJECT_SELF)
						{
						if (la.question)
							{
							/* Question action using with statement */
							if (apto == org.lateralgm.resources.GmObject.OBJECT_OTHER)
								code.append("with (other) "); //$NON-NLS-1$
							else if (apto.get() != null)
								code.append("with (").append(apto.get().getName()).append(") "); //$NON-NLS-1$ //$NON-NLS-2$
							else
								code.append("/*null with!*/"); //$NON-NLS-1$

							}
						else
							{
							if (apto == org.lateralgm.resources.GmObject.OBJECT_OTHER)
								code.append("with (other) {"); //$NON-NLS-1$
							else if (apto.get() != null)
								code.append("with (").append(apto.get().getName()).append(") {"); //$NON-NLS-1$ //$NON-NLS-2$
							else
								code.append("/*null with!*/{"); //$NON-NLS-1$
							}
						}
					if (la.question)
						{
						code.append("if "); //$NON-NLS-1$
						numberOfIfs++;
						}
					if (act.isNot()) code.append('!');
					if (la.allowRelative)
						{
						if (la.question)
							code.append("(argument_relative := ").append(act.isRelative()).append(", "); //$NON-NLS-1$ //$NON-NLS-2$
						else
							code.append("{argument_relative := ").append(act.isRelative()).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
						}
					if (la.question && la.execType == Action.EXEC_CODE)
						code.append("lib").append(la.parentId).append("_action").append(la.id); //$NON-NLS-1$ //$NON-NLS-2$
					else
						code.append(la.execInfo);
					if (la.execType == Action.EXEC_FUNCTION)
						{
						code.append('(');
						for (int i = 0; i < args.size(); i++)
							{
							if (i != 0) code.append(',');
							code.append(toString(args.get(i)));
							}
						code.append(')');
						}
					if (la.allowRelative) code.append(la.question ? ')' : "\n}"); //$NON-NLS-1$
					code.append(nl);

					if (apto != org.lateralgm.resources.GmObject.OBJECT_SELF && (!la.question))
						code.append("\n}"); //$NON-NLS-1$
					}
					break;
				}
			}
		if (numberOfBraces > 0)
			{
			//someone forgot the closing block action
			for (int i = 0; i < numberOfBraces; i++)
				code.append("\n}"); //$NON-NLS-1$
			}
		return code.toString();
		}

	public static String toString(Argument arg)
		{
		String val = arg.getVal();
		switch (arg.kind)
			{
			case Argument.ARG_BOTH:
				//treat as literal if starts with quote (")
				if (val.startsWith("\"") || val.startsWith("'")) return val; //$NON-NLS-1$ //$NON-NLS-2$
				//else fall through
			case Argument.ARG_STRING:
				return "\"" + val.replace("\\","\\\\").replace("\"","\"+'\"'+\"") + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			case Argument.ARG_BOOLEAN:
				return Boolean.toString(!val.equals("0")); //$NON-NLS-1$
			case Argument.ARG_MENU:
				return val;
			case Argument.ARG_COLOR:
				try
					{
					return String.format("$%06X",Integer.parseInt(val)); //$NON-NLS-1$
					}
				catch (NumberFormatException e)
					{
					}
				return val;
			default:
				if (Argument.getResourceKind(arg.kind) == null) return val;
				try
					{
					return arg.getRes().get().getName();
					}
				catch (NullPointerException e)
					{
					val = "-1"; //$NON-NLS-1$
					}
				return val;
			}
		}

	//in order to allow question actions to get converted to code, we treat their internal code as scripts
	public static ArrayList<LibAction> getQuestionLibActions()
		{
		ArrayList<LibAction> ala = new ArrayList<LibAction>();
		for (Library lib : LibManager.libs)
			for (LibAction act : lib.libActions)
				if (act.question && act.execType == Action.EXEC_CODE) ala.add(act);
		return ala;
		}
	}
