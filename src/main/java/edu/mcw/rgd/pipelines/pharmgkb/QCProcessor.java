package edu.mcw.rgd.pipelines.pharmgkb;

import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordProcessor;
import edu.mcw.rgd.process.PipelineLogFlagManager;
import edu.mcw.rgd.process.PipelineLogger;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: Apr 28, 2011
 * Time: 5:39:00 PM
 */
public class QCProcessor extends RecordProcessor {

    private Dao dao;
    PipelineLogger dbLogger = PipelineLogger.getInstance();
    PipelineLogFlagManager dbFlagManager;
    private int qcThreadCount;

    @Override
    public void process(PipelineRecord pipelineRecord) throws Exception {

        PharmGKBRecord rec = (PharmGKBRecord) pipelineRecord;

        runMatcher(rec);

        if( rec.getMatchingRgdId()>0 ) {
            checkIfAlreadyLoaded(rec);
        }

        // generate XML record for this gene and write it and its QC flags into database
        String xml = rec.toXml();
        dbLogger.addLogProp(null, null, rec.getRecNo(), PipelineLogger.REC_XML, xml);
        // write log props to database
        dbLogger.writeLogProps(rec.getRecNo());
        // remove the log props from log in memory
        dbLogger.removeAllLogProps(rec.getRecNo());

        dbFlagManager.writeFlags(rec.getRecNo());
    }

    private void runMatcher(PharmGKBRecord rec) throws Exception {

        rec.setGenesInRgdMatchingByHgncId( dao.getActiveGenesByXdbId(XdbId.XDB_KEY_HGNC, rec.getHgncId()) );
        rec.setGenesInRgdMatchingByGeneId( dao.getActiveGenesByXdbId(XdbId.XDB_KEY_NCBI_GENE, rec.getGeneId()) );
        rec.setGenesInRgdMatchingByEnsemblId(dao.getActiveGenesByXdbId(XdbId.XDB_KEY_ENSEMBL_GENES, rec.getEnsemblId()));

        if( rec.getGenesInRgdMatchingByHgncId().size()==1 ) {
            rec.setFlag("MATCH_BY_HGNC_ID");
            rec.setMatchingRgdId(rec.getGenesInRgdMatchingByHgncId().get(0).getRgdId());
            getSession().incrementCounter("MATCH_BY_HGNC_ID", 1);
            dbFlagManager.setFlag("MATCH_BY_HGNC_ID", rec.getRecNo());
        }
        else if( rec.getGenesInRgdMatchingByGeneId().size()==1 ) {
            rec.setFlag("MATCH_BY_GENE_ID");
            rec.setMatchingRgdId(rec.getGenesInRgdMatchingByGeneId().get(0).getRgdId());
            getSession().incrementCounter("MATCH_BY_GENE_ID", 1);
            dbFlagManager.setFlag("MATCH_BY_GENE_ID", rec.getRecNo());
        }
        else if( rec.getGenesInRgdMatchingByEnsemblId().size()==1 ) {
            rec.setFlag("MATCH_BY_ENSEMBL_ID");
            rec.setMatchingRgdId(rec.getGenesInRgdMatchingByEnsemblId().get(0).getRgdId());
            getSession().incrementCounter("MATCH_BY_ENSEMBL_ID", 1);
            dbFlagManager.setFlag("MATCH_BY_ENSEMBL_ID", rec.getRecNo());
        }
        else {
            // there is no single match by either HGNC, NCBI Gene, or Ensembl id
            // check if there are multiple matches
            String multi = "MULTIMATCH_BY";
            if( rec.getGenesInRgdMatchingByHgncId().size()>1 ) {
                multi += "_HGNC_ID";
            }
            if( rec.getGenesInRgdMatchingByGeneId().size()>1 ) {
                multi += "_GENE_ID";
            }
            if( rec.getGenesInRgdMatchingByEnsemblId().size()>1 ) {
                multi += "_ENSEMBL_ID";
            }

            if( !multi.equals("MULTIMATCH_BY") ) {
                // there are multiple matches
                rec.setFlag("MULTI_MATCH");
                getSession().incrementCounter(multi, 1);
                dbFlagManager.setFlag("MULTI_MATCH", rec.getRecNo());
            }
            else {
                // there are no matches at all
                rec.setFlag("NO_MATCH");
                getSession().incrementCounter("NO_MATCH_BY_HGNC_NCBIGENE_ENSEMBL", 1);
                dbFlagManager.setFlag("NO_MATCH", rec.getRecNo());
            }

            // no more QC if multi matches or no matches at all
        }
    }

