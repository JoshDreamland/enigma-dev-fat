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

#include <map>
#include <string>
#include <cstdlib>

#include <iostream>

using namespace std;

#include "../general/textfile.h"
#include "../general/darray.h"

#include "../general/parse_basics.h"
#include "../externs/externs.h"

#include "../settings.h"

#include "../parser/object_storage.h"

string tostring(int val);


//From parser_components
int dropscope();
int quickscope();
int initscope(string name);
int quicktype(unsigned flags, string name);


namespace syncheck
{
  int lastln;
  string error="";
  int syntacheck(string code);

  void addscr(string name)
  {
    externs *ne=global_scope.members[name];
    ne->refstack += referencer('(',16,0,1);
  }
}

namespace syncheck
{
  // Start off with some basic things
  // we'll need during the check process
  ////////////////////////////////////////

  #include "chkfunction.h"

    //Map for flexibility, dynamics, and legibility
    typedef map<int,int> array;

    //How high we are, stack wise
     int level=0;
    //These tell us what kind of level this is
     array levelt;
     enum 
     {
       LEVELTYPE_BRACE,
       LEVELTYPE_IF,
       LEVELTYPE_ELSE,
       LEVELTYPE_DO,
       LEVELTYPE_CASE,
       LEVELTYPE_SWITCH,
       LEVELTYPE_SWITCH_BLOCK,
       LEVELTYPE_FOR_PARAMETERS,
       LEVELTYPE_GENERAL_STATEMENT,
       LEVELTYPE_TEMPLATE_P,
       LEVELTYPE_TERNARY,
       LEVELTYPE_STRUCT,
       LEVELTYPE_LOOP
     };

    //If we are in an if statement, we expect two expressions before else statement.
      //For example, if a=b b=c else d=e;
      //a=b is expression one, then b=c is expression two.
      //This variable represents the number of statements.
      array statement_pad;

    //How high we are, parenthesis wise
     int plevel=0;
    //These tell us what kind of parentheses we're in
     array plevelt;
     enum {
      PLT_PARENTH,
      PLT_BRACKET,
      PLT_FUNCTION,
      PLT_FORSTATEMENT,
      PLT_TEMPLATE_PARAMS,
      PLT_CAST
     };


    array assop; //Whether we have an assignment operator, organized per-level
    array lastnamed; //The last thing that was named
    array lastnamedatt; //The last thing that was named
    //Values for type of last named token:
      enum { 
        LN_NOTHING,        //
        LN_VARNAME,        // somevar
        LN_OPERATOR,       // = + - *...
        LN_DIGIT,          // 0 1 2... (...)
        LN_VALUE,          // 1.5, "", function()
        LN_FUNCTION_NAME,  // game_end
        LN_TYPE_NAME,      // int, double, whatever 
        LN_CLOSING_SYMBOL, // { ; }
        LN_LOCGLOBAL,      // global/local
        LN_FOR,            // for
        LN_NEW,            // new
        LN_NEW_TYPE        // new int
      };
      
      enum {
        LNA_NOTHING,
        LNA_DOT, // a.b, 1.2, 1.a
        LNA_MEMBERVAR
      };

    //For declarations based on a typename
     array indeclist;

    //either in a declaration list and plevel is zero, or it's one and plevelt is PLT_FORSTATEMENT
    //or we're in a function
      #define inlist() ((indeclist[level] && (plevel==0 || plevelt[plevel]==PLT_FORSTATEMENT)) or (plevelt[plevel]==PLT_FUNCTION))

    //One more precaution. This is for a=.b
    //it enables the checker to dismiss a . it comes by until next iteration,
    //where it is treated as a regular number. This makes it so when that happens,
    //you can't have .5.23
    int decimal_named = 0;
    int number_of_statements = 0;

   //Anything else the checker will use a lot
    #include "syntaxtools.h"

  // Now that the basics are over, we get into
  // the specific variables we'll be working with
  //////////////////////////////////////////////////

    int errorpos=-1; //Position of any errors
    externs *function_ext; //Pointer to info on the last function dealt with.
    string last_identifier; //The name of the last identifier read.

