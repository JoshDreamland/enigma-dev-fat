/*
 * Copyright (C) 2010 IsmAvatar <IsmAvatar@gmail.com>
 * 
 * This file is part of Enigma Plugin.
 * Enigma Plugin is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */


#include "Event.h"
#include "../JavaStruct.h"

struct MainEvent
{
	int id;
	Event *events;
	
	int eventCount;
};
