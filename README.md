# pharmgkb-pipeline
Imports external ids for human genes from PharmGKB
(https://api.pharmgkb.org/v1/download/file/data/genes.zip).

After downloading the file, every data line is compared against RGD genes.
To find the matching gene in RGD, a lookup is performed by:

1. HGNC id

2. If a single match, the gene is found. If not a single match, or multiple matches,
   match by NCBI Gene Id is performed

3. If a single match, the gene is found. If not a single match, or multiple matches,
   match by Ensembl Gene Id is performed
