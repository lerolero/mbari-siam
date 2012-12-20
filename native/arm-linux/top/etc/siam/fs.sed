## fs.sed
# strip file size
# original for UNIX: s/\(.*users *\)\([0-9]*\)\( .*\)/\2/
# for CYGWIN/UNIX
# This should be installed in /etc/siam subdirectory on the target
s/\([a-z-]* *\)\([0-9]* *\)\([A-Za-z]* *\)\([A-Za-z]* *\)\([0-9]*\)\(.*\)/\5/
