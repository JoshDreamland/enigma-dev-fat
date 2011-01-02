/********************************************************************************\
**                                                                              **
**  Copyright (C) 2008 Josh Ventura                                             **
**                                                                              **
**  This file is a part of the ENIGMA Development Environment.                  **
**                                                                              **
**                                                                              **
**  ENIGMA is free software: you can redistribute it and/or modify it under the **
**  terms of the GNU General Public License as published by the Free Software   **
**  Foundation, version 3 of the license or any later version.                  **
**                                                                              **
**  This application and its source code is distributed AS-IS, WITHOUT ANY      **
**  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS   **
**  FOR A PARTICULAR PURPOSE. See the GNU General Public License for more       **
**  details.                                                                    **
**                                                                              **
**  You should have recieved a copy of the GNU General Public License along     **
**  with this code. If not, see <http://www.gnu.org/licenses/>                  **
**                                                                              **
**  ENIGMA is an environment designed to create games and other programs with a **
**  high-level, fully compilable language. Developers of ENIGMA or anything     **
**  associated with ENIGMA are in no way responsible for its users or           **
**  applications created by its users, or damages caused by the environment     **
**  or programs made in the environment.                                        **
**                                                                              **
\********************************************************************************/

namespace enigma
{
  // This function will be passed sprite data, including pixel data (uncompressed RGBA bytes),
  // Offsets (origin point) x and y, and dimensions w and h. This function is intended to generate
  // a collision object, such as a bitmask or polygon mesh, and return a void* pointer to it.
  void *collisionsystem_sprite_data_create(char*,int,int,int,int); // It is called for every subimage of every sprite loaded.
  
  #ifdef _COLLISIONS_OBJECT_H
    // This function will be invoked each collision event to obtain a pointer to any
    // instance being collided with. It is expected to return NULL for no collision, or
    // an object_basic* pointing to the first instance found.
    object_basic *place_meeting_inst(double x, double y, int object);
  #endif
}
