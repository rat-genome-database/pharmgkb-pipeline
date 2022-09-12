# download genes.zip file from PharmGKB website and load PharmGKB ids into table RGD_ACC_XDB
#
. /etc/profile
APPNAME="pharmgkb-pipeline"
APPDIR=/home/rgddata/pipelines/$APPNAME
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" = "REED" ]; then
  EMAIL_LIST=rgd.devops@mcw.edu,jrsmith@mcw.edu
fi

# run java app by calling gradle-generated wrapper script
cd $APPDIR
java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/$APPNAME.jar "$@" | tee run.log 2>&1

mailx -s "[$SERVER] PharmGKB pipeline OK!" $EMAIL_LIST < $APPDIR/logs/summary.log

