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

/*
  DEVELOPERS:
  Don't use these. Really. Unless you know what you are doing. You could easily cause another
  segmentation fault if you aren't careful. And I say another because of how many I've had to
  deal with making them. Reason being, each reiteration can contain any number of pointers to
  other types or related values. For example, take hspeed, vspeed, direction, and speed: Each
  keeps tabs on the other three. When one is modified, it modifies the other three as needed.
*/

#ifndef MFV_ALREADY_INCLUDED
#include "multifunction_variant.h"
#else
#undef MFV_ALREADY_INCLUDED
#endif
#include "var_te.h"

#undef  types_extrapolate_alldec_i
#undef  types_extrapolate_alldec_ib
#define types_extrapolate_alldec_i(op, stringops, sentiments)\
 types_extrapolate_real_p  (TYPEPURPOSE& TYPEPURPOSE::operator op, { rval.d op x; sentiments;  return *this; } )\
 types_extrapolate_string_p(TYPEPURPOSE& TYPEPURPOSE::operator op, { stringops;   sentiments; return *this; } )\
 TYPEPURPOSE& TYPEPURPOSE::operator op (const variant &x)          { rval.d op x; sentiments; return *this; }\
 TYPEPURPOSE& TYPEPURPOSE::operator op(const var &x)               { rval.d op x; sentiments; return *this; }
#define types_extrapolate_alldec_ib(op, sentiments)\
 types_extrapolate_real_p  (TYPEPURPOSE& TYPEPURPOSE::operator op##=, { rval.d = long(rval.d) op long(x); sentiments;  return *this; } )\
 types_extrapolate_string_p(TYPEPURPOSE& TYPEPURPOSE::operator op##=, { terrortrue();                     sentiments; return *this; } )\
 TYPEPURPOSE& TYPEPURPOSE::operator op##= (const variant &x)          { rval.d = long(rval.d) op long(x); sentiments; return *this; }\
 TYPEPURPOSE& TYPEPURPOSE::operator op##= (const var &x)              { rval.d = long(rval.d) op long(x); sentiments; return *this; }

namespace enigma
{
  types_extrapolate_alldec_i(=,   sval = x,     TYPEFUNCTION; );
  types_extrapolate_alldec_i(+=,  sval += x,    TYPEFUNCTION; );
  types_extrapolate_alldec_i(-=,  terrortrue(), TYPEFUNCTION; );
  types_extrapolate_alldec_i(*=,  terrortrue(), TYPEFUNCTION; );
  types_extrapolate_alldec_i(/=,  terrortrue(), div0c(x); TYPEFUNCTION; );
  types_extrapolate_alldec_ib(<<, TYPEFUNCTION; );
  types_extrapolate_alldec_ib(>>, TYPEFUNCTION; );
  types_extrapolate_alldec_ib(&,  TYPEFUNCTION; );
  types_extrapolate_alldec_ib(|,  TYPEFUNCTION; );
  types_extrapolate_alldec_ib(^,  TYPEFUNCTION; );
}

#undef  types_extrapolate_alldec_i
