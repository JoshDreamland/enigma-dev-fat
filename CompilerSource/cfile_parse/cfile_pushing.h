
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

#include <stack>
#include <string>
using namespace std;

#include "cparse_shared.h"

struct includings { 
  string name, path;
  includings(string n,string p);
};
struct ifstack {
  struct ifnode {
    includings *i;
    ifnode *prev;
    ifnode(includings *ia, ifnode *p);
  };
  ifnode* last;
  void push(const includings&);
  includings &top();
  bool empty();
  void pop();
  ifstack();
};
extern ifstack included_files;


struct cfnode
{
  my_string scfile;
  pt spos;
  pt slen;
  cfnode();
  ~cfnode();
};

//extern unsigned int macrod=0;
//extern varray<string> inmacros;

template<typename r> struct dlstack {
  struct node {
    r m;
    node *prev, *next;
    node(r nm, node* np): m(nm), prev(np), next(NULL) {}
  } *m;
  r &top() { return m->m; };
  void push(r v) { if (m->next) m = m->next, m->m = v; else m = m->next = new node(v,m); }
  void pop() { m = m->prev; }
  bool empty() { return !m->m; }
  
  node* peek() { return m->prev; }
  node* peek(node* m) { return m->prev; }
  
  dlstack(): m(new node(NULL,NULL)) {}
};
extern dlstack<cfnode*> cfstack;
void handle_macro_pop();
pt handle_macros(const string n);

extern varray<string> include_directories;
extern unsigned int include_directory_count;
