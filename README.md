# pharmgkb-pipeline

Imports PharmGKB accession IDs for human genes into RGD.

## Data Source

Downloads `genes.zip` from the PharmGKB API (`https://api.pharmgkb.org/v1/download/file/data/genes.zip`),
extracts the `genes.tsv` file, and parses each row into a `PharmGKBRecord`.

The TSV file header is validated on every run; the pipeline will abort if columns change unexpectedly.

## Pipeline Logic

### 1. Download & Parse (PreProcessor)
- Downloads `genes.zip` to a local `data/` directory (date-stamped).
- Extracts and parses `genes.tsv` (tab-delimited, at least 11 columns required).
- Normalizes HGNC IDs by ensuring the `HGNC:` prefix is present.

### 2. Gene Matching (QCProcessor)
Each incoming record is matched to an active RGD gene using a cascading lookup:

1. **HGNC ID** — if exactly one active gene matches, use it.
2. **NCBI Gene ID** — tried only if HGNC match fails or is ambiguous.
3. **Ensembl Gene ID** — tried only if NCBI Gene ID match also fails.

If multiple genes match at any level, the record is flagged as a multi-match.
If no gene matches at any level, the record is counted as unmatched.

### 3. Load / Update (QCProcessor)
For each successfully matched gene:
- If the PharmGKB accession ID already exists in RGD for that gene, update its modification date.
- If it is new, insert it as a new XDB ID (source pipeline = `PharmGKB`).

### 4. Stale ID Cleanup (Manager)
After processing, IDs whose modification date was not refreshed (older than 1 hour)
are candidates for deletion. A safety threshold (default **5%**) prevents mass deletion:
if the number of stale IDs exceeds the threshold, deletion is skipped and a warning is logged.

## Processing Modes

Configured via Spring XML (`properties/AppConfigure.xml`):

| Property | Default | Description |
|---|---|---|
| `multithreadQC` | `true` | Process records with parallel streams |
| `cacheQCData` | `false` | Pre-load gene/XDB data into memory (currently disabled — cache fill is too slow) |
| `staleIdsDeleteThreshold` | `5%` | Max percentage of IDs that can be deleted in one run |

## Logging

Uses Log4j2 with four named loggers:

| Logger | File | Content |
|---|---|---|
| `status` | `logs/status.log` | Main pipeline progress and summary |
| `inserted` | `logs/inserted.log` | Pipe-delimited dump of every inserted XDB ID |
| `deleted` | `logs/deleted.log` | Pipe-delimited dump of every deleted XDB ID |
| `incoming` | `logs/incoming.log` | JSON representation of each processed record |

Memory usage statistics are logged at the end of each run via `MemoryMonitor`.

## Build & Run

Requires **Java 17**. Built with Gradle:

```bash
./gradlew clean assembleDist
```

The distribution zip is created under `build/distributions/`. Run via:

```bash
java -jar pharmgkb-pipeline.jar
```

Spring configuration and Log4j2 config must be in a `properties/` directory relative to the working directory.
