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

#include <math.h>
#include "OpenGLHeaders.h"
#include "../../Universal_System/spritestruct.h"

#define __GETR(x) ((x & 0x0000FF))
#define __GETG(x) ((x & 0x00FF00) >> 8)
#define __GETB(x) ((x & 0xFF0000) >> 16)

namespace enigma{extern unsigned bound_texture;}

int sprite_exists(int spr) {
  return (unsigned(spr) < enigma::sprite_idmax) and bool(enigma::spritestructarray[spr]);
}

typedef struct {
    GLfloat x;
    GLfloat y;
    GLfloat z;
} Vertex3D;

static inline Vertex3D Vertex3DMake(float inX,  float inY, float inZ)
{
    Vertex3D ret;
    ret.x = inX;
    ret.y = inY;
    ret.z = inZ;
    return ret;
}

int draw_sprite(int spr,int subimg,double x,double y)
{
  enigma::sprite *spr2d = enigma::spritestructarray[spr];
  if (!spr2d)
    return -1;
  
  if (enigma::bound_texture != spr2d->texturearray[subimg % spr2d->subcount])
  {
    glBindTexture(GL_TEXTURE_2D,spr2d->texturearray[subimg % spr2d->subcount]);
    enigma::bound_texture = spr2d->texturearray[subimg % spr2d->subcount];
  }
	glEnable(GL_TEXTURE_2D);
	
	glColor4f(1,1,1,1); //IMPORTANT makes sprites black without it
  
  //glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE); //OPENGLES
  //glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE); //OPENGLES
    
    const float tbx=spr2d->texbordx,tby=spr2d->texbordy;
    //START OPENGLES
	
	//glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
	// Enable blending
	//glEnable(GL_BLEND);
	
	//end edit
	GLfloat vertices[] = {
        x-spr2d->xoffset,  y-spr2d->yoffset,
		x+spr2d->width-spr2d->xoffset, y-spr2d->yoffset,
        x+spr2d->width-spr2d->xoffset, y+spr2d->height-spr2d->yoffset,
		x-spr2d->xoffset, y+spr2d->height-spr2d->yoffset
    };
	GLfloat texCoords[] = {
        0.0, 0.0,
        tbx, 0.0,
        tbx, tby,
        0.0, tby
    };
	static const Vertex3D normals[] = {
        {0.0, 0.0, 1.0},
        {0.0, 0.0, 1.0},
        {0.0, 0.0, 1.0},
        {0.0, 0.0, 1.0}
    };
	//glClear(GL_COLOR_BUFFER_BIT);
	glEnableClientState(GL_VERTEX_ARRAY);
	glEnableClientState (GL_TEXTURE_COORD_ARRAY);
	glEnableClientState(GL_NORMAL_ARRAY);
	glVertexPointer(2, GL_FLOAT, 0, vertices);//vertices);
	glNormalPointer(GL_FLOAT, 0, normals);
	glTexCoordPointer(2, GL_FLOAT, 0, texCoords);
	glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
	glDisableClientState(GL_VERTEX_ARRAY);
	glDisableClientState (GL_TEXTURE_COORD_ARRAY);
	glDisable(GL_TEXTURE_2D);
	//END OPENGLES
	
	return 0;
}

int draw_sprite_stretched(int spr,int subimg,double x,double y,double w,double h)
{
	enigma::sprite *spr2d=enigma::spritestructarray[spr];
	if (!spr2d)
    return -1;
	/*glPushAttrib(GL_CURRENT_BIT);
    glColor4f(1,1,1,1);
    if (enigma::bound_texture!=spr2d->texturearray[subimg % spr2d->subcount])
    {
      glBindTexture(GL_TEXTURE_2D,spr2d->texturearray[subimg % spr2d->subcount]);
      enigma::bound_texture=spr2d->texturearray[subimg % spr2d->subcount];
    }
    
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE); //OPENGLES
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE); //OPENGLES
    
    glBegin(GL_QUADS);
      glTexCoord2f(spr2d->texbordx,0);
        glVertex2f(x-spr2d->xoffset,y-spr2d->yoffset);
      glTexCoord2f(0,0);
        glVertex2f(x+w-spr2d->xoffset,y-spr2d->yoffset);
      glTexCoord2f(0,spr2d->texbordy);
        glVertex2f(x+w-spr2d->xoffset,y+h-spr2d->yoffset);
      glTexCoord2f(spr2d->texbordx,spr2d->texbordy);
        glVertex2f(x-spr2d->xoffset,y+h-spr2d->yoffset);
    glEnd();
	glPopAttrib(); OPENGLES*/
	return 0;
}

