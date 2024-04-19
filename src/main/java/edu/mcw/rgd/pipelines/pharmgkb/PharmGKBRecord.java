package edu.mcw.rgd.pipelines.pharmgkb;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.XdbId;
import java.util.List;

/**
 * @author mtutaj
 * @since Apr 29, 2011
 * custom data, both read from incoming data and from rgd database
 */
public class PharmGKBRecord {

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
        this.hgncId = hgncId;
    }

    // remove gene descriptions to significantly reduce json footprint of this object
    public void removeGeneDescriptions() {
        removeGeneDescriptions(genesInRgdMatchingByHgncId);
        removeGeneDescriptions(genesInRgdMatchingByGeneId);
        removeGeneDescriptions(genesInRgdMatchingByEnsemblId);
    }

    public static void removeGeneDescriptions( List<Gene> list ) {
        if( list==null ) {
            return;
        }
        for( Gene g: list ) {
            g.setDescription(null);
            g.setAgrDescription(null);
            g.setMergedDescription(null);
            g.setNotes(null);
        }
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
}
