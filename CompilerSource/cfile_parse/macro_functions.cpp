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

#include <iostream>
using namespace std;
#include "../general/parse_basics.h"
#include "../general/darray.h"
#include "cparse_shared.h"

string escaped_string(string x)
{
  string res = '"' + x;
  for (unsigned i = 1; i < res.length(); i++)
    if (res[i] == '"' or res[i] == '\\') { 
      res.insert(i,"\\"); i++; }
  return res + '"';
}

string stripspace(string x)
{
  unsigned int i;
  for (i=0; is_useless(x[i]); i++);
  x.erase(0,i);
  return x;
}

bool is_entirely_white(string x)
{
  for (unsigned i=0; i<x.length(); i++)
    if (!is_useless(x[i])) return 0;
  return 1;
}

bool macro_function_parse(string cfile,string macroname,unsigned int &pos,string& macrostr, varray<string> &args, const int numparams, const int au_at, bool cppcomments = true);


//This function has its own recursion stack.
//It is only called when a parameter is passed to a macro function.
bool preprocess_separately(string &macs)
{
  bool stringify = false;
  unsigned int macrod = 0;
  varray<string> inmacros;
  for (pt i = 0; i < macs.length(); i++)
  {
    if (is_digit(macs[i]))
    {
      const unsigned is = is;
      while (is_letterd(macs[++i]));
      if (stringify)
      {
        stringify = false;
        const string os = macs.substr(is,i-is);
        const size_t osl = os.length();
        string ns = escaped_string(os);
        macs.replace(is,i-is,ns);
        i += ns.length() - osl;
      }
    }
    else if (is_letter(macs[i]))
    {
      const size_t si = i;
      while (is_letterd(macs[++i]));
      string n = macs.substr(si,i-si);
      
      maciter t;
      if ((t=macros.find(n)) != macros.end())
      {
        if (stringify)
        {
          stringify = false;
          macs.replace(si,i-si,escaped_string(n));
          continue;
        }
        
        bool recurs=0;
        for (unsigned int iii=0;iii<macrod;iii++)
           if (inmacros[iii]==n) { recurs=1; break; }
        if (!recurs)
        {
          string macrostr = t->second;
          
          if (t->second.argc != -1) //Expect ()
          {
            if (!macro_function_parse(macs,n,i,macrostr,t->second.args,t->second.argc,t->second.args_uat)) {
              cferr = macrostr;
              return false;
            }
          }
          
          macs.replace(si,i-si,macrostr);
          inmacros[macrod++] = n;
          i = si;
        }
        //else puts("Recursing macro. This may be a problem.\r\n");
      }
    }
    else if (macs[i] == '#')
    {
      if (macs[i+1] == '#')
      {
        if (stringify)
          return false;
        unsigned ib = i;
        while (is_useless(macs[--ib])); ib++;
        i++;  while (is_useless(macs[++ib]));
        
        macs.erase(ib,i-ib);
        i = ib;
      }
      else //Stringify
      {
        stringify = true; //Notice the parallel in structure to the below function. No, not "redundant." The word is "parallel." <_<
        macs.erase(i,1);
      }
    }
  }
  return true;
}

