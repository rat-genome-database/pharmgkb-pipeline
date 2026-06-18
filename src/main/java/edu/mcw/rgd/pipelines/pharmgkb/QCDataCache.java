package edu.mcw.rgd.pipelines.pharmgkb;

import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class QCDataCache {

    public static QCDataCache instance;

    private final Logger log = LogManager.getLogger("status");

    private QCDataCache() {}

    public static QCDataCache getInstance() {

        if( instance == null ) {
            synchronized( QCDataCache.class ) {
                if( instance == null ) {
                    instance = new QCDataCache();
                }
            }
        }
        return instance;
    }

    Map<Integer, List<XdbId>> pharmGKBIdsInRgd = new HashMap<>();

    public void loadCache() throws Exception {

        log.info("  preloading data cache ...");

        Dao dao = new Dao();

        List<XdbId> xdbIds = dao.getXdbIds(XdbId.XDB_KEY_PHARMGKB, SpeciesType.HUMAN, RgdId.OBJECT_KEY_GENES);
        for( XdbId id: xdbIds ) {

            List<XdbId> ids = pharmGKBIdsInRgd.get(id.getRgdId());
            if( ids==null ) {
                ids = new ArrayList<>();
                pharmGKBIdsInRgd.put(id.getRgdId(), ids);
            }
            ids.add(id);
        }
        log.info("    PharmGKB ids loaded = "+pharmGKBIdsInRgd.size());

        log.info("  data cache loaded");
    }

    public List<XdbId> getPharmGKBId( int rgdId ) {
        List<XdbId> xdbIds = pharmGKBIdsInRgd.get(rgdId);
        return xdbIds!=null ? xdbIds : Collections.emptyList();
    }
}
