# download genes.zip file from PharmGKB website and load PharmGKB ids into table RGD_ACC_XDB
#
. /etc/profile
APPNAME=PharmGKB
APPDIR=/home/rgddata/pipelines/$APPNAME
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" = "REED" ]; then
  EMAIL_LIST=rgd.developers@mcw.edu,jrsmith@mcw.edu
fi

# run java app by calling gradle-generated wrapper script
cd $APPDIR
DB_OPTS="-Dspring.config=$APPDIR/../properties/default_db.xml"
LOG4J_OPTS="-Dlog4j.configuration=file://$APPDIR/properties/log4j.properties"
declare -x "PHARM_GKB_OPTS=$DB_OPTS $LOG4J_OPTS"
bin/$APPNAME "$@" 2>&1 | tee run.log

grep status $APPDIR/logs/status.log | mailx -s "[$SERVER] PharmGKB pipeline OK!" $EMAIL_LIST

