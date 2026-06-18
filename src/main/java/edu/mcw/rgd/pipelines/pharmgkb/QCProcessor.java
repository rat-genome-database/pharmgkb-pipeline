package edu.mcw.rgd.pipelines.pharmgkb;


import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.process.CounterPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

        ObjectMapper json = JsonMapper.builder()
                .changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();

        return json.writeValueAsString(rec);
    }

    private void runMatcher(PharmGKBRecord rec) throws Exception {

        rec.setGenesInRgdMatchingByHgncId(dao.getActiveGenesByXdbId(XdbId.XDB_KEY_HGNC, rec.getHgncId()));
        rec.setGenesInRgdMatchingByGeneId(dao.getActiveGenesByXdbId(XdbId.XDB_KEY_NCBI_GENE, rec.getGeneId()));
        rec.setGenesInRgdMatchingByEnsemblId(dao.getActiveGenesByXdbId(XdbId.XDB_KEY_ENSEMBL_GENES, rec.getEnsemblId()));
        rec.removeGeneDescriptions();

        if( rec.getGenesInRgdMatchingByHgncId().size()==1 ) {
            rec.setMatchingRgdId(rec.getGenesInRgdMatchingByHgncId().get(0).getRgdId());
            counters.increment("MATCH_TIER1_BY_HGNC_ID");
        }
        else if( rec.getGenesInRgdMatchingByGeneId().size()==1 ) {
            rec.setMatchingRgdId(rec.getGenesInRgdMatchingByGeneId().get(0).getRgdId());
            counters.increment("MATCH_TIER2_BY_GENE_ID");
        }
        else if( rec.getGenesInRgdMatchingByEnsemblId().size()==1 ) {
            rec.setMatchingRgdId(rec.getGenesInRgdMatchingByEnsemblId().get(0).getRgdId());
            counters.increment("MATCH_TIER3_BY_ENSEMBL_ID");
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
                counters.increment("MATCH_TIER4_NO_MATCH");
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

        int index = pharmGkbIdsInRgd.indexOf(incomingPharmGkb);
        if( index >= 0 ) {

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
