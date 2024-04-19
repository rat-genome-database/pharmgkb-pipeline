package edu.mcw.rgd.pipelines.pharmgkb;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.process.CounterPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.List;

/**
 * @author mtutaj
 * @since Apr 28, 2011
 */
public class QCProcessor {

    private Dao dao;
    private CounterPool counters;
    private boolean useDataCache = false;

    private static final Logger log = LogManager.getLogger("incoming");

    public void process(PharmGKBRecord rec, CounterPool counters) throws Exception {

        this.counters = counters;

        runMatcher(rec);

        if( rec.getMatchingRgdId()>0 ) {
            checkIfAlreadyLoaded(rec);
        }

        // generate JSON record for this gene and write it and its QC flags into a file
        log.debug("\n"+formatAsJson(rec));

        if( rec.getXdbIdForUpdate()!=null ) {
            XdbId xdbId = rec.getXdbIdForUpdate();
            xdbId.setModificationDate(new java.util.Date());
            getDao().updateByKey(xdbId);
        }

        if( rec.getXdbIdForInsert()!=null ) {

            XdbId xdbId = rec.getXdbIdForInsert();
            getDao().insertXdb(xdbId);
        }

        counters.increment("INCOMING_ROWS_PROCESSED");
    }

    public String formatAsJson(PharmGKBRecord rec) throws Exception {
        // setup a JSON object array to collect all DafAnnotation objects
        ObjectMapper json = new ObjectMapper();
        // do not export fields with NULL values
        json.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // dump records to a file in JSON format
        ByteArrayOutputStream byteBuf = new ByteArrayOutputStream();
        OutputStreamWriter out = new OutputStreamWriter(byteBuf, "UTF8");
        BufferedWriter jsonWriter = new BufferedWriter(out);

        jsonWriter.write(json.writerWithDefaultPrettyPrinter().writeValueAsString(rec));

        jsonWriter.close();

        return byteBuf.toString();
    }

    private void runMatcher(PharmGKBRecord rec) throws Exception {

        //if( useDataCache ) {
        if( false ) { // note: caching of this kind of data slows down everything:
            // filling up the cache simply takes up too much time

            QCDataCache cache = QCDataCache.getInstance();
            rec.setGenesInRgdMatchingByHgncId( cache.getActiveGenesByHgncId(rec.getHgncId()) );
            rec.setGenesInRgdMatchingByGeneId( cache.getActiveGenesByNcbiGeneId(rec.getGeneId()));
            rec.setGenesInRgdMatchingByEnsemblId( cache.getActiveGenesByEnsemblGeneId(rec.getEnsemblId()));

        } else {
            rec.setGenesInRgdMatchingByHgncId(dao.getActiveGenesByXdbId(XdbId.XDB_KEY_HGNC, rec.getHgncId()));
            rec.setGenesInRgdMatchingByGeneId(dao.getActiveGenesByXdbId(XdbId.XDB_KEY_NCBI_GENE, rec.getGeneId()));
            rec.setGenesInRgdMatchingByEnsemblId(dao.getActiveGenesByXdbId(XdbId.XDB_KEY_ENSEMBL_GENES, rec.getEnsemblId()));
            rec.removeGeneDescriptions();
        }

        if( rec.getGenesInRgdMatchingByHgncId().size()==1 ) {
            rec.setMatchingRgdId(rec.getGenesInRgdMatchingByHgncId().get(0).getRgdId());
            counters.increment("MATCH_BY_HGNC_ID");
        }
        else if( rec.getGenesInRgdMatchingByGeneId().size()==1 ) {
            rec.setMatchingRgdId(rec.getGenesInRgdMatchingByGeneId().get(0).getRgdId());
            counters.increment("MATCH_BY_GENE_ID");
        }
        else if( rec.getGenesInRgdMatchingByEnsemblId().size()==1 ) {
            rec.setMatchingRgdId(rec.getGenesInRgdMatchingByEnsemblId().get(0).getRgdId());
            counters.increment("MATCH_BY_ENSEMBL_ID");
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
                counters.increment(multi);
            }
            else {
                // there are no matches at all
                counters.increment("MATCH_NOT_BY_HGNC_NCBIGENE_ENSEMBL");
            }

            // no more QC if multi matches or no matches at all
        }
    }

    private void checkIfAlreadyLoaded(PharmGKBRecord rec) throws Exception {

        // we have one matching rgd id: cross compare PharmGKB ids between RGD and incoming data
        List<XdbId> pharmGkbIdsInRgd = null;
        if( this.useDataCache ) {
            pharmGkbIdsInRgd = QCDataCache.getInstance().getPharmGKBId(rec.getMatchingRgdId());
        } else {
            pharmGkbIdsInRgd = dao.getXdbIdsByRgdId(XdbId.XDB_KEY_PHARMGKB, rec.getMatchingRgdId());
        }

        XdbId incomingPharmGkb = new XdbId();
        incomingPharmGkb.setAccId(rec.getPharmGkbAccId());
        incomingPharmGkb.setXdbKey(XdbId.XDB_KEY_PHARMGKB);
        incomingPharmGkb.setRgdId(rec.getMatchingRgdId());
        incomingPharmGkb.setSrcPipeline("PharmGKB");

        if( pharmGkbIdsInRgd.contains(incomingPharmGkb) ) {

            int index = pharmGkbIdsInRgd.indexOf(incomingPharmGkb);
            rec.setXdbIdForUpdate(pharmGkbIdsInRgd.get(index));

            counters.increment("XDBS_ALREADY_IN_RGD");
        }
        else {

            // setup the remaining fields
            java.util.Date dt = new java.util.Date();
            incomingPharmGkb.setCreationDate(dt);
            incomingPharmGkb.setModificationDate(dt);

            rec.setXdbIdForInsert(incomingPharmGkb);

            counters.increment("XDBS_INSERTED_INTO_RGD");
        }
    }
    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public void setUseDataCache( boolean useDataCache ) {
        this.useDataCache = useDataCache;
    }
}
