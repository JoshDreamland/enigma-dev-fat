/********************************************************************************\
 **                                                                              **
 **  Copyright (C) 2011 Polygone                                                 **
 **  Copyright (C) 2011 Josh Ventura                                             **
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

void motion_set(int dir, double newspeed);
void motion_add(double newdirection, double newspeed);
void move_random(const double snapHor, const double snapVer);
void move_snap(const double hsnap, const double vsnap);

void move_wrap(const bool hor, const bool vert, const double margin);
void move_towards_point (const double point_x, const double point_y, const double newspeed);
bool place_snapped(int hsnap, int vsnap);

