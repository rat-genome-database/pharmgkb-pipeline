package edu.mcw.rgd.pipelines.pharmgkb;

import edu.mcw.rgd.process.CounterPool;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author mtutaj
 * @since 12/22/11
 */
public class Manager {

    private PreProcessor preProcessor;
    private QCProcessor qcProcessor;
    private Dao dao;

    private final Logger log = LogManager.getLogger("status");
    private String version;
    private String staleIdsDeleteThreshold;
    private String pipelineName;
    private boolean multithreadQC;
    private boolean cacheQCData;

    /**
     * run the pipeline to import PharmGKB Ids for human genes
     * <p>
     * Note: in March 2012 the format of source file genes.tsv changed: columns uniprot_id, is-genotyped, pd, pk
     * have been discontinued. Cross-references column have been introduced.
     * Therefore old pipeline matchers (NCBIGENE-ID  to  ENSEMBL-ID  to  UNIPROTKB-ID)
     * have been replaced with (HGNC-ID  to  NCBIGENE-ID  to  ENSEMBL-ID)
     * @param args cmdline args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        Manager manager = (Manager) (bf.getBean("manager"));

        try {
            manager.run();
        } catch(Exception e) {
            Utils.printStackTrace(e, manager.log);
            throw e;
        }
    }

    void run() throws Exception {

        Date now = new Date();

        log.info(this.getVersion());
        log.info("   "+dao.getConnectionInfo());

        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("   started at "+sdt.format(now));
        log.info("   multithread qc:  "+isMultithreadQC());
        log.info("   cache qc data:  "+isCacheQCData());

        if( isCacheQCData() ) {
            QCDataCache.getInstance().loadCache();
        }

        qcProcessor.setUseDataCache(isCacheQCData());
        qcProcessor.setDao(dao);

        // load number of xdb ids loaded so far by PharmGKB pipeline
        Date dt = Utils.addHoursToDate(now, 1);
        int oldXdbIdCount = dao.getCountOfXdbIdsModifiedBefore(getPipelineName(), dt);
        log.debug("count of PharmGKB IDs in the database: "+oldXdbIdCount);

        CounterPool counters = new CounterPool();

        List<PharmGKBRecord> incomingRecords = preProcessor.process();

        Stream<PharmGKBRecord> incomingRecordsStream;
        if( isMultithreadQC() ) {
            incomingRecordsStream = incomingRecords.parallelStream();
        } else {
            incomingRecordsStream = incomingRecords.stream();
        }

        incomingRecordsStream.forEach( rec -> {

            try {
                qcProcessor.process(rec, counters);
            } catch(Exception e) {
                Utils.printStackTrace(e, log);
                throw new RuntimeException(e);
            }
        });

        deleteObsoleteXdbIds(now, oldXdbIdCount, counters);

        log.info(counters.dumpAlphabetically());

        dt = Utils.addHoursToDate(new Date(), 1);
        int newXdbIdCount = dao.getCountOfXdbIdsModifiedBefore(getPipelineName(), dt);
        int diffCount = newXdbIdCount - oldXdbIdCount;
        NumberFormat plusMinusNF = new DecimalFormat(" +###,###,###; -###,###,###");
        String diffCountStr = diffCount!=0 ? "     difference: "+ plusMinusNF.format(diffCount) : "     no changes";
        log.info("TOTAL PharmGKB ID count: "+Utils.formatThousands(newXdbIdCount)+diffCountStr);
        log.info("");

        log.info("--SUCCESS-- elapsed "+ Utils.formatElapsedTime(now.getTime(), System.currentTimeMillis()));
    }

    void deleteObsoleteXdbIds(java.util.Date now, int xdbIdCount, CounterPool counters) throws Exception {

        // due to differences between app server time and db server time, it is safer to delete data
        // that have been modified more than 1 hour ago
        Date cutoffDate = Utils.addHoursToDate(now, -1);
        int xdbIdCountForDelete = dao.getCountOfXdbIdsModifiedBefore(getPipelineName(), cutoffDate);
        log.debug("count of PharmGKB IDs to be deleted: "+xdbIdCountForDelete);

        // if count of rows to be deleted is greater than 5% of existing rows, that means trouble
        //
        // convert '5%' (or other) to number
        int deleteThresholdInPercent = 5;
        int percentPos = getStaleIdsDeleteThreshold().lastIndexOf('%');
        if( percentPos>0 ) {
            deleteThresholdInPercent = Integer.parseInt(getStaleIdsDeleteThreshold().substring(0, percentPos));
            if( deleteThresholdInPercent<=0 || deleteThresholdInPercent>100 ) {
                deleteThresholdInPercent = 5;
            }
        }
        int deleteThreshold = (deleteThresholdInPercent*xdbIdCount) / 100;
        log.debug("   stale xdb ids delete threshold is "+deleteThreshold + " ("+getStaleIdsDeleteThreshold()+")");

        if( xdbIdCountForDelete > deleteThreshold ) {
            log.info("***** stale xdb ids delete threshold is "+deleteThreshold + " ("+getStaleIdsDeleteThreshold()+")");
            log.warn("***** count of PharmGKB IDs to be deleted ("+xdbIdCountForDelete+") is more than "+getStaleIdsDeleteThreshold()+" threshold -- REVIEW needed");
        }
        else if( xdbIdCountForDelete>0 ) {
            int count = dao.deleteXdbIdsModifiedBefore(getPipelineName(), now);
            counters.add("XDBS_DELETED_FROM_RGD", count);
        }
    }

    public PreProcessor getPreProcessor() {
        return preProcessor;
    }

    public void setPreProcessor(PreProcessor preProcessor) {
        this.preProcessor = preProcessor;
    }

    public QCProcessor getQcProcessor() {
        return qcProcessor;
    }

    public void setQcProcessor(QCProcessor qcProcessor) {
        this.qcProcessor = qcProcessor;
    }

    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }


    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setStaleIdsDeleteThreshold(String staleIdsDeleteThreshold) {
        this.staleIdsDeleteThreshold = staleIdsDeleteThreshold;
    }

    public String getStaleIdsDeleteThreshold() {
        return staleIdsDeleteThreshold;
    }

    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public boolean isMultithreadQC() {
        return multithreadQC;
    }

    public void setMultithreadQC(boolean multithreadQC) {
        this.multithreadQC = multithreadQC;
    }

    public boolean isCacheQCData() {
        return cacheQCData;
    }

    public void setCacheQCData(boolean cacheQCData) {
        this.cacheQCData = cacheQCData;
    }
}
