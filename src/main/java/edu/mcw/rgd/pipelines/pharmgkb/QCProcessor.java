package edu.mcw.rgd.pipelines.pharmgkb;

import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordProcessor;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.List;

/**
 * @author mtutaj
 * @since Apr 28, 2011
 */
public class QCProcessor extends RecordProcessor {

    private Dao dao;
    private int qcThreadCount;

    private static final Logger log = Logger.getLogger("incoming");

    @Override
    public void process(PipelineRecord pipelineRecord) throws Exception {

        PharmGKBRecord rec = (PharmGKBRecord) pipelineRecord;

        runMatcher(rec);

        if( rec.getMatchingRgdId()>0 ) {
            checkIfAlreadyLoaded(rec);
        }

        // generate XML record for this gene and write it and its QC flags into a file
        String xml = rec.toXml();
        log.debug("\n"+formatXml(xml));
    }

    public String formatXml(String xml) throws Exception {

        final InputSource src = new InputSource(new StringReader(xml));
        final Node document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src).getDocumentElement();
        final Boolean keepDeclaration = Boolean.valueOf(xml.startsWith("<?xml"));

        //May need this: System.setProperty(DOMImplementationRegistry.PROPERTY,"com.sun.org.apache.xerces.internal.dom.DOMImplementationSourceImpl");


        final DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        final DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
        final LSSerializer writer = impl.createLSSerializer();

        writer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE); // Set this to true if the output needs to be beautified.
        writer.getDomConfig().setParameter("xml-declaration", keepDeclaration); // Set this to true if the declaration is needed to be outputted.

        return writer.writeToString(document);
    }

    private void runMatcher(PharmGKBRecord rec) throws Exception {

        rec.setGenesInRgdMatchingByHgncId( dao.getActiveGenesByXdbId(XdbId.XDB_KEY_HGNC, rec.getHgncId()) );
        rec.setGenesInRgdMatchingByGeneId( dao.getActiveGenesByXdbId(XdbId.XDB_KEY_NCBI_GENE, rec.getGeneId()) );
        rec.setGenesInRgdMatchingByEnsemblId(dao.getActiveGenesByXdbId(XdbId.XDB_KEY_ENSEMBL_GENES, rec.getEnsemblId()));

        if( rec.getGenesInRgdMatchingByHgncId().size()==1 ) {
            rec.setFlag("MATCH_BY_HGNC_ID");
            rec.setMatchingRgdId(rec.getGenesInRgdMatchingByHgncId().get(0).getRgdId());
            getSession().incrementCounter("MATCH_BY_HGNC_ID", 1);
        }
        else if( rec.getGenesInRgdMatchingByGeneId().size()==1 ) {
            rec.setFlag("MATCH_BY_GENE_ID");
            rec.setMatchingRgdId(rec.getGenesInRgdMatchingByGeneId().get(0).getRgdId());
            getSession().incrementCounter("MATCH_BY_GENE_ID", 1);
        }
        else if( rec.getGenesInRgdMatchingByEnsemblId().size()==1 ) {
            rec.setFlag("MATCH_BY_ENSEMBL_ID");
            rec.setMatchingRgdId(rec.getGenesInRgdMatchingByEnsemblId().get(0).getRgdId());
            getSession().incrementCounter("MATCH_BY_ENSEMBL_ID", 1);
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
            }
            else {
                // there are no matches at all
                rec.setFlag("NO_MATCH");
                getSession().incrementCounter("NO_MATCH_BY_HGNC_NCBIGENE_ENSEMBL", 1);
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
        }
        else {

            // setup the remaining fields
            incomingPharmGkb.setLinkText(incomingPharmGkb.getAccId());
            incomingPharmGkb.setCreationDate(new java.util.Date());
            incomingPharmGkb.setModificationDate(new java.util.Date());

            rec.setXdbIdForInsert(incomingPharmGkb);

            rec.setFlag("INSERT_INTO_RGD");
            getSession().incrementCounter("XDBS_INSERTED_INTO_RGD", 1);
        }
    }
    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public void setQcThreadCount(int qcThreadCount) {
        this.qcThreadCount = qcThreadCount;
    }

    public int getQcThreadCount() {
        return qcThreadCount;
    }
}
