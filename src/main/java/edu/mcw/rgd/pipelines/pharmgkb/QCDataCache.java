package edu.mcw.rgd.pipelines.pharmgkb;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class QCDataCache {

    public static QCDataCache instance;

    private final Logger log = LogManager.getLogger("status");

    private boolean cacheOnlyPharmGKBIds = true;

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

    Map<Integer, Gene> _geneMap = new HashMap<>();
    Map<String, List<Gene>> _hgncMap = new HashMap<>();
    Map<String, List<Gene>> _ncbiMap = new HashMap<>();
    Map<String, List<Gene>> _ensemblMap = new HashMap<>();

    Map<Integer, List<XdbId>> pharmGKBIdsInRgd = new HashMap<>();

    public void loadCache() throws Exception {

        log.info("  preloading data cache ...");

        Dao dao = new Dao();

        if( !cacheOnlyPharmGKBIds ) {

            List<Gene> genes = dao.getActiveGenes(SpeciesType.HUMAN);
            PharmGKBRecord.removeGeneDescriptions(genes);

            for( Gene g: genes ) {
                _geneMap.put(g.getRgdId(), g);
            }
            log.info("    genes loaded="+_geneMap.size());

            List<XdbId> xdbIds = dao.getXdbIds(XdbId.XDB_KEY_HGNC, SpeciesType.HUMAN, RgdId.OBJECT_KEY_GENES);
            load(_hgncMap, xdbIds);
            log.info("    hgnc map loaded="+_hgncMap.size());

            xdbIds = dao.getXdbIds(XdbId.XDB_KEY_ENTREZGENE, SpeciesType.HUMAN, RgdId.OBJECT_KEY_GENES);
            load(_ncbiMap, xdbIds);
            log.info("    ncbi map loaded="+_ncbiMap.size());

            xdbIds = dao.getXdbIds(XdbId.XDB_KEY_ENSEMBL_GENES, SpeciesType.HUMAN, RgdId.OBJECT_KEY_GENES);
            load(_ensemblMap, xdbIds);
            log.info("    ensembl map loaded="+_ensemblMap.size());

        }

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

    void load( Map<String, List<Gene>> targetMap, List<XdbId> xdbIds ) {

        for( XdbId id: xdbIds ) {

            Gene gene = _geneMap.get(id.getRgdId());
            if( gene == null ) {
                continue;
            }

            List<Gene> genes = targetMap.get(id.getAccId());
            if( genes==null ) {
                genes = new ArrayList<>();
                targetMap.put(id.getAccId(), genes);
            }
            genes.add( gene );
        }
    }

    public List<Gene> getActiveGenesByHgncId( String hgncId ) {
        List<Gene> genes = _hgncMap.get(hgncId);
        return genes!=null ? genes : Collections.emptyList();
    }

    public List<Gene> getActiveGenesByNcbiGeneId( String ncbiId ) {
        List<Gene> genes = _ncbiMap.get(ncbiId);
        return genes!=null ? genes : Collections.emptyList();
    }

    public List<Gene> getActiveGenesByEnsemblGeneId( String ensemblId ) {
        List<Gene> genes = _ensemblMap.get(ensemblId);
        return genes!=null ? genes : Collections.emptyList();
    }

    public List<XdbId> getPharmGKBId( int rgdId ) {
        List<XdbId> xdbIds = pharmGKBIdsInRgd.get(rgdId);
        return xdbIds!=null ? xdbIds : Collections.emptyList();
    }
}