int draw_sprite_part(int spr,int subimg,double left,double top,double width,double height,double x,double y)
{
  enigma::sprite *spr2d=enigma::spritestructarray[spr];
  if (!spr2d)
    return -1;
  
  /*glPushAttrib(GL_CURRENT_BIT);
    glColor4f(1,1,1,1);
    if (enigma::bound_texture != spr2d->texturearray[subimg % spr2d->subcount])
    {
      glBindTexture(GL_TEXTURE_2D,spr2d->texturearray[subimg % spr2d->subcount]);
      enigma::bound_texture=spr2d->texturearray[subimg % spr2d->subcount];
    }
    
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE); //OPENGLES
    float tbw=spr2d->width/(float)spr2d->texbordx,tbh=spr2d->height/(float)spr2d->texbordy;
    
    glBegin(GL_TRIANGLE_STRIP);
      glTexCoord2f(left/tbw,top/tbh);
        glVertex2f(x,y);
      glTexCoord2f((left+width)/tbw,top/tbh);
        glVertex2f(x+width,y);
      glTexCoord2f(left/tbw,(top+height)/tbh);
        glVertex2f(x,y+height);
      glTexCoord2f((left+width)/tbw,(top+height)/tbh);
        glVertex2f(x+width,y+height);
    glEnd();
	glPopAttrib(); OPENGLES*/
	return 0;
}

int draw_sprite_part_offset(int spr,int subimg,double left,double top,double width,double height,double x,double y)
{
  enigma::sprite *spr2d=enigma::spritestructarray[spr];
  if (!spr2d)
    return -1;
  
  /*glPushAttrib(GL_CURRENT_BIT);
    glColor4f(1,1,1,1);
    if (enigma::bound_texture != spr2d->texturearray[subimg % spr2d->subcount])
    {
      glBindTexture(GL_TEXTURE_2D,spr2d->texturearray[subimg % spr2d->subcount]);
      enigma::bound_texture=spr2d->texturearray[subimg % spr2d->subcount];
    }
    
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE); //OPENGLES
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE); //OPENGLES
    float tbw=spr2d->width/spr2d->texbordx,tbh=spr2d->height/spr2d->texbordy;
    
    glBegin(GL_TRIANGLE_STRIP);
      glTexCoord2f(left/tbw,top/tbh);
        glVertex2f(x-spr2d->xoffset,y-spr2d->yoffset);
      glTexCoord2f((left+width-1)/tbw,top/tbh);
        glVertex2f(x+width-1-spr2d->xoffset,y-spr2d->yoffset);
      glTexCoord2f(left/tbw,(top+height-1)/tbh);
        glVertex2f(x-spr2d->xoffset,y+height-1-spr2d->yoffset);
      glTexCoord2f((left+width-1)/tbw,(top+height-1)/tbh);
        glVertex2f(x+width-1-spr2d->xoffset,y+height-1-spr2d->yoffset);
    glEnd();
    glBindTexture(GL_TEXTURE_2D,0);
	glPopAttrib(); OPENGLES */
	return 0;
}

int draw_sprite_ext(int spr,int subimg,double x,double y,double xscale,double yscale,double rot,int blend,double alpha)
{
  return draw_sprite(spr,subimg,x,y);
  enigma::sprite *spr2d = enigma::spritestructarray[spr];
  if (!spr2d)
    return -1;
  
  if (enigma::bound_texture != spr2d->texturearray[subimg % spr2d->subcount])
  {
    glBindTexture(GL_TEXTURE_2D,spr2d->texturearray[subimg % spr2d->subcount]);
    enigma::bound_texture = spr2d->texturearray[subimg % spr2d->subcount];
  }
  
  glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE); //OPENGLES
  glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE); //OPENGLES
  
  /*glPushAttrib(GL_CURRENT_BIT);
    glBegin(GL_TRIANGLE_STRIP);
      const float w = spr2d->width*xscale,   h = spr2d->height*yscale;
      const float tbx = spr2d->texbordx,   tby = spr2d->texbordy;
      rot *= M_PI/180;
      
      float
        ulcx = x + spr2d->xoffset * cos(M_PI+rot) + spr2d->yoffset * cos(M_PI/2+rot),
        ulcy = y - spr2d->xoffset * sin(M_PI+rot) - spr2d->yoffset * sin(M_PI/2+rot);
      
      glColor4ub(__GETR(blend),__GETG(blend),__GETB(blend),char(alpha*255)); //Implement "blend" parameter
        glTexCoord2f(0,0);
          glVertex2f(ulcx,ulcy);
        glTexCoord2f(tbx,0);
          glVertex2f(ulcx + w*cos(rot), ulcy - w*sin(rot));
        glTexCoord2f(0,tby);
          ulcx += h * cos(3*M_PI/2 + rot);
          ulcy -= h * sin(3*M_PI/2 + rot);
          glVertex2f(ulcx,ulcy);
        glTexCoord2f(tbx,tby);
          glVertex2f(ulcx + w*cos(rot), ulcy - w*sin(rot));
    glEnd();
  glPopAttrib(); OPENGLES*/
  return 0;
}

