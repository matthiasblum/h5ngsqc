# h5ngsqc
h5ngsqc is a Java program that allows to store NGS data in HDF5 files for being used on the QC Genomics platform.
Stored data are fixed-size windows or *bins*:

- wiggles: number of reads overlapping a 50bp (default) bin
- localQCs: 500bp bins retained by [NGS-QC Generator](http://ngs-qc.org/) because they present a robust signal
 
## Usage

    java -jar h5ngsqc.jar BED TABLE CHROMSIZES OUTPUT [options]

**BED** is an alignment file in the [BED format](https://genome.ucsc.edu/FAQ/FAQformat#format1). Each line corresponds to a mapped read. The file can be gzip-compressed.

**TABLE** is a file generated by NGS-QC Generator. Can be gzip-compressed.

**CHROMSIZES** is an UCSC-like chromosome sizes file. Each line contains the name and the size of a chromosome.

**OUTPUT** is the output HDF5 file. If it already exists, it will be overwritten.

### Options

| Option      | Description           | Default  |
| ----------- |-------------| -----|
| -s, --span INT  | resolution in bp of wiggle bins | 50 |
| -e, --ext INT  | read extension in bp for wiggle bins | 150 |
| --bg INT       | background threshold for localQCs | 0 |
| -5          | enable 5-replicates localQCs | false |
| --skip      | do not stop the program if a localQC or a read is on an unknown chromosome | false |
| --quiet     | do not display progress messages | false  |


## A word on PCR duplicates
 
 While computing wiggles, two values are associated at each bin: the intensity, and the intensity without PCR duplicates.
 To identify PCR duplicates, the BED file has to be sorted as follows:
 
    sort -k1,1V -k2,2n profile.bed > profile.sort.bed
    
## Todo

- [x] gzip-compressed files support
- [ ] read BED from stdin
