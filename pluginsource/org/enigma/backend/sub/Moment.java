/*
 * Copyright (C) 2010 IsmAvatar <IsmAvatar@gmail.com>
 * 
 * This file is part of Enigma Plugin.
 * Enigma Plugin is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */

package org.enigma.backend.sub;

import com.sun.jna.Structure;

public class Moment extends Structure
	{
	public int stepNo;
	public String code;

	public static class ByReference extends Moment implements Structure.ByReference
		{
		}
	}
