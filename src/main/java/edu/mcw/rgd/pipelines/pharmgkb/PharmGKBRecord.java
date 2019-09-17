package edu.mcw.rgd.pipelines.pharmgkb;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.pipelines.PipelineRecord;
import java.util.List;
import nu.xom.*;

/**
 * @author mtutaj
 * @since Apr 29, 2011
 * custom data, both read from incoming data and from rgd database
 */
public class PharmGKBRecord extends PipelineRecord {

    private static int _recno = 0;

    // incoming data
    private String pharmGkbAccId;
    private String hgncId;
    private String geneId;
    private String ensemblId;
    private String geneName;
    private String geneSymbol;
    private String altGeneNames;
    private String altGeneSymbols;
    private String isVIP;
    private String hasVariantAnnotation;
    private String crossReferences;

    // matching active genes
    private List<Gene> genesInRgdMatchingByHgncId;
    private List<Gene> genesInRgdMatchingByGeneId;
    private List<Gene> genesInRgdMatchingByEnsemblId;

    private int matchingRgdId; // matching rgd id of an active gene

    // LOADING
    private XdbId xdbIdForUpdate; // MODIFICATION_DATE is to be updated
    private XdbId xdbIdForInsert; // to be inserted into RGD

    public PharmGKBRecord() {
        setRecNo(++_recno);
    }

    public String getPharmGkbAccId() {
        return pharmGkbAccId;
    }

    public void setPharmGkbAccId(String pharmGkbAccId) {
        this.pharmGkbAccId = pharmGkbAccId;
    }

    public String getHgncId() {
        return hgncId;
    }

    public void setHgncId(String hgncId) {
        if( !hgncId.isEmpty() )
            this.hgncId = "HGNC:"+hgncId;
        else
            this.hgncId = hgncId;
    }

    public String getGeneId() {
        return geneId;
    }

    public void setGeneId(String geneId) {
        this.geneId = geneId;
    }

    public String getEnsemblId() {
        return ensemblId;
    }

    public void setEnsemblId(String ensemblId) {
        this.ensemblId = ensemblId;
    }

    public String getGeneName() {
        return geneName;
    }

    public void setGeneName(String geneName) {
        this.geneName = geneName;
    }

    public String getGeneSymbol() {
        return geneSymbol;
    }

    public void setGeneSymbol(String geneSymbol) {
        this.geneSymbol = geneSymbol;
    }

    public String getAltGeneNames() {
        return altGeneNames;
    }

    public void setAltGeneNames(String altGeneNames) {
        this.altGeneNames = altGeneNames;
    }

    public String getAltGeneSymbols() {
        return altGeneSymbols;
    }

    public void setAltGeneSymbols(String altGeneSymbols) {
        this.altGeneSymbols = altGeneSymbols;
    }

    public String getVIP() {
        return isVIP;
    }

    public void setVIP(String VIP) {
        isVIP = VIP;
    }

    public String getHasVariantAnnotation() {
        return hasVariantAnnotation;
    }

    public void setHasVariantAnnotation(String hasVariantAnnotation) {
        this.hasVariantAnnotation = hasVariantAnnotation;
    }

    public String getCrossReferences() {
        return crossReferences;
    }

    public void setCrossReferences(String crossReferences) {
        this.crossReferences = crossReferences;
    }

    public List<Gene> getGenesInRgdMatchingByGeneId() {
        return genesInRgdMatchingByGeneId;
    }

    public void setGenesInRgdMatchingByGeneId(List<Gene> genesInRgdMatchingByGeneId) {
        this.genesInRgdMatchingByGeneId = genesInRgdMatchingByGeneId;
    }

    public List<Gene> getGenesInRgdMatchingByEnsemblId() {
        return genesInRgdMatchingByEnsemblId;
    }

    public void setGenesInRgdMatchingByEnsemblId(List<Gene> genesInRgdMatchingByEnsemblId) {
        this.genesInRgdMatchingByEnsemblId = genesInRgdMatchingByEnsemblId;
    }

    public List<Gene> getGenesInRgdMatchingByHgncId() {
        return genesInRgdMatchingByHgncId;
    }

