# download genes.zip file from PharmGKB website and load PharmGKB ids into table RGD_ACC_XDB
#
. /etc/profile
APPNAME="pharmgkb-pipeline"
APPDIR=/home/rgddata/pipelines/$APPNAME
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" = "REED" ]; then
  EMAIL_LIST="rgd.devops@mcw.edu jrsmith@mcw.edu"
fi

# pipefail: exit status of the pipe is the java exit status, not tee's
set -o pipefail

cd $APPDIR
java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/$APPNAME.jar "$@" 2>&1 | tee run.log

if [ $? -eq 0 ]; then
  mailx -s "[$SERVER] pharmgkb-pipeline OK!" $EMAIL_LIST < $APPDIR/logs/summary.log
else
  mailx -s "[$SERVER] pharmgkb-pipeline FAILED!" $EMAIL_LIST < $APPDIR/run.log
fi
