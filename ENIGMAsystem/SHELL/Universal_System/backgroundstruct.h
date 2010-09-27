/********************************************************************************\
**                                                                              **
**  Copyright (C) 2008 Josh Ventura                                             **
**  Copyright (C) 2010 Alasdair Morrison <tgmg@g-java.com>                      **
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
  struct background
  {
    int width, height;
    unsigned texture;
    
    bool transparent;
    bool smooth;
    bool preload;
	  double texbordx, texbordy;
    
    const bool tileset;
    
    background();
    background(const bool);
    background(int w,int h,unsigned tex,bool trans,bool smth,bool prel);
    background(const bool,int w,int h,unsigned tex,bool trans,bool smth,bool prel);
  };
  struct background_tileset: background
  {
    int tileWidth;
    int tileHeight;
    int hOffset;
    int vOffset;
    int hSep;
    int vSep;
    
    background_tileset();
    background_tileset(int tw, int th, int ho, int vo, int hs, int vs);
    background_tileset(int w,int h,unsigned tex,bool trans,bool smth,bool prel,int tw, int th, int ho, int vo, int hs, int vs);
  };
  
  extern background** backgroundstructarray;
	void background_new(int bkgid, unsigned w, unsigned h, unsigned char* chunk, bool transparent, bool smoothEdges, bool preload, bool useAsTileset, int tileWidth, int tileHeight, int hOffset, int vOffset, int hSep, int vSep);

}

namespace enigma
{
	//Allocates and zero-fills the array at game start
	void backgrounds_init();
}