    public void setGenesInRgdMatchingByHgncId(List<Gene> genesInRgdMatchingByHgncId) {
        this.genesInRgdMatchingByHgncId = genesInRgdMatchingByHgncId;
    }

    public int getMatchingRgdId() {
        return matchingRgdId;
    }

    public void setMatchingRgdId(int matchingRgdId) {
        this.matchingRgdId = matchingRgdId;
    }

    public XdbId getXdbIdForUpdate() {
        return xdbIdForUpdate;
    }

    public void setXdbIdForUpdate(XdbId xdbIdForUpdate) {
        this.xdbIdForUpdate = xdbIdForUpdate;
    }

    public XdbId getXdbIdForInsert() {
        return xdbIdForInsert;
    }

    public void setXdbIdForInsert(XdbId xdbIdForInsert) {
        this.xdbIdForInsert = xdbIdForInsert;
    }

    public String toXml() {

        Element root = new Element("rec");

        // incoming data
        Element pharmGKB = new Element("PharmGKB");
        root.appendChild(pharmGKB);

        pharmGKB.addAttribute(new Attribute("recno", Integer.toString(this.getRecNo())));

        Element el = new Element("accId");
        el.appendChild(this.getPharmGkbAccId());
        pharmGKB.appendChild(el);

        el = new Element("hgncId");
        el.appendChild(this.getHgncId());
        pharmGKB.appendChild(el);

        el = new Element("geneId");
        el.appendChild(this.getGeneId());
        pharmGKB.appendChild(el);

        el = new Element("ensemblId");
        el.appendChild(this.getEnsemblId());
        pharmGKB.appendChild(el);

        el = new Element("crossReferences");
        el.appendChild(this.getCrossReferences());
        pharmGKB.appendChild(el);

        el = new Element("geneName");
        el.appendChild(this.getGeneName());
        pharmGKB.appendChild(el);

        el = new Element("geneSymbol");
        el.appendChild(this.getGeneSymbol());
        pharmGKB.appendChild(el);

        el = new Element("altGeneNames");
        el.appendChild(this.getAltGeneNames());
        pharmGKB.appendChild(el);

        el = new Element("altGeneSymbols");
        el.appendChild(this.getAltGeneSymbols());
        pharmGKB.appendChild(el);

        el = new Element("props");
        el.addAttribute(new Attribute("isVIP", this.getVIP()));
        el.addAttribute(new Attribute("hasVariantAnnotation", this.getHasVariantAnnotation()));
        pharmGKB.appendChild(el);

        // rgd
        Element rgd = new Element("rgd");
        root.appendChild(rgd);

        el = new Element("matchingRgdId");
        el.appendChild(Integer.toString(this.matchingRgdId));
        rgd.appendChild(el);

        // matching active genes
        appendGeneList(rgd, genesInRgdMatchingByHgncId, "MatchingGenesInRgdHgncIds");
        appendGeneList(rgd, genesInRgdMatchingByGeneId, "MatchingGenesInRgdByGeneIds");
        appendGeneList(rgd, genesInRgdMatchingByEnsemblId, "MatchingGenesInRgdEnsemblIds");

        el = new Element("loadingAction");
        rgd.appendChild(el);
        if( xdbIdForUpdate!=null ) {
            Element el2 = new Element("update");
            el2.addAttribute(new Attribute("acc_id", xdbIdForUpdate.getAccId()));
            el2.addAttribute(new Attribute("creation_date", xdbIdForUpdate.getCreationDate().toString()));
            el.appendChild(el2);
        }
        if( xdbIdForInsert!=null ) {
            Element el2 = new Element("insert");
            el2.addAttribute(new Attribute("acc_id", xdbIdForInsert.getAccId()));
            el.appendChild(el2);
        }

        return root.toXML();
    }

    public void appendGeneList(Element el, List<Gene> geneList, String elName) {

        Element el2 = new Element(elName);
        el.appendChild(el2);

        for( Gene gene: geneList ) {
            Element el3 = new Element("gene");
            el3.addAttribute(new Attribute("rgdId", Integer.toString(gene.getRgdId())));
            el3.addAttribute(new Attribute("symbol", gene.getSymbol()));
            el2.appendChild(el3);
        }
    }
}
