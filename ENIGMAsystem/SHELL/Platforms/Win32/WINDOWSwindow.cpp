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

#include <string>
#include <windows.h>
using namespace std;

#include "../../Universal_System/var4.h"
#include "../../Universal_System/roomsystem.h"

namespace enigma
{
  extern HWND hWnd,hWndParent;
  extern int windowcolor; extern double viewscale; extern bool windowIsTop;
  
  void clampparent()
  {
    RECT c;
    GetWindowRect(hWnd,&c);
    AdjustWindowRect(&c, WS_CAPTION | WS_POPUPWINDOW | WS_VISIBLE | WS_SIZEBOX, FALSE);
    SetWindowPos(hWndParent,NULL,c.left,c.top,c.right-c.left,c.bottom-c.top,SWP_NOZORDER);
  }
}


int show_message(string str)
{
  MessageBox(enigma::hWnd,str.c_str(),"Message",MB_OK);
  return 0;
}

int window_get_x()
{
  RECT window;
  GetWindowRect(enigma::hWnd,&window);
  return window.left;
}

int window_get_y()
{
  RECT window;
  GetWindowRect(enigma::hWnd,&window);
  return window.top;
}

int window_get_width()
{
  RECT window;
  GetWindowRect(enigma::hWnd,&window);
  return window.right-window.left;
}

int window_get_height()
{
  RECT window;
  GetWindowRect(enigma::hWnd,&window);
  return window.bottom-window.top;
}

void window_set_visible(bool visible)
{
  ShowWindow(enigma::hWnd,visible);
}

int window_get_visible()
{
   return IsWindowVisible(enigma::hWnd);
}



void window_set_stayontop(const bool stay)
{
   if ((bool)stay)
   {
     SetWindowPos(enigma::hWnd,HWND_TOPMOST,0,0,0,0,SWP_NOSIZE|SWP_NOMOVE);
     enigma::windowIsTop=1;
   } else
   {
     SetWindowPos(enigma::hWnd,HWND_BOTTOM,0,0,0,0,SWP_NOSIZE|SWP_NOMOVE|SWP_NOACTIVATE);
     SetWindowPos(enigma::hWnd,HWND_TOP,0,0,0,0,SWP_NOSIZE|SWP_NOMOVE);
     enigma::windowIsTop=0; //Cheap hax, but every windows program I've experimented with does it, 
     //though GM changes it back every step if you try to make it stay on top. It does not notice if you send it back.
   }
}
int window_get_stayontop()
{
  return enigma::windowIsTop;
}

void window_set_caption(char* caption)
{
  SetWindowText(enigma::hWnd,caption);
}

void window_set_caption(string caption) {
  SetWindowText(enigma::hWndParent,(char*) caption.c_str());
}

string window_get_caption()
{
  char text_buffer[513];
  GetWindowText(enigma::hWnd, text_buffer, 512);
  return text_buffer;
}

void window_set_color(int color)
{
  enigma::windowcolor=(int)color;
}

int window_get_color()
{
  return enigma::windowcolor;
}

int window_set_region_scale(double scale, const bool adaptwindow)
{
  enigma::viewscale=scale;
  if ((bool)adaptwindow)
  {
    RECT window; GetWindowRect(enigma::hWnd,&window);
    SetWindowPos(enigma::hWnd, HWND_TOP, window.right, window.top, (int)(room_width*scale), (int)(room_height*scale), SWP_SHOWWINDOW);
  }
  return 0;
}
double window_get_region_scale()
{
  return enigma::viewscale;
}

void window_set_position(int x, int y)
{
  SetWindowPos(enigma::hWnd, HWND_TOP, (int)x, (int)y, 0, 0, SWP_NOSIZE|SWP_NOACTIVATE);
}
void window_set_size(unsigned int width, unsigned int height)
{
    SetWindowPos(enigma::hWnd, HWND_TOP, 0, 0, (int)width, (int)height, SWP_NOMOVE|SWP_NOACTIVATE);
    enigma::clampparent();
}

