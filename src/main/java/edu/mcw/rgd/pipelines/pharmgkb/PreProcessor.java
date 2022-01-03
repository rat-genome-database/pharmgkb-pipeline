package edu.mcw.rgd.pipelines.pharmgkb;

import edu.mcw.rgd.process.FileDownloader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author mtutaj
 * @since Apr 28, 2011
 *
 * download genes file from PharmGKB website, process it and break into lines
 */
public class PreProcessor {

    private final Logger log = LogManager.getLogger("status");
    private String genesFile;
    private String headerLine;

    /** genes.tsv format:<pre>
     * As of May 2018, columns {"Chromosomal Start" "Chromosomal Stop"} have been replaced by columns
     * {"Chromosomal Start - GRCh37.p13" "Chromosomal Stop - GRCh37.p13" "Chromosomal Start - GRCh38.p7" "Chromosomal Stop - GRCh38.p7"}
     * <b>After February 2016:</b>
     * PharmGKB Accession Id	NCBI Gene ID    HGNC ID Ensembl Id	Name	Symbol	Alternate Names	Alternate Symbols	Is VIP	Has Variant Annotation	Cross-references	Has CPIC Dosing Guideline	Chromosome	Chromosomal Start	Chromosomal Stop
     * </pre>
     * As of December 2012 there is 11th column at the end of the file named "Has CPIC Dosing Guideline"<br>
     * As of October 2013 there are new columns at the end of the file: "Chromosome" "Chromosomal Start" "Chromosomal Stop"<br>
     * As of March 2016 there is a new column 'HGNC ID' inserted between 'NCBI Gene ID' and 'Ensembl Id'
     * @throws Exception
     */
    public List<PharmGKBRecord> process() throws Exception {

        List<PharmGKBRecord> result = new ArrayList<>();

        // download the file to a local folder
        log.info("  downloading file "+genesFile);
        String fileName = downloadFile();

        // unzip the file: process only files ending with '*.tsv' -- 'genes.tsv'
        try (ZipFile zipFile = new ZipFile(fileName)){
            Enumeration enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
                log.debug("  unzipping: " + zipEntry.getName());
                if (!zipEntry.getName().endsWith("tsv")) {
                    log.debug("  skipping file: " + zipEntry.getName());
                    continue;
                }

                result.addAll(parseFile(zipFile.getInputStream(zipEntry)));
            }
        }

        return result;
    }

    private List<PharmGKBRecord> parseFile(InputStream ios) throws Exception {

        // this is a text file, tab separated
        List<PharmGKBRecord> result = new ArrayList<>();

        // validate the first line: header line
        BufferedReader reader = new BufferedReader(new InputStreamReader(ios));
        String line = reader.readLine();
        validateHeaderLine(line);

        // header line ok: normal processing
        while( (line=reader.readLine())!=null ) {
            // skip comment lines
            if( line.startsWith("#") )
                continue;
            // split line into words
            String[] words = line.split("\t", -1);
            if( words.length<11 ) {
                continue; // there must be at least 11 columns present
            }

            // parse the data
            PharmGKBRecord rec = new PharmGKBRecord();
            rec.setPharmGkbAccId(words[0]);
            rec.setGeneId(words[1]);
            rec.setHgncId(words[2]);
            rec.setEnsemblId(words[3]);
            rec.setGeneName(words[4]);
            rec.setGeneSymbol(words[5]);
            rec.setAltGeneNames(words[6]);
            rec.setAltGeneSymbols(words[7]);
            rec.setVIP(words[8]);
            rec.setHasVariantAnnotation(words[9]);
            rec.setCrossReferences(words[10]);

            result.add(rec);
        }

        // cleanup
        reader.close();

        return result;
    }

    /**
     * download genes.zip file, save it to a local directory
     * @return the name of the local copy of the file
     */
    private String downloadFile() throws Exception {

        FileDownloader downloader = new FileDownloader();
        downloader.setExternalFile(getGenesFile());
        downloader.setLocalFile("data/genes.zip");
        downloader.setPrependDateStamp(true); // prefix downloaded files with the current date

        // starting Jan 2017, HttpClient from apache commons had problems downloading files via HTTPS;
        // therefore we switched to use native java URL object to download the file from PharmGKB
        // it worked!
        downloader.setDoNotUseHttpClient(true);

        return downloader.downloadNew();
    }

    void validateHeaderLine(String line) throws Exception {

        // detect any changes in the header line
        String headerLine = line.replace('\t','|');
        if( headerLine.equals(getHeaderLine()) ) {
            // header line as expected
            return;
        }

        // changes in header line: different nr of columns?
        String[] colsOld = getHeaderLine().split("[\\|]");
        String[] colsNew = headerLine.split("[\\|]");
        if( colsOld.length != colsNew.length ) {
            String msg = "ERROR: Change in header line detected: different number of columns!";
            log.error(msg);
            throw new Exception(msg);
        }

        // same nr of columns: which column changed?
        for( int col=0; col<colsOld.length; col++ ) {
            if( colsOld[col].equals(colsNew[col]) ) {
                String msg = "ERROR: Change in header line detected: different column "+(col+1)
                        +"; old=["+colsOld[col]+"], new=["+colsNew[col]+"]";
                log.error(msg);
                throw new Exception(msg);
            }
        }
    }

    public String getGenesFile() {
        return genesFile;
    }

    public void setGenesFile(String genesFile) {
        this.genesFile = genesFile;
    }

    public void setHeaderLine(String headerLine) {
        this.headerLine = headerLine;
    }

    public String getHeaderLine() {
        return headerLine;
    }
}
