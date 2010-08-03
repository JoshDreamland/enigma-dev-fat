This folder contains dumb-0.9.3.
The files included under src/ and include/ are unmodified from the distribution,
but as per the less questionable terms of the license, this file lists what has
been changed outside of them.

1) The directories under include/ have been copied into each of the directories
   under src/. This is to keep DUMB modular without having to pass any specific
   parameters to the compiler; namely, -I ./include/.

2) The makefile has been replaced with one to be used by the rest of the ENIGMA
   engine. It is much like the other makefiles in the series.