bool macro_function_parse(string cfile,string macroname,unsigned int &pos,string& macrostr, varray<string> &args, const int numparams, const int au_at, bool cppcomments)
{
  //Skip comments. Ignore this block; it's savage but efficient.
  //Basically, I don't trust the compiler to correctly unroll a large conditional of shared parts.
    pos--; do you_know_you_love_this_block: if (cfile[++pos] == '/') {
    if (cfile[++pos] == '/') {
      pos += cppcomments;
      while (cfile[++pos] and cfile[pos] != '\n' and cfile[pos] != '\r'); goto you_know_you_love_this_block;
    } else if (cfile[pos] == '*') {
      pos += cppcomments;
      while (cfile[pos] and (cfile[pos++] != '*' or cfile[pos] != '/')); goto you_know_you_love_this_block;
    } } while (is_useless(cfile[pos]));
  //You can resume reading the code
  
  if (cfile[pos] != '(') { macrostr = macroname; return true; } //"Expected parameters to macro function"; return false; }
  pos++;
  
  //cout << endl << endl << endl << endl << endl << endl << "Raw: \r\n" << macrostr;
  
  int args_given = 0;
  varray<string> macro_args;
  const size_t len = cfile.length();
  
  unsigned lvl = 1;
  for (int i = 0; i < numparams or lvl > 0; i++) //parse out each parameter value into an array
  {
    if (i > numparams)
    { macrostr = "Expected closing parenthesis for macro function at this point: too many parameters"; return false; }
    const unsigned spos = pos;
    while (is_useless(cfile[pos])) pos++;
    while ((lvl > 1 or (cfile[pos] != ',' or args_given == au_at)) and pos < len and lvl)
    {
      if (cfile[pos] == ')') lvl--;
      else if (cfile[pos] == '(') lvl++;
      else if (cfile[pos] == '\"') { pos++; while (cfile[pos] != '\"' and pos < len) { if (cfile[pos] == '\\') pos++; pos++; } } 
      else if (cfile[pos] == '\'') { pos++; while (cfile[pos] != '\'' and pos < len) { if (cfile[pos] == '\\') pos++; pos++; } }  
      if (lvl) pos++; 
    }
    //Comma drops out as soon as cfile[pos] == ','
    //End Parenth will not increment if !lvl, so cfile[pos] == ')'
    const string rw = cfile.substr(spos,pos-spos);
    if (args_given or cfile[pos] != ')' or !is_entirely_white(rw))
      macro_args[args_given++] = rw;
    //cout << "Argument " << i << ": " << cfile.substr(spos,pos-spos) << endl;
    //cout << "This: '"<<cfile[pos]<<"'\r\n";
    pos++;
  }
  
  if (args_given != numparams)
  {
    if (au_at == -1 or args_given <= au_at) {
      macrostr = "Macro function requires " + tostring(numparams) + " parameters, passed " + tostring(args_given);
      return false;
    }
  }
  
  //cout << "Params: "; for (int i=0; i<args_given; i++) cout << macro_args[i] << ", "; cout << "end.";
  
  bool stringify = 0;
  
  for (unsigned i = 0; i < macrostr.length(); ) //unload the array of values we created before into the macro's definiens
  {
    if (is_letter(macrostr[i]) and !is_digit(macrostr[i-1]))
    {
      const unsigned si = i;
      
      i++; //Guaranteed letter here, so incrememnt
      while (i < macrostr.length() and is_letterd(macrostr[i])) i++;
      string sstr = macrostr.substr(si,i-si);
      
      for (int ii = 0; ii < numparams; ii++)
        if (args[ii] ==  sstr)
        {
          string iinto = macro_args[ii];
          if (stringify)
            macrostr.replace(si,i-si,escaped_string(iinto));
          else
          {
            if (!preprocess_separately(iinto))
              return false;
            macrostr.replace(si,i-si,iinto);
          }
              
          i = si + iinto.length();
          break;
        }
      stringify = false;
    }
    //Be on the lookout for such homosexualities as # and ##
    //To be ISO compliant, add a space between everything that doesn't have a ## in it
    else if (macrostr[i] == '#')
    {
      if (macrostr[i+1] == '#')
      {
        unsigned int end = i+2;
        while (i > 0 and is_useless(macrostr[--i])); i++;
        while (end < macrostr.length() and is_useless(macrostr[end]))
          if (macrostr[end++] == '\\') end++;
        macrostr.erase(i,end-i);
      }
      else
      {
        stringify = true;
        macrostr[i] = ' ';
        i++;
      }
    }
    else i++;
  }
  //cout << endl << endl << endl << macrostr << endl << endl << endl << endl << endl << endl;
  return true;
}
