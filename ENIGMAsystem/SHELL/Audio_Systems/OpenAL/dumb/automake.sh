# Makefile generator instantiated from /ENIGMAsystem/SHELL/Developer/.

echo "#Awesome Locally Generated Makefile" > Makefile

echo "" >> Makefile;
echo ".objs:" >> Makefile;
echo "	-mkdir .objs" >> Makefile;
echo "" >> Makefile

for file in src/*/*.c ;
  do
  {
    npf="dumb_${file##*/}"
    printf ".objs/dumb_${npf%.c}.o: $file" >> Makefile;
    for i in `c_incl $file | gawk '/\/usr\/include/ { next } { print } '`;
    do
      printf " $i" >> Makefile;
    done;
    echo "" >> Makefile;
    
    echo "	gcc -c $file		-o .objs/dumb_${npf%.c}.o \$(ECFLAGS)"  >> Makefile;
  };
  done;

echo "" >> Makefile;

  printf "\$(DEST)/libdumb.a: .objs " >> Makefile;
  for file in src/*/*.c ;
    do npf="dumb_${file##*/}"
    printf ".objs/dumb_${npf%.c}.o " >> Makefile; 
    done;
  echo "" >> Makefile;
  echo "	ar r \$(DEST)/libdumb.a .objs/*.o" >> Makefile;
  echo "" >> Makefile;

echo "static: \$(DEST)/libdumb.a" >> Makefile;
echo "" >> Makefile;
echo "clean:" >> Makefile;
echo "	-rm .objs/*" >> Makefile;

