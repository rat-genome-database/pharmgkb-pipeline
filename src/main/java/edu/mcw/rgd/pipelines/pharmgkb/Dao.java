package edu.mcw.rgd.pipelines.pharmgkb;

import edu.mcw.rgd.dao.impl.XdbIdDAO;
import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.XdbId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * @author mtutaj
 * @since 12/22/11
 * <p>
 * wrapper class: have all DAO code in one place
 */
public class Dao {

    private final Logger logInserted = LogManager.getLogger("inserted");
    private final Logger logDeleted = LogManager.getLogger("deleted");

    private XdbIdDAO xdbIdDAO = new XdbIdDAO();

    public String getConnectionInfo() {
        return xdbIdDAO.getConnectionInfo();
    }

    /**
     * get active genes with given external id
     * @param xdbKey - external db key
     * @param accId - external id to be looked for
     * @return list of Gene objects
     * @throws Exception when unexpected error in spring framework occurs
     */
    public List<Gene> getActiveGenesByXdbId(int xdbKey, String accId) throws Exception {
        if( accId==null )
            return Collections.emptyList();
        return xdbIdDAO.getActiveGenesByXdbId(xdbKey, accId);
    }

    /**
     * return external ids for given xdb key and rgd-id
     * @param xdbKey - external database key (like 3 for NCBIGene)
     * @param rgdId - rgd-id
     * @return list of external ids
     * @throws Exception when unexpected error in spring framework occurs
     */
    public List<XdbId> getXdbIdsByRgdId(int xdbKey, int rgdId) throws Exception {
        return xdbIdDAO.getXdbIdsByRgdId(xdbKey, rgdId);
    }

    /**
     * update properties of this row by ACC_XDB_KEY
     * @param xdbId object with data to be updated
     * @return 1 if row have been changed, or 0 if left unchanged
     * @throws Exception when something went wrong in spring framework
     */
    public int updateByKey(XdbId xdbId) throws Exception {
        return xdbIdDAO.updateByKey(xdbId);
    }

    /**
     * insert an XdbId; duplicate entries are not inserted (with same RGD_ID,XDB_KEY,ACC_ID,SRC_PIPELINE)
     * @param xdb XdbId object to be inserted
     * @return number of actually inserted rows (0 or 1)
     * @throws Exception when unexpected error in spring framework occurs
     */
    public int insertXdb(XdbId xdb) throws Exception {
        logInserted.info(xdb.dump("|"));
        return xdbIdDAO.insertXdb(xdb);
    }

    /**
     * get count of xdb ids for the given pipeline modified before given date
     *
     * @param srcPipeline source pipeline
     * @param modDate modification date
     * @return count of xdb ids for the given pipeline modified before given date
     * @throws Exception when unexpected error in spring framework occurs
     */
    public int getCountOfXdbIdsModifiedBefore(String srcPipeline, java.util.Date modDate) throws Exception {

        return xdbIdDAO.getCountOfXdbIdsModifiedBefore(XdbId.XDB_KEY_PHARMGKB, srcPipeline, modDate);
    }

    /**
     * delete entries for PharmGKB and pipeline modified before given date
     *
     * @param srcPipeline source
     * @param modDate modification date
     * @return count of rows deleted
     * @throws Exception when unexpected error in spring framework occurs
     */
    public int deleteXdbIdsModifiedBefore(String srcPipeline, java.util.Date modDate) throws Exception {

        List<XdbId> xdbIds = xdbIdDAO.getXdbIdsModifiedBefore(XdbId.XDB_KEY_PHARMGKB, srcPipeline, modDate);
        for( XdbId xdbId: xdbIds ) {
            logDeleted.info(xdbId.dump("|"));
        }
        xdbIdDAO.deleteXdbIds(xdbIds);
        return xdbIds.size();
    }
}