void window_set_rectangle(int x, int y, int width, int height)
{
    SetWindowPos(enigma::hWnd, HWND_TOP, x, y, width, height, SWP_SHOWWINDOW);
    enigma::clampparent();
}    

void window_center()
{
    RECT window;
    GetWindowRect(enigma::hWnd,&window);
    
    int tmp_wind_width = window.right - window.left;
    int tmp_wind_height = window.bottom - window.top;
    int screen_width = GetSystemMetrics(SM_CXBORDER);
    int screen_height = GetSystemMetrics(SM_CYBORDER);
    SetWindowPos(enigma::hWnd, HWND_TOP, (screen_width-tmp_wind_width)/2, (screen_height-tmp_wind_height)/2, 0, 0, SWP_NOSIZE|SWP_NOACTIVATE|SWP_DRAWFRAME);
}

void window_default()
{
    RECT window;
    GetWindowRect(enigma::hWnd,&window);
    
    int xm=0, ym=0;
    if (view_enabled) for (int i=0;i<7;i++) 
    {
      if (view_visible[i]) 
      { if (view_xview[i]+view_wview[i]>xm) xm=(int)(view_xview[i]+view_wview[i]); 
        if (view_yview[i]+view_hview[i]>ym) ym=(int)(view_yview[i]+view_hview[i]); } 
    } else { xm=(int)room_width; ym=(int)room_height; }
    
    int screen_width = GetSystemMetrics(SM_CXBORDER);
    int screen_height = GetSystemMetrics(SM_CYBORDER);
    SetWindowPos(enigma::hWnd, HWND_TOP, screen_width - xm/2, screen_height - ym/2, xm, ym, SWP_SHOWWINDOW);
}

int window_mouse_get_x()
{
    RECT window;
    GetWindowRect(enigma::hWnd,&window);
    POINT mouse;
	GetCursorPos(&mouse);
	
	return mouse.x-window.left;
}
int window_mouse_get_y()
{
    RECT window;
    GetWindowRect(enigma::hWnd,&window);
    POINT mouse;
	GetCursorPos(&mouse);
	
	return mouse.y-window.top;
}
void window_mouse_set(int x,int y)
{
    SetCursorPos(x,y);
}

namespace enigma {
  long int laststyle;
  int isFullScreen=0;
}
void window_set_fullscreen(const bool full)
{
  if (full)
  {
    if (!enigma::isFullScreen)
    {
      enigma::laststyle=SetWindowLongPtr(enigma::hWndParent,GWL_STYLE,WS_POPUP);
      ShowWindow(enigma::hWndParent,SW_MAXIMIZE);
      ShowWindow(enigma::hWnd,SW_MAXIMIZE);
      enigma::isFullScreen=1;
    }
  }
  else
  {
    if (enigma::isFullScreen)
    {
      SetWindowLongPtr(enigma::hWndParent,GWL_STYLE,enigma::laststyle);
      ShowWindow(enigma::hWndParent,SW_RESTORE);
      ShowWindow(enigma::hWnd,SW_RESTORE);
      enigma::isFullScreen=0;
    }
  }
}
int window_get_fullscreen() { return enigma::isFullScreen;  }

/*
window_set_showborder(show)
window_get_showborder()
window_set_showicons(show)
window_get_showicons()
window_set_sizeable(sizeable)
window_get_sizeable()

// section 5.10

view_angle[0..7]
view_hspeed[0..7]
view_vspeed[0..7]
window_set_region_size(w,h,adaptwindow)
window_get_region_width()
window_get_region_height()
window_views_mouse_get_x()
window_views_mouse_get_y()
window_views_mouse_set(x,y)
*/

/*
int window_set_cursor(int curs)
{
    switch (cursor)
    {
       SetCursor(LoadCursor(NULL, IDC_ARROW)); return 0;
       SetCursor(LoadCursor(NULL, IDC_APPSTART)); return 0;
    }
}
*/
/*
int window_get_curor()
{
    //Returns C++'s current cursor
    return GetCursor();
}
*/




