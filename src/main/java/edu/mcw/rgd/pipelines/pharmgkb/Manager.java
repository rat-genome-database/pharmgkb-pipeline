package edu.mcw.rgd.pipelines.pharmgkb;

import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.pipelines.PipelineManager;
import edu.mcw.rgd.process.PipelineLogFlagManager;
import edu.mcw.rgd.process.PipelineLogger;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

/**
 * @author mtutaj
 * @since 12/22/11
 */
public class Manager {

    private PreProcessor preProcessor;
    private QCProcessor qcProcessor;
    private LoadProcessor loadProcessor;
    private Dao dao;

    public static final String PIPELINE_NAME = "PharmGKB";

    private final Logger log = Logger.getLogger("status");
    private PipelineLogger dbLogger = PipelineLogger.getInstance();
    private PipelineLogFlagManager dbFlagManager = new PipelineLogFlagManager(dbLogger);
    private String version;
    private String staleIdsDeleteThreshold;

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
        manager.run();
    }

    void run() throws Exception {

        java.util.Date now = new java.util.Date();

        log.info(this.getVersion());

        dbLogger.init(SpeciesType.HUMAN, "download+process", PIPELINE_NAME);

        qcProcessor.setDao(dao);
        qcProcessor.setDbFlagManager(dbFlagManager);
        qcProcessor.registerDbLogFlags();

        loadProcessor.setDao(dao);

        // load number of xdb ids loaded so far by PharmGKB pipeline
        int xdbIdCount = dao.getCountOfXdbIdsModifiedBefore(PIPELINE_NAME, now);
        log.info("count of PharmGKB IDs in the database: "+xdbIdCount);

        PipelineManager manager = new PipelineManager();
        manager.addPipelineWorkgroup(preProcessor, "PP", 1, 1000);
        manager.addPipelineWorkgroup(qcProcessor, "QC", qcProcessor.getQcThreadCount(), 1000);
        manager.addPipelineWorkgroup(loadProcessor, "DL", 1, 1000);

        try {
            manager.run();

            // dump counter statistics
            manager.dumpCounters(log);

            for( String counter: manager.getSession().getCounters() ) {
                int count = manager.getSession().getCounterValue(counter);
                if( count!=0 ) {
                    dbLogger.log(counter, Integer.toString(count), PipelineLogger.TOTAL);
                }
            }

            deleteObsoleteXdbIds(now, xdbIdCount);

            dbLogger.getPipelineLog().setSuccess("OK");
            dbLogger.close(true);

            log.info("--SUCCESS--");
        }
        catch(Exception e) {
            e.printStackTrace();
            dbLogger.getPipelineLog().setSuccess(e.getMessage());
            dbLogger.close(false);

            // rethrow the exception
            throw e;
        }
    }

    void deleteObsoleteXdbIds(java.util.Date now, int xdbIdCount) throws Exception {

        int xdbIdCountForDelete = dao.getCountOfXdbIdsModifiedBefore(PIPELINE_NAME, now);
        log.info("count of PharmGKB IDs to be deleted: "+xdbIdCountForDelete);

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
        log.info("   stale xdb ids delete threshold is "+deleteThreshold + " ("+getStaleIdsDeleteThreshold()+")");

        if( xdbIdCountForDelete > deleteThreshold ) {
            log.warn("***** count of PharmGKB IDs to be deleted is more than "+getStaleIdsDeleteThreshold()+" of PharmGKB ids -- REVIEW needed");
        }
        else if( xdbIdCountForDelete>0 ) {
            int count = dao.deleteXdbIdsModifiedBefore(PIPELINE_NAME, now);
            log.info("XDBS_DELETED_FROM_RGD: "+count);
            dbLogger.log("DELETED_FROM_RGD", Integer.toString(count), PipelineLogger.TOTAL);
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

    public LoadProcessor getLoadProcessor() {
        return loadProcessor;
    }

    public void setLoadProcessor(LoadProcessor loadProcessor) {
        this.loadProcessor = loadProcessor;
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
}
