Sep 17, 2019
  - incoming records logged into file instead of database, in xml format

ver 2.4.2,  Jan 23, 2019
  - tuned up summary email

ver 2.4.1,  Sep 14, 2018
  - moved from subversion to github
  
ver 2.4.0,  May 21, 2018
  - source file change: columns {"Chromosomal Start" "Chromosomal Stop"} have been replaced by columns
    {"Chromosomal Start - GRCh37.p13" "Chromosomal Stop - GRCh37.p13" "Chromosomal Start - GRCh38.p7" "Chromosomal Stop - GRCh38.p7"}

ver 2.3.9,  Dec 13, 2017
  - updated jars

ver 2.3.8 as of Jul 28, 2017
  - percentage threshold to delete stale xdb ids is now stored in the application properties file

ver 2.3.7 as of May 8, 2017
  - qcThreadCount is no longer hard-coded: it can be tuned up in AppConfigure.xml

ver 2.3.6 as of Mar 28, 2017
  - updated build from ANT to GRADLE

ver 2.3.5 as of Feb 6, 2017
  - fixed downloading problems: FileDownloader does not use HttpClient anymore

ver 2.3.4 as of Nov 16, 2016
  - updated code: Ent rez ID --> NCBI Gene Id -- per RGDD-512

ver 2.3.3 as of Mar 11, 2016
  - file format change: new column 'HGNC ID';
    updated jars, updated logging to log4j

ver 2.3.2 as of Dec 30, 2014
  - updated jars, fixed logging

ver 2.3.1 as of Feb 3, 2014
  - changed url to genes.zip file; updated rgdcore.jar

ver 2.3 as of October 17, 2013
  - file format change: 3 new columns added at the end of source file
  - added a log file to track inserted PharmGKB ids

ver 2.2 as of December 21, 2012
  - file format change: new 11th column at the end of the file named "Has CPIC Dosing Guideline"
  - updated rgdcore: inserts to RGD_ACC_XDB table use sequence

ver 2.1 as of March 14, 2012
  file format change