namespace getstr
{
  HWND nwindow,ntextdp,bconfrm,bcancel,ninputf;
  HINSTANCE aninstance;
  bool initdstrfncs=0;
  
  void init()
  {
    nwindow=CreateWindow("getStringForm","Input",WS_CAPTION|WS_POPUP,96,64,300,144,enigma::hWnd,NULL,aninstance,NULL);
    ntextdp=CreateWindow("STATIC","",WS_CHILD|WS_VISIBLE,8,8,240,56,nwindow,NULL,aninstance,NULL);
    bconfrm=CreateWindow("BUTTON","OK",WS_CHILD|WS_VISIBLE,256,8,64,24,nwindow,NULL,aninstance,NULL);
    bcancel=CreateWindow("BUTTON","Cancel",WS_CHILD|WS_VISIBLE,256,40,64,24,nwindow,NULL,aninstance,NULL);
    
    ninputf=CreateWindow("EDIT","",WS_CHILD|WS_VISIBLE,8,72,304,16,nwindow,NULL,aninstance,NULL);
    initdstrfncs=1;
  }
}

string get_string(string message, string def)
{
  if (!getstr::initdstrfncs) 
  {
  #if SHOWERRORS
  show_error("ENIGMA Error: get_string not initialized.",0);
  #endif
  return "";
  }
  
  RECT nwsizes; int wm=8,hm=8;
   SetWindowText(getstr::ntextdp,message.c_str());
   SetWindowText(getstr::ninputf,def.c_str());
    GetWindowRect(getstr::ntextdp,&nwsizes); wm+=nwsizes.right-nwsizes.left+8; hm+=nwsizes.bottom-nwsizes.top+8;
    GetWindowRect(getstr::bconfrm,&nwsizes); wm+=nwsizes.right-nwsizes.left+8;
    GetWindowRect(getstr::ninputf,&nwsizes); hm+=nwsizes.bottom-nwsizes.top+8;
  RECT nwnszwb; nwnszwb.left=0; nwnszwb.top=0; nwnszwb.right=wm; nwnszwb.bottom=hm;
    AdjustWindowRect(&nwnszwb,WS_CAPTION|WS_VISIBLE|WS_POPUP,0);
  
  SetWindowPos(getstr::nwindow,HWND_TOPMOST,0,0,nwnszwb.right-nwnszwb.left,nwnszwb.bottom-nwnszwb.top,SWP_NOMOVE);
  
  ShowWindow(getstr::nwindow,SW_SHOW);
  
  for (;;) 
  {
    if (SendMessage(getstr::bconfrm,BM_GETSTATE,0,0)==108) { int sz=GetWindowTextLength(getstr::ninputf)+2; 
        char sbuffer[sz]; GetWindowText(getstr::ninputf,sbuffer,sz); ShowWindow(getstr::nwindow,SW_HIDE); return sbuffer; }
    if (SendMessage(getstr::bcancel,BM_GETSTATE,0,0)==108) { ShowWindow(getstr::nwindow,SW_HIDE); return ""; }
    MSG msg;
    if (GetMessage (&msg, NULL, 0, 0))
    {
      if (msg.message == WM_QUIT) { ShowWindow(getstr::nwindow,SW_HIDE); return ""; }
      TranslateMessage (&msg); DispatchMessage (&msg);
    }
    Sleep(1);
  }
}



int game_end() { PostQuitMessage(0); return 0; }




int get_color(double defcolor)
{
    COLORREF defc=(int)defcolor;
    static COLORREF custcs[16];
    
    CHOOSECOLOR gcol;
    gcol.lStructSize=sizeof(CHOOSECOLOR);
    gcol.hwndOwner=enigma::hWnd;
    gcol.rgbResult=defc;
    gcol.lpCustColors=custcs; 
    gcol.Flags=CC_RGBINIT; 
    gcol.lpTemplateName=""; 
    
    if (ChooseColor(&gcol))
    return (int)defc; else return -1;
}