    private void checkIfAlreadyLoaded(PharmGKBRecord rec) throws Exception {

        // we have one matching rgd id: cross compare PharmGKB ids between RGD and incoming data
        List<XdbId> pharmGkbIdsInRgd = dao.getXdbIdsByRgdId(XdbId.XDB_KEY_PHARMGKB, rec.getMatchingRgdId());

        XdbId incomingPharmGkb = new XdbId();
        incomingPharmGkb.setAccId(rec.getPharmGkbAccId());
        incomingPharmGkb.setXdbKey(XdbId.XDB_KEY_PHARMGKB);
        incomingPharmGkb.setRgdId(rec.getMatchingRgdId());
        incomingPharmGkb.setSrcPipeline("PharmGKB");

        if( pharmGkbIdsInRgd.contains(incomingPharmGkb) ) {

            int index = pharmGkbIdsInRgd.indexOf(incomingPharmGkb);
            rec.setXdbIdForUpdate(pharmGkbIdsInRgd.get(index));

            rec.setFlag("ALREADY_IN_RGD");
            getSession().incrementCounter("XDBS_ALREADY_IN_RGD", 1);
            dbFlagManager.setFlag("ALREADY_IN_RGD", rec.getRecNo());
        }
        else {

            // setup the remaining fields
            incomingPharmGkb.setLinkText(incomingPharmGkb.getAccId());
            incomingPharmGkb.setCreationDate(new java.util.Date());
            incomingPharmGkb.setModificationDate(new java.util.Date());

            rec.setXdbIdForInsert(incomingPharmGkb);

            rec.setFlag("INSERT_INTO_RGD");
            getSession().incrementCounter("XDBS_INSERTED_INTO_RGD", 1);
            dbFlagManager.setFlag("INSERT_INTO_RGD", rec.getRecNo());
        }
    }

    synchronized protected void registerDbLogFlags() throws Exception {
        dbFlagManager.registerFlag(
            "MATCH_BY_HGNC_ID",
            "incoming HGNC Id matches exactly one active gene in RGD"
            );
        dbFlagManager.registerFlag(
            "MATCH_BY_GENE_ID",
            "incoming GeneId matches exactly one active gene in RGD; there was no exact match by incoming HGNC Id"
            );
        dbFlagManager.registerFlag(
            "MATCH_BY_ENSEMBL_ID",
            "incoming EnsemblId matches exactly one active gene in RGD; there was no exact match by incoming HGNC Id and Gene Id"
            );

        dbFlagManager.registerFlag(
            "MULTI_MATCH",
            "CONFLICT: no exact match by incoming HGNC Id, Gene Id or Ensembl Id; but there is a match to multiple active genes in RGD"
            );

        dbFlagManager.registerFlag(
            "NO_MATCH",
            "no match by incoming HGNC Id, Gene Id, or Ensembl ID"
            );

        dbFlagManager.registerFlag(
            "ALREADY_IN_RGD",
            "PharmGKB id is already associated with the matching gene in RGD -- modification date will be updated"
            );

        dbFlagManager.registerFlag(
            "INSERT_INTO_RGD",
            "PharmGKB id is not associated with the matching gene in RGD -- it has to be inserted into RGD"
            );
    }

    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public PipelineLogFlagManager getDbFlagManager() {
        return dbFlagManager;
    }

    public void setDbFlagManager(PipelineLogFlagManager dbFlagManager) {
        this.dbFlagManager = dbFlagManager;
    }

    public void setQcThreadCount(int qcThreadCount) {
        this.qcThreadCount = qcThreadCount;
    }

    public int getQcThreadCount() {
        return qcThreadCount;
    }
}
