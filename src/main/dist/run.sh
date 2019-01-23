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
java -Dspring.config=$APPDIR/../properties/default_db.xml \
    -Dlog4j.configuration=file://$APPDIR/properties/log4j.properties \
    -jar lib/$APPNAME.jar "$@" | tee run.log 2>&1

mailx -s "[$SERVER] PharmGKB pipeline OK!" $EMAIL_LIST < $APPDIR/logs/summary.log

