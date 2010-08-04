# Call this file from the directory in question if you would like a generic
# Makefile generated which invokes GCC for each individual file.

echo "#Awesome Locally Generated Makefile" > Makefile

echo "" >> Makefile;
echo "odir:" >> Makefile;
echo "	-mkdir .objs" >> Makefile;
echo "" >> Makefile

for file in src/*/*.c ;
  do
  {
    npf="dumb_${file##*/}"
    printf ".objs/${npf%.c}.o: $file" >> Makefile;
    for i in `c_incl $file | gawk '/\/usr\/include/ { next } { print } '`;
    do
      printf " $i" >> Makefile;
    done;
    echo "" >> Makefile;
    
    echo "	gcc -c $file		-o .objs/${npf%.c}.o"  >> Makefile;
  };
  done;

echo "" >> Makefile;

  printf "\$(DEST)/libdumb.a: odir " >> Makefile;
  for file in src/*/*.c ;
    do npf="dumb_${file##*/}"
    printf ".objs/${npf%.c}.o " >> Makefile; 
    done;
  echo "" >> Makefile;
  echo "	ar r \$(DEST)/libdumb.a .objs/*.o" >> Makefile;
  echo "" >> Makefile;

echo "static: \$(DEST)/libdumb.a" >> Makefile;
echo "" >> Makefile;
echo "clean:" >> Makefile;
echo "" >> Makefile;

