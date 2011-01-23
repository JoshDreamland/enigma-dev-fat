# Makefile generator instantiated from /ENIGMAsystem/SHELL/Developer/.

echo "#Awesome Makefile generated by custom ENIGMAsystem/Developer/automake.sh" > Makefile

echo "" >> Makefile;
echo "odir:" >> Makefile;
echo "	-mkdir .objs" >> Makefile;
echo "" >> Makefile

for file in *.cpp ;
  do
  {
    printf ".objs/${file%.cpp}.o: $file" >> Makefile;
    for i in `c_incl $file | gawk '/\/usr\/include/ { next } { print } '`;
    do
      printf " $i" >> Makefile;
    done;
    echo "" >> Makefile;
    
    echo "	\$(CXX) -c $file		-o .objs/${file%.cpp}.o \$(FLAGS)"  >> Makefile;
  };
  done;

echo "" >> Makefile;
printf "\$(DEST)/libalure.a: odir " >> Makefile
  for file in *.cpp ;
    do printf ".objs/${file%.cpp}.o " >> Makefile; 
    done;
echo "" >> Makefile;
echo "	ar r \$(DEST)/libalure.a .objs/*.o" >> Makefile;
echo "" >> Makefile;

echo "static: \$(DEST)/libalure.a" >> Makefile;
echo "" >> Makefile;
echo "clean:" >> Makefile;
echo "	-rm .objs/*" >> Makefile;

