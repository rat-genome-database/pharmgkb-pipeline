package edu.mcw.rgd.pipelines.pharmgkb;


import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordProcessor;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: Apr 28, 2011
 * Time: 5:39:31 PM
 */
public class LoadProcessor extends RecordProcessor {

    private Dao dao;

    @Override
    public void process(PipelineRecord pipelineRecord) throws Exception {
        
        PharmGKBRecord rec = (PharmGKBRecord) pipelineRecord;

        getSession().incrementCounter("PROCESSED", 1);

        if( rec.getXdbIdForUpdate()!=null ) {
            XdbId xdbId = rec.getXdbIdForUpdate();
            xdbId.setModificationDate(new java.util.Date());
            getDao().updateByKey(xdbId);
        }

        if( rec.getXdbIdForInsert()!=null ) {

            XdbId xdbId = rec.getXdbIdForInsert();
            getDao().insertXdb(xdbId);
        }
    }

    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }
}