  // Onto the actual syntax checker code
  ////////////////////////////////////////
  
  int syntacheck(string code) //string for substr() member
  {
    error="";
    errorpos=-1;

    pt pos=0;
    const pt len = code.length();

    level=0; plevel=0;
    assop[level]=0;
    lastnamed[level]=LN_NOTHING;
    lastnamedatt[level]=LNA_NOTHING;
    
    statement_pad[level]=0;
    
    initscope("script scope | Syntax");

    while (pos < len)
    {
      //Handle whitespace first of all
      if (is_useless(code[pos])) {
        while (is_useless(code[++pos]));
        continue;
      }
      
      if (is_letter(code[pos])) //we're at an identifier, a statement, or a word-based operator
      {
        const pt spos = pos;
        while (is_letterd(code[++pos]));
        const string name = code.substr(spos,pos-spos);
        
        if (!is_wordop(name))
        {
          //Close any previously completed statements, since this isn't an operator
          //Also handle else and until statements
          if (statement_completed(lastnamed[level])) {
            int cs = close_statement(code,pos);
            if (cs != -1) return cs;
          }
          else if (lastnamed[level]==LN_VARNAME or lastnamed[level]==LN_FUNCTION_NAME)
          { error="Operator expected at this point"; return pos; }
          
          const pt issr = handle_if_statement(code,name,pos); //Check *if* it's a statement, handle it *if* it is.
          if (issr != pt(-2)) {
            if (issr != pt(-1))
              return issr;
          }
          else if (name == "local" or name == "global") //These two are very special...
            lastnamed[level] = LN_LOCGLOBAL,
            lastnamedatt[level]=LNA_NOTHING;
          else if (name == "long" or name == "short" or name == "signed" or name == "register"
               or  name == "unsigned" or name == "const" or name == "volatile" or name == "static")
            goto lbl_typename;
          else if (name == "new")
          {
            if (lastnamed[level] != LN_OPERATOR and lastnamed[level] != LN_NOTHING)
              return (error = "Unexpected token `new' at this point", pos);
            lastnamed[level] = LN_NEW;
              lastnamedatt[level] = LNA_NOTHING;
          }
          else if (shared_object_locals.find(name) != shared_object_locals.end())
            goto just_an_identifier;
          else if (find_extname(name,0xFFFFFFFF))
          {
            if (ext_retriever_var->flags & EXTFLAG_TYPENAME)
            {
              lbl_typename:
              if (lastnamed[level] == LN_NEW or lastnamed[level] == LN_NEW_TYPE)
              {
                lastnamed[level] = LN_NEW_TYPE;
                continue;
              }
              indeclist[level] = true;
              lastnamed[level] = LN_TYPE_NAME,
                lastnamedatt[level]=LNA_NOTHING;
              if (plevel > 0)
              {
                if (plevelt[plevel] == PLT_PARENTH)
                  plevelt[plevel] = PLT_CAST;
                else if (plevelt[plevel] != PLT_FORSTATEMENT) {
                  lastnamed[level] = plevelt[plevel] == PLT_TEMPLATE_PARAMS ? LN_TYPE_NAME : LN_OPERATOR; //Cast ~~ Unary operator
                  lastnamedatt[level] = LNA_NOTHING;
                }
              }
            }
            else if (ext_retriever_var->is_function()) {
              lastnamed[level] = LN_FUNCTION_NAME;
              lastnamedatt[level] = LNA_NOTHING;
              function_ext = ext_retriever_var;
            }
            else goto just_an_identifier;
          }
          else //just an identifier; ie, a variable or function name
          {
            just_an_identifier:
            last_identifier = name;
            if (levelt[level] == LEVELTYPE_STRUCT and statement_pad[level] == PAD_STRUCT_NOTHING)
              quicktype(EXTFLAG_STRUCT,last_identifier);
            lastnamed[level] = LN_VARNAME;
            lastnamedatt[level] = lastnamedatt[level] == LNA_DOT ? LNA_MEMBERVAR : LNA_NOTHING;
          }
        }
        else //this is actually a word-based operator, such as `and' and `or'
        {
          if (name[0]=='n') //not, the only unary operator.
          {
            if (!assop[level] or lastnamed[level] != LN_OPERATOR) { //has to be preceeded by operator
              error = "Expected operator before unary `not' operator";
              return pos;
            }
          }
          else //Binary operator
          {
            if (!assop[level] && !(plevel>0)) //randomly placed 'varname and varname'
            { error="Assignment operator expected before this point"; return pos; }
            if (lastnamed[level] != LN_VARNAME and lastnamed[level] != LN_DIGIT and lastnamed[level] != LN_VALUE)
            { error="Expected primary expression before operator"; return pos; }
            lastnamed[level] = LN_OPERATOR;
            lastnamedatt[level] = LNA_NOTHING;
          }
        }
        continue;
      }
      
      if (is_digit(code[pos])) //We're at a number
      {
        //Should be a previous operator, should have been an assignment operator
        int introuble=0;
        if (!(plevel>0))
        {
          if (!assop[level] and !(levelt[level] == LEVELTYPE_FOR_PARAMETERS and statement_pad[level] == 2))
          introuble=1;//{ error="Assignment operator expected before numeric constant"; return pos; }
          if (lastnamed[level] != LN_OPERATOR && !(inlist() and lastnamed[level] == LN_NOTHING))
          introuble=2;//{ error="Operator expected before numeric constant"; return pos; }
        }

        while (is_digit(code[pos])) pos++;
        while (is_useless(code[pos])) pos++;
        if (!decimal_named)
        if (code[pos]=='.')
        {
          int cpos=pos+1;
          while (is_useless(code[cpos])) cpos++;
          if (is_digit(code[cpos]))
          {
            pos=cpos;
            while (is_digit(code[pos])) pos++;
          }
          else if (introuble>0)
          {
            if (is_letter(code[cpos]))
            {
              if (statement_completed(lastnamed[level])){
                int cs = close_statement(code,pos);
                if (cs != -1) return cs;
              }
            }
            else
            {
              if (introuble==1)
              { error="Assignment operator expected before numeric constant"; return pos; }
              if (introuble==2)
              { error="Operator expected before numeric constant"; return pos; }
            }
          }
        }
        if (introuble>0)
        {
          pt cpos=pos;
          while (is_useless(code[cpos])) cpos++;
          if (code[cpos]=='.')
          {
            if (statement_completed(lastnamed[level])){
              int cs = close_statement(code,pos);
              if (cs != -1) return cs;
            }
          }
          else
          {
            if (introuble==1)
            { error="Assignment operator expected before numeric constant.."; return pos; }
            if (introuble==2)
            { error="Operator expected before numeric constant.."; return pos; }
          }
        }
        decimal_named=0; //reset this, it serves only one purpose
        lastnamed[level]=LN_DIGIT,
        lastnamedatt[level] = LNA_NOTHING;
        continue;
      }
      
      
      switch (code[pos])//It's a symbol: Not a letter or digit
      {
        case ';':
            {
              if (plevel>0 and plevelt[plevel] != PLT_FORSTATEMENT)
                { error="Semicolon does not belong inside set of parentheses"; return pos; }
              if (lastnamed[level] == LN_OPERATOR)
                { error="Secondary expression expected before semicolon"; return pos; }
              if (statement_completed(lastnamed[level]))
              {
                if((levelt[level] == LEVELTYPE_IF and statement_pad[level] == 3)
                or (levelt[level] == LEVELTYPE_LOOP))
                  statement_pad[level]--;
                int cs = close_statement(code,pos);
                if (cs != -1) return cs;
              }
              else if (plevel > 0 and plevelt[plevel] == PLT_FORSTATEMENT)
                statement_pad[level]--;
              indeclist[level] = false;
              lastnamed[level] = LN_NOTHING,
              lastnamedatt[level] = LNA_NOTHING;
            }
          pos++; continue;
        
        
        case ',':
            {
              //if (!inlist())
               //{ error="Comma outside of function parameters or declarations"; return pos; }
              if (lastnamed[level] == LN_OPERATOR)
                { error="Secondary expression expected before comma"; return pos; }
              
              //The beauty: if we have unterminated parentheses before the comma, the first error is true.
              //There does not need to be an assignment operator in either case.
              
              lastnamed[level] = LN_NOTHING,
              lastnamedatt[level] = LNA_NOTHING;
            }
          pos++; continue;
        
        
        //Moving on...
        //first priority is to check for a bracket of any sort; () [] {}
        case '(':
            plevel++;
            if (lastnamed[level] == LN_NOTHING or lastnamed[level] == LN_OPERATOR)
            {
              plevelt[plevel] = PLT_PARENTH;
              lastnamed[level] = LN_NOTHING; // So we'll just assume it was.
              lastnamedatt[level] = LNA_NOTHING; // Unset our member flag
              assop[level] = true; // And we'll treat it like a function.
            }
            else
            {
              if (lastnamed[level] == LN_VARNAME) //May be a script.
              {
                //but we can't check that now
                if (lastnamedatt[level] == LNA_MEMBERVAR) // We can't check if it's a member, either, since the type system is so flexible.
                {
                  plevelt[plevel] = PLT_FUNCTION;
                  lastnamed[level] = LN_NOTHING; // So we'll just assume it was.
                  lastnamedatt[level] = LNA_NOTHING; // Unset our member flag
                  assop[level] = true; // And we'll treat it like a function.
                }
                else {
                  error = "Unrecognized function or script `" + last_identifier + "'"; // Otherwise, we can't find any such function or script.
                  return pos; // So we'll bail in error.
                }
              }
              else if (lastnamed[level] == LN_FUNCTION_NAME) //Is a C++ function.
              {
                plevelt[plevel] = PLT_FUNCTION;
                int cf=checkfunction(function_ext,code,pos,len);
                if (cf!=-1)
                  return cf;
                lastnamed[level] = LN_NOTHING;
                lastnamedatt[level] = LNA_NOTHING;
                assop[level] = true;
              }
              else if (lastnamed[level] == LN_TYPE_NAME) //Is a C++ function.
              {
                plevelt[plevel] = PLT_PARENTH;
                lastnamed[level] = LN_NOTHING;
                lastnamedatt[level] = LNA_NOTHING;
              }
              else if (lastnamed[level] == LN_FOR)
              {
                plevelt[plevel] = PLT_FORSTATEMENT;
                lastnamed[level] = LN_NOTHING;
                lastnamedatt[level] = LNA_NOTHING;
              }
            }
          pos++; continue;
        
        case ')':
            {
              if (lastnamed[level] == LN_OPERATOR) { 
                error = "Expected secondary expression before closing parenthesis";
                return pos;
              }
              if (!plevel)
                return (error="Unpaired closing parenthesis at this point", pos);
              if (plevelt[plevel] != PLT_PARENTH and plevelt[plevel] != PLT_FUNCTION)
              {
                if (plevelt[plevel] == PLT_FORSTATEMENT)
                {
                  if (statement_completed(levelt[level],pos)) {
                    int cs = close_statement(code,pos);
                    if (cs != -1) return cs;
                  }
                  if (statement_pad[level] != 0 and statement_pad[level] != 1) { // for (something;something;) is fine, too.
                    error = "Too soon for closing parentheses to for() statement " + tostring(statement_pad[level]);
                    return pos;
                  }
                  level--, plevel--;
                  pos++; continue;
                }
                
                if (plevelt[plevel] == PLT_CAST) {
                  plevel--; lastnamed[level] = LN_OPERATOR; //Casts are the equivalent to unary operators
                  lastnamedatt[level] = LNA_NOTHING;
                  pos++; continue;
                }
                
                error="Unexpected closing parenthesis at this point";
                return pos;
              }
              else
                lastnamed[level] = LN_VALUE,
                lastnamedatt[level] = LNA_NOTHING;
              plevel--;
            }
          pos++; continue;
        
        
        case '[':
            {
              if (lastnamed[level] != LN_VARNAME and lastnamed[level] != LN_VALUE and lastnamed[level] != LN_NEW_TYPE)
                { error="Invalid use of `[' symbol for " + tostring(lastnamed[level]); return pos; }
              plevel++;
              plevelt[plevel] = PLT_BRACKET;
              lastnamed[level] = LN_OPERATOR,
              lastnamedatt[level] = LNA_NOTHING;
              pos++;
              continue;
            }
          pos++; continue;
        case ']':
            {
              if (!plevel)
                return (error="Unpaired closing bracket at this point", pos);
              if (plevelt[plevel] != PLT_BRACKET)
                return (error="Unexpected closing bracket at this point", pos);
              lastnamed[level]=LN_VARNAME,
              lastnamedatt[level] = LNA_NOTHING;
              plevel--;
            }
          pos++; continue;


        case '{':
            {
              /*int einfo;
              if (levelt[level]==LEVELTYPE_SWITCH) einfo=LEXTRAINFO_SWITCHBLOCK;
              if (levelt[level]==LEVELTYPE_DO) einfo=LEXTRAINFO_LOOPBLOCK;
              if (levelt[level]==LEVELTYPE_FOR_PARAMETERS) einfo=LEXTRAINFO_LOOPBLOCK;
              if (levelt[level]==LEVELTYPE_GENERAL_STATEMENT)
              {
                if (levelei[level]==LEXTRAINFO_WHILE) einfo=LEXTRAINFO_LOOPBLOCK;
                if (levelei[level]==LEXTRAINFO_WHILE) einfo=LEXTRAINFO_LOOPBLOCK;
              }*/
              bool isbr = (levelt[level] == LEVELTYPE_SWITCH);
              
              quickscope();

              if (lastnamed[level] == LN_OPERATOR)
              { error="Unexpected brace at this point"; return pos; }
              if (plevel) {
                error = plevelt[plevel] == PLT_TEMPLATE_PARAMS ? "Expected closing triangle bracket before brace" : plevelt[plevel] == PLT_BRACKET ? "Expected closing bracket before brace" : "Expected closing parenthesis before brace";
                return pos;
              }
              
              if (statement_completed(lastnamed[level])) {
                if((levelt[level] == LEVELTYPE_IF and statement_pad[level] == 3)
                or (levelt[level] == LEVELTYPE_LOOP or levelt[level] == LEVELTYPE_FOR_PARAMETERS))
                  statement_pad[level]--;
                int cs = close_statement(code,pos);
                if (cs != -1) return cs;
              }
              
              if (levelt[level] == LEVELTYPE_DO and statement_pad[level]-- != 2)
              { error = "Unexpected '{' in `do' statement"; return pos; }

              level += !isbr;
              levelt[level] = (isbr?LEVELTYPE_SWITCH_BLOCK:LEVELTYPE_BRACE);
              lastnamed[level] = LN_NOTHING; //This is the beginning of a glorious new level
              lastnamedatt[level] = LNA_NOTHING;
              statement_pad[level] = -1;
              assop[level] = 0;
            }
          pos++; continue;
        case '}':
            {
              dropscope();
              
              if (level<=0)
                { error="Unexpected closing brace at this point"; return pos; }
              
              if (lastnamed[level]==LN_OPERATOR)
              { error="Expected secondary expression before closing brace"; return pos; }
              
              if (statement_completed(lastnamed[level])) {
                int cs=close_statement(code,pos);
                if (cs != -1) return cs;
              }
              
              lower_to_level(LEVELTYPE_BRACE,"`}' symbol");
              if (level<=0)
                { error="Unexpected closing brace at this point"; return pos; }
              level--;
              //statement_pad[level] = -1;
              lastnamed[level] = LN_NOTHING;//LN_CLOSING_SYMBOL; //We closed at the beginning of this group.
              lastnamedatt[level] = LNA_NOTHING;
              //this will invoke the next statement to close the current statement
            }
          pos++; continue;
        
        
        case '"':
            {
              if (!assop[level] && !(plevel>0))
              { error="Assignment operator expected before string literal"; return pos; }
              if (lastnamed[level] != LN_OPERATOR && !(plevel>0) && !(inlist() and lastnamed[level]==LN_NOTHING))
              { error="Operator expected before string literal"; return pos; }
              
              if (setting::use_cpp_escapes)
                while (code[++pos]!='"') { 
                  if (code[pos] == '\\')
                    pos++;
                }
              else
                while (code[++pos]!='"');
              
              lastnamed[level] = LN_DIGIT;
              lastnamedatt[level] = LNA_NOTHING;
            }
          pos++; continue;
        
        case '\'':
            {
              if (!assop[level] && !(plevel>0))
              { error="Assignment operator expected before string literal"; return pos; }
              if (lastnamed[level] != LN_OPERATOR && !(plevel>0) && !(inlist() and lastnamed[level]==LN_NOTHING))
              { error="Operator expected before string literal"; return pos; }
              
              if (setting::use_cpp_escapes)
                while (code[++pos]!='\'') { 
                  if (code[pos] == '\\')
                    pos++;
                }
              else
                while (code[++pos]!='\'');
              
              lastnamed[level]=LN_DIGIT;
              lastnamedatt[level] = LNA_NOTHING;
            }
          pos++; continue;
        
        
        
        case '.':
            {
              if (lastnamed[level] == LN_DIGIT || lastnamed[level]==LN_VARNAME || lastnamed[level]==LN_VALUE || lastnamed[level]==LN_LOCGLOBAL)
              {
                pos++;
                while (is_useless(code[pos])) pos++;
                if (!is_letter(code[pos])) { error="Identifier expected following '.' symbol"; return pos; }
                lastnamed[level]=LN_OPERATOR;
                lastnamedatt[level] = LNA_DOT;
                continue;
              }
              else
              {
                //in this case, there could be a number following
                //Syntax checking on the positioning of the number will be handled next iteration
                const int dpos = pos;
                while (is_useless(code[++pos]));
                if (is_digit(code[pos]))
                { decimal_named=1; continue; }
                else
                { error="Unexpected symbol '.' in expression"; return dpos; }
              }
            }
          break;
        
        
        //Now we can assume it's supposed to be an operator
        
        case '=': //handle = and == first
            {
              if (code[pos+1]=='=') //it's operator==
              {
                if (!assop[level] && !(plevel>0))
                { error="Assignment operator expected before comparison operator"; return pos; }
                if (lastnamed[level]==LN_CLOSING_SYMBOL || lastnamed[level]==LN_OPERATOR || lastnamed[level]==LN_NOTHING)
                { error="Expected primary expression before comparison operator"; return pos; }
                pos+=2; lastnamed[level]=LN_OPERATOR;
                lastnamedatt[level] = LNA_NOTHING;
              }
              else //operator=
              {
                assop[level]=1;
                pos+=1; lastnamed[level]=LN_OPERATOR;
                lastnamedatt[level] = LNA_NOTHING;
              }
            }
          continue;
          
        //TERNARY
        case '?':
            if (lastnamed[level] != LN_VARNAME and lastnamed[level] != LN_VALUE and lastnamed[level] != LN_DIGIT)
            { error = "Unexpected ternary operator at this point"; return pos; }
            levelt[++level] = LEVELTYPE_TERNARY;
            lastnamedatt[level] = LNA_NOTHING;
            lastnamed[level] = LN_OPERATOR;
            statement_pad[level] = 2;
            assop[level] = true;
          pos++; continue;
        
        case ':': //The unhappiest symbol in the C language!
            {
              if (code[pos+1] == '=')
                { pos++; continue; }
              if (levelt[level] == LEVELTYPE_TERNARY)
              {
                while (statement_pad[level] != 2)
                {
                  if (level > 1 and levelt[level-1] == LEVELTYPE_TERNARY)
                    level--;
                  else
                  { error = "Already used a colon this ternary expression"; return pos; }
                }
                if (lastnamed[level] == LN_OPERATOR)
                { error = "Expected secondary expression to ternary operator before colon"; return pos; }
                statement_pad[level]--;
                lastnamed[level] = LN_OPERATOR;
                lastnamedatt[level] = LNA_NOTHING;
              }
              else if (plevel>0)
                { error="Unexpected colon at this point"; return pos; }
              else if (level>0 and levelt[level]==LEVELTYPE_CASE)
              {
                if (statement_pad[level] != 1 || lastnamed[level]==LN_OPERATOR)
                { error="Expression expected before `:' symbol"; return pos; }
                lastnamed[level]=LN_CLOSING_SYMBOL;
                lastnamedatt[level] = LNA_NOTHING;
              }
              else if (lastnamed[level]==LN_VARNAME and !assop[level])
              {
                error="Goto labels not yet supported"; return pos;
                lastnamed[level]=LN_NOTHING; //Pretend you never saw the identifier
                lastnamedatt[level] = LNA_NOTHING;
              }
              else
              {
                error="Unexpected symbol `:' at this point";
                return pos;
              }
            }
          pos++; continue;
        
        //Now we check if it's an operator... last chance
        ///////////////////////////////////////////////////
        default:
            if ((code[pos+1]=='=') or (code[pos+1]==code[pos] && code[pos+2]=='=')) //is an assignment operator (or potentially a comparison)
            {
              if (code[pos+1] == '=') //one character base operator, followed by =
              {
                switch (code[pos])
                {
                  case '+': case '-': case '*': 
                  case '/': case '&': case '|': 
                  case '^': //Same code as two below follwing case '<':
                    if (lastnamed[level]==LN_VALUE && !assop[level])
                    { error="Cannot assign to a function, use == to test equality"; return pos; }
                    if (lastnamed[level]!=LN_VARNAME && !assop[level])
                    { error="Variable name expected before assignment operator"; return pos; }
                    break;

                  case '>': case '<': case '!':
                    if (!assop[level] && !(plevel>0))
                    { error="Assignment operator expected before comparison operator"; return pos; }
                    if (lastnamed[level]==LN_CLOSING_SYMBOL || lastnamed[level]==LN_OPERATOR || lastnamed[level]==LN_NOTHING)
                    { error="Expected primary expression before comparison operator"; return pos; }
                    break;

                  default:
                    error="Invalid operator at this point";
                    return pos;
                }
                assop[level]=1;
                lastnamed[level]=LN_OPERATOR;
                pos+=2;
                continue;
              }
              else //two character base operator, followed by =
              {
                switch (code[pos])
                {
                  case '>':
                  case '<': //same code as above following case '^':
                    if (lastnamed[level]!=LN_VARNAME)
                    { error="Variable name expected before assignment operator"; return pos; }
                    if (lastnamed[level]==LN_VALUE)
                    { error="Cannot assign to left operand; use == to test equality"; return pos; }
                    assop[level]=1;
                    lastnamed[level]=LN_OPERATOR;
                    break;

                  default:
                    error="Invalid operator at this point";
                    return pos;
                }
                pos+=3;
                continue;
              }
            }
            else //is not an assignment operator
            {
              switch (code[pos])
              {
                case '+': case '-':
                    if (setting::use_incrementals and code[pos+1] == code[pos]) //Operators ++ and --
                    {
                      if (lastnamed[level] == LN_OPERATOR) {
                        pos += 2;
                        break;
                      }
                      if (lastnamed[level] == LN_VARNAME) {
                        assop[level] = true;
                        lastnamed[level] = LN_DIGIT;
                        pos += 2; break;
                      }
                      if (lastnamed[level] == LN_NOTHING or lastnamed[level] == LN_CLOSING_SYMBOL) {
                        assop[level] = true;
                        lastnamed[level] = LN_OPERATOR;
                        pos += 2; break;
                      }
                    }
                  //Intentional overflow into next case label otherwise.
                case '*': case '&':
                    if (!assop[level] && !(plevel>0))
                    { 
                      if (indeclist[level])
                      { pos++; continue; }
                      error="Assignment operator expected before arithmetic operators"; return pos;
                    }
                    //Since +, - and * can also be unary, we do not have to check for previous identifier
                    lastnamed[level] = LN_OPERATOR;
                    while (code[pos]=='+' or code[pos]=='-' or code[pos]=='*' or code[pos]=='&' or is_useless(code[pos]))
                      pos++;
                  break;

                case '/':
                    if (code[pos+1] == '/')
                    {
                      pos++;
                      while (code[++pos] != '\n' and code[pos] != '\r' and pos < len);
                      continue;
                    }
                    else if (code[pos+1] == '*')
                    {
                      pos++;
                      while ((code[pos++] != '*' or code[pos] != '/') and pos < len);
                      pos++; continue;
                    }
                  //overflow into next otherwise
                  goto they_see_me_rollin; // Skip the next case. It's pure evil :3
                
                case '>': case '<':
                  if (lastnamed[level] == LN_TYPE_NAME or (plevel and plevelt[plevel] == PLT_TEMPLATE_PARAMS))
                    goto FUCKING_HELL_A_TEMPLATE;
                
                case '%': case '|': case '^':
                  they_see_me_rollin:
                    if ((!assop[level] && !(plevel>0)) and !(levelt[level] == LEVELTYPE_FOR_PARAMETERS and statement_pad[level] == 2))
                    { error="Assignment operator expected before any secondary operators"; return pos; }
                    if (lastnamed[level] != LN_VARNAME && lastnamed[level] != LN_DIGIT && lastnamed[level] != LN_VALUE)
                    { error="Expected primary expression before operator"; return pos; }
                    lastnamed[level]=LN_OPERATOR;
                    if (code[pos+1]==code[pos])
                    {
                      if (code[pos]!='^' && code[pos]!='|' && code[pos]!='^' && code[pos]!='<' && code[pos]!='>')
                      { error="Invalid operator at this point"; return pos; }
                      pos++;
                    }
                    if (code[pos]=='<' and code[pos+1]=='>')
                      pos++;
                    pos++;
                  break;
                
                FUCKING_HELL_A_TEMPLATE:
                    if (code[pos] == '<')
                    {
                      plevel++; plevelt[plevel] = PLT_TEMPLATE_PARAMS;
                      level++;   levelt[ level] = LEVELTYPE_TEMPLATE_P;
                      lastnamed[level] = LN_NOTHING; pos++;
                    }
                    else if (code[pos] == '>')
                    {
                      if (lastnamed[level] != LN_NOTHING and lastnamed[level] != LN_VALUE and lastnamed[level] != LN_DIGIT and lastnamed[level] != LN_TYPE_NAME)
                        { error = "Dunno what the hell that is, but it doesn't belong in a template hurrr"; return pos; }
                      plevel--; level--;
                      lastnamed[level] = LN_TYPE_NAME; pos++;
                    }
                  break;
                
                case '!': case '~': //we already know the next symbol is not '=', so this is unary
                    if (lastnamed[level] != LN_OPERATOR and lastnamed[level] != LN_NOTHING)
                    { error="Unexpected unary operator following value"; return pos; }
                    pos++;
                  break; //lastnamed is already operator, so just break
                  
                case '$':
                  if (!is_hexdigit(code[pos+1]))
                    return (error = "Unexpected dollar sign: used to indixate hex literal", pos);
                  while (is_hexdigit(code[++pos]));
                  lastnamed[level] = LN_DIGIT;
                break;
                
                default:
                    error="Unexpected symbol reached";
                  return pos;
              }
              continue;
            }
          //End of assignment operator checking
      }
      //End of switch(code[pos])
    }
    
    //We're now at the end of the code. Perform a couple checks...
    if (plevel>1)
     { error="Code ends with "+tostring(plevel)+" unterminated parentheses"; return pos; }
    if (plevel==1)
     { error="Code ends with one unterminated parenthesis"; return pos; }

    if (lastnamed[level]==LN_VARNAME and !assop[level])
     { error="Assignment operator expected before end of code"; return pos; }


    if (statement_completed(lastnamed[level])) {
      int cs = close_statement(code,pos);
      if (cs != -1) return cs;
    }

    if (lastnamed[level]==LN_OPERATOR)
     { error="Expected secondary expression before end of code"; return pos; }
    
    while (level>0)
    {
      if (levelt[level]==LEVELTYPE_BRACE or levelt[level]==LEVELTYPE_SWITCH_BLOCK)
        { error="Symbol `}' expected before end of code"; return pos; }
      if (levelt[level]==LEVELTYPE_DO)
        { error="Statement `until' expected before end of code"; return pos; }
      else level--;
    }

    return -1;
  }
}
