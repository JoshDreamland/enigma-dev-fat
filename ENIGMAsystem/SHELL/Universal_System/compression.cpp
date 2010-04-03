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

#include "../../additional/zlib/zlib.h"


/*int zlib_compressed_size=0;
int zlib_decompressed_size=0;

Bytef* zlib_compress(char* inbuffer,int actualsize)
{
    uLongf outsize=(int)(actualsize*1.1)+12;
    Bytef* outbytef=new Bytef[outsize];

    int res=compress(outbytef,&outsize,(Bytef*)inbuffer,actualsize);

    if (res != Z_OK)
    {
     #if SHOWERRORS
     if (res==Z_MEM_ERROR)
     show_error("Zlib failed to compress the buffer. Out of memory.",0);
     if (res==Z_BUF_ERROR)
     show_error("Zlib failed to compress the buffer. Output size greater than allotted.",0);
     #endif
    }

    zlib_compressed_size=outsize;
    return outbytef;
}*/

int zlib_decompress(Bytef* inbuffer, int insize, int uncompresssize,Bytef* outbytef){
	uLongf outused=uncompresssize;
	switch(uncompress(outbytef,&outused,(Bytef*)inbuffer,insize)){
	case Z_OK:return outused;
	case Z_MEM_ERROR:
		#if SHOWERRORS
			show_error("Zerror: Memory out",0);
		#endif
		return -1;
	case Z_BUF_ERROR:
		#if SHOWERRORS
			show_error("Zerror: Output of "+string(outused)+" above alloted "+string(uncompresssize),0);
		#endif
		return -2;
	case Z_DATA_ERROR:
		#if SHOWERRORS
			show_error("Zerror: Invalid data",0);
		#endif
		return -3;
	default:return -4;
	}
}
