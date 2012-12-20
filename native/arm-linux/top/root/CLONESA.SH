#!/bin/bash
#
# clonesa.sh: a small script to clone a sidearm root filesystem
# $Id: CLONESA.SH,v 1.1 2008/11/04 22:11:30 bobh Exp $
#
# caveats:
#
# [1] This script must be run as root
#
# [2] Edit this script and fill in the USERDIR and NAME values below
#     these values correspond to the directory path 
#     /mnt/hda/$USERDIR/$NAME, and are the name of the user directory 
#     and 'run' respectively. 
#
# [3] /mnt/hda/$USERDIR should exist or the script won't run
#
# fill in the USERDIR and NAME values below (e.g. timm and test1)
#
USERDIR="backups"
NAME="dup_mse-axis_rootfs_february16_2007"
#
# 
# don't edit below this point, unless you know what you're doing
#
#

MYUID=`/usr/bin/id -u`

# if [$MYUID -neq 0]; then
#   echo "Please logout, login as root, and rerun the script. Thank you."
#   exit 0
# else
#   echo "You're logged in as root -- that's good"
# fi

if [ -e ${DESTROOT} ]; then
  echo "Using home directory: ${DESTHOME}"
else
  echo "Directory ${DESTHOME}, not found. Please create it and re-run script."
  /bin/sleep 3
  exit 0
fi

CFPREFIX="/mnt/hda"
DESTDIR="${CFPREFIX}/${USERDIR}/${NAME}"
DESTHOME="${CFPREFIX}/${USERDIR}"
OUTFILE="${CFPREFIX}/${USERDIR}/${NAME}.jffs2"
TARFILE="${CFPREFIX}/${USERDIR}/${NAME}.tar"
TARGZFILE="${CFPREFIX}/${USERDIR}/${NAME}.tar.gz"

CP="/bin/cp"
CPOPTS=" --preserve --target-directory=${DESTDIR} -R -v "
MKDIR="/bin/mkdir"
MKDIROPTS=" -v "
LN="/bin/ln"
LNOPTS=" --symbolic -v "

echo "Destination dir: "${DESTDIR}
echo "cp options: "${CPOPTS}
echo "jffs2 file: "${OUTFILE}
echo "tar file: "${TARFILE}
echo "tar.gz file: "${TARGZFILE}

if [ -e ${DESTDIR} ]; then
  echo "Directory ${DESTDIR} already exists, it will be removed"
fi

echo "."
echo "."
echo "."
echo "IMPORTANT: you need to be logged in as root. If not, exit now!"
echo "."
echo "."
echo "Starting in 10 seconds, ^Z to exit now"
/bin/sleep 10

if [ -e ${OUTFILE} ]; then
  echo "Removing existing jffs2 image ${OUTFILE}"
  rm -f ${OUTFILE}
fi

if [ -e ${TARFILE} ]; then
  echo "Removing existing tar file ${TARFILE}"
  rm -f ${TARFILE}
fi

if [ -e ${TARGZFILE} ]; then
  echo "Removing existing tar.gz file ${TARGZFILE}"
  rm -f ${TARGZFILE}
fi

if [ -e ${DESTDIR} ]; then
  echo "Removing existing build directory ${DESTDIR}"
  rm -rf ${DESTDIR}
fi

${MKDIR} ${MKDIROPTS} ${DESTDIR}
pushd ${DESTDIR}

echo "copying files..."
${CP} ${CPOPTS} /bin /dev /etc /home /j9 /lib /linuxrc /linuxrc.debian /root /sbin /usr /.ramfs.tar.gz
echo "making dirs and symlinks in ${DESTDIR}"
${MKDIR} ${MKDIROPTS} mnt
${MKDIR} ${MKDIROPTS} mnt/hda
${MKDIR} ${MKDIROPTS} mnt/nfs
${MKDIR} ${MKDIROPTS} mnt/ramfs
${MKDIR} ${MKDIROPTS} proc
${LN} ${LNOPTS} mnt/ramfs/tmp tmp
${LN} ${LNOPTS} mnt/ramfs/var var
echo "setting ${DESTDIR}/etc/hostname to sidearm"
rm -f ${DESTDIR}/etc/hostname
echo "sidearm" > ${DESTDIR}/etc/hostname
echo "removing old ssh keys..."
if [ -e ${DESTDIR}/etc/ssh/ssh_host_dsa_key ]; then
  echo "removing ssh_host_dsa_key so it is generated on new host"
  rm -f ${DESTDIR}/etc/ssh/ssh_host_dsa_key
fi
if [ -e ${DESTDIR}/etc/ssh/ssh_host_dsa_key.pub ]; then
  echo "removing ssh_host_dsa_key.pub so it is generated on new host"
  rm -f ${DESTDIR}/etc/ssh/ssh_host_dsa_key.pub
fi
echo "adding /etc/gbversion..."
/bin/date > ${DESTDIR}/etc/gbversion
echo "Filesystem clone complete, now generating image file..."
echo "making jffs2 image file: ${OUTFILE}"
/sbin/mkfs.jffs2 -o ${OUTFILE} -e 0x20000 -r ${DESTDIR} -p -l
echo "making tar file of cloned filesystem: ${TARFILE}"
/bin/tar cvf ${TARFILE} --preserve .
echo "compressing tar file: ${TARFILE}"
/bin/gzip -1 -v ${TARFILE}
if [ -e ${DESTDIR} ]; then
  echo "Removing existing build directory ${DESTDIR}"
  rm -rf ${DESTDIR}
fi
popd
echo "clonesa done."