int draw_sprite_part_ext(int spr,int subimg,double left,double top,double width,double height,double x,double y,double xscale,double yscale,int color,double alpha)
{
	enigma::sprite *spr2d=enigma::spritestructarray[spr];
	if (!spr2d)
    return -1;
	
	/*glPushAttrib(GL_CURRENT_BIT);
    glColor4ub(__GETR(color),__GETG(color),__GETB(color),char(alpha*255));
    if (enigma::bound_texture != spr2d->texturearray[subimg % spr2d->subcount])
    {
      glBindTexture(GL_TEXTURE_2D,spr2d->texturearray[subimg % spr2d->subcount]);
      enigma::bound_texture=spr2d->texturearray[subimg % spr2d->subcount];
    }
    
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE); //OPENGLES
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE); //OPENGLES
    float tbw=spr2d->width/spr2d->texbordx,tbh=spr2d->height/spr2d->texbordy;
    
    glBegin(GL_TRIANGLE_STRIP);
      glTexCoord2f(left/tbw,top/tbh);
        glVertex2f(x-spr2d->xoffset,y-spr2d->yoffset);
      glTexCoord2f((left+width-1)/tbw,top/tbh);
        glVertex2f(x+width-1-spr2d->xoffset,y-spr2d->yoffset);
      glTexCoord2f(left/tbw,(top+height-1)/tbh);
        glVertex2f(x-spr2d->xoffset,y+height-1-spr2d->yoffset);
      glTexCoord2f((left+width-1)/tbw,(top+height-1)/tbh);
        glVertex2f(x+width-1-spr2d->xoffset,y+height-1-spr2d->yoffset);
    glEnd();
	glPopAttrib(); OPENGLES*/
	return 0;
}

int draw_sprite_general(int spr,int subimg,double left,double top,double width,double height,double x,double y,double xscale,double yscale,double rot,int c1,int c2,int c3,int c4,double alpha)
{
	enigma::sprite *spr2d=enigma::spritestructarray[spr];
	if (!spr2d)
    return -1;
	
 /* glPushAttrib(GL_CURRENT_BIT);
    if (enigma::bound_texture != spr2d->texturearray[subimg % spr2d->subcount])
    {
      glBindTexture(GL_TEXTURE_2D,spr2d->texturearray[subimg % spr2d->subcount]);
      enigma::bound_texture=spr2d->texturearray[subimg % spr2d->subcount];
    }
    
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE); //OPENGLES
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE); //OPENGLES
    int w = int(spr2d->width * xscale),h = int(spr2d->height*yscale);
    float tbx=spr2d->texbordx,tby=spr2d->texbordy;
    
    glBegin(GL_TRIANGLE_STRIP);
      glColor4ub(__GETR(c1),__GETG(c1),__GETB(c1),char(alpha*255));
        glTexCoord2f(width/tbx,top/tby);
          glVertex2f(x-spr2d->xoffset,y-spr2d->yoffset);
      glColor4ub(__GETR(c2),__GETG(c2),__GETB(c2),char(alpha*255));
        glTexCoord2f(left/tbx,top/tby);
          glVertex2f(x+w-spr2d->xoffset,y-spr2d->yoffset);
      glColor4ub(__GETR(c3),__GETG(c3),__GETB(c3),char(alpha*255));
        glTexCoord2f(width/tbx,height/tby);
          glVertex2f(x-spr2d->xoffset,y+h-spr2d->yoffset);
      glColor4ub(__GETR(c4),__GETG(c4),__GETB(c4),char(alpha*255));
        glTexCoord2f(left/tbx,height/tby);
          glVertex2f(x+w-spr2d->xoffset,y+h-spr2d->yoffset);
    glEnd();
	glPopAttrib(); OPENGLES */
	return 0;
}
