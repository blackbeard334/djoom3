package neo.framework;

/**
 *
 */
@Deprecated
public class Unzip {

    static class unzFile {

    }

    /* tm_unz contain date/time info */
    class tm_unz {

        int tm_sec;            /* seconds after the minute - [0,59] */

        int tm_min;            /* minutes after the hour - [0,59] */

        int tm_hour;           /* hours since midnight - [0,23] */

        int tm_mday;           /* day of the month - [1,31] */

        int tm_mon;            /* months since January - [0,11] */

        int tm_year;           /* years - [1980..2044] */

    } /*tm_unz_s*/;

    /* unz_global_info structure contain global data about the ZIPfile
     These data comes from the end of central dir */
    class unz_global_info {

        long number_entry;         /* total number of entries in the central dir on this disk */

        long size_comment;         /* size of the global comment of the zipfile */

    } /*unz_global_info_s*/;

    /* unz_file_info contain information about a file in the zipfile */
    class unz_file_info {

        long version;              /* version made by                 2 unsigned chars */

        long version_needed;       /* version needed to extract       2 unsigned chars */

        long flag;                 /* general purpose bit flag        2 unsigned chars */

        long compression_method;   /* compression method              2 unsigned chars */

        long dosDate;              /* last mod file date in Dos fmt   4 unsigned chars */

        long crc;                  /* crc-32                          4 unsigned chars */

        long compressed_size;      /* compressed size                 4 unsigned chars */

        long uncompressed_size;    /* uncompressed size               4 unsigned chars */

        long size_filename;        /* filename length                 2 unsigned chars */

        long size_file_extra;      /* extra field length              2 unsigned chars */

        long size_file_comment;    /* file comment length             2 unsigned chars */
//

        long disk_num_start;       /* disk number start               2 unsigned chars */

        long internal_fa;          /* internal file attributes        2 unsigned chars */

        long external_fa;          /* external file attributes        4 unsigned chars */

        tm_unz tmu_date;
    } /*unz_file_info_s*/;

    class unz_file_info_internal {

        long offset_curfile;/* relative offset of static header 4 unsigned chars */

    } /*unz_file_info_internal_s*/;
}
