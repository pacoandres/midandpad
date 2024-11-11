package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.io.InputStream
import java.io.OutputStream
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.security.MessageDigest;
import java.util.zip.ZipInputStream

class BackupDatabase {
    companion object {
        private const val BUFFSIZE = 1024
        private const val DIGESTFILE = "midandpad.md5"
        private const val MD5LEN = 32
        private const val TMPDIR = "tmp"
        @OptIn(ExperimentalStdlibApi::class)
        private fun CreateTmpZip (context: Context, appdir: String, bkpname: String): File?{
            var origin: BufferedInputStream?  = null;
            var databasesdir: File? = null
            var zipfile: File? = null
            try {
                for (f in File(appdir).listFiles()!!) {
                    if (f.name.contains("databases")) {
                        databasesdir = f
                        break;
                    }
                }
                if (databasesdir == null) {
                    showErrorDialog(context, context.getString(R.string.sbackuperror),
                        context.getString(R.string.nodatabasedir))
                    return null
                }

                //val zipfile = kotlin.io.path.createTempFile(bkpname, ".zip").toFile()
                zipfile = File.createTempFile(bkpname, ".zip")
                val out: ZipOutputStream = ZipOutputStream(
                    BufferedOutputStream(
                        FileOutputStream(zipfile)
                    )
                )
                val data: ByteArray = ByteArray(BUFFSIZE)
                val md = MessageDigest.getInstance("MD5")
                for (file in databasesdir!!.listFiles()!!) {
                    Log.v("Compress", "Adding: ${file.name}")
                    val fi: FileInputStream = FileInputStream(file)
                    origin = BufferedInputStream(fi, BUFFSIZE)

                    val entry: ZipEntry = ZipEntry(file.name)
                    out.putNextEntry(entry);
                    var count: Int = 0

                    while (true) {
                        count = origin.read(data, 0, BUFFSIZE)
                        if (count == -1)
                            break
                        out.write(data, 0, count)
                        md.update(data, 0, count)
                    }
                    origin.close();
                }
                val entry: ZipEntry = ZipEntry(DIGESTFILE)
                out.putNextEntry(entry);
                out.write(md.digest().toHexString().toByteArray())
                out.close();
                return zipfile
            }
            catch (e: Exception){
                showErrorDialog(context, context.getString(R.string.sbackuperror),
                    context.getString(R.string.serrorziping), e)
            }
            if (zipfile?.exists() == true){
                zipfile.delete()
            }
            return null
        }
        fun BackupToDir (context: MainActivity, dir: DocumentFile, appdir: String){
            var tmpzip: File? = null
            var zipfile: DocumentFile? = null
            var orig: FileInputStream? = null
            var dest: OutputStream? = null
            try {

                val bkpname: String = ConfigParams.MODULE + "_" +
                        SimpleDateFormat ("yyyyMMdd'T'HHmmss").format(Date ())




                tmpzip = CreateTmpZip(context, appdir, bkpname)
                if (tmpzip == null){
                    return
                }

                zipfile = dir.createFile("application/zip", bkpname)
                if (zipfile == null){
                    tmpzip.delete()
                    showErrorDialog(context, context.getString(R.string.sbackuperror),
                        "Can't write backup destination")
                    return
                }

                dest = context.contentResolver.openOutputStream(zipfile.uri)
                if (dest == null){
                    showErrorDialog(context, context.getString(R.string.sbackuperror),
                        "Can't write backup destination")
                    tmpzip.delete()
                    zipfile.delete()
                    return
                }
                orig = FileInputStream (tmpzip)
                if (orig == null){
                    showErrorDialog(context, context.getString(R.string.sbackuperror),
                        "Can't read compressed backup")
                    dest.close()
                    tmpzip.delete()
                    zipfile.delete()
                    return
                }
                val data: ByteArray = ByteArray(BUFFSIZE)
                var count = 0
                while (true){
                    count = orig.read(data, 0, BUFFSIZE)
                    if (count == -1)
                        break
                    dest.write(data, 0, count)
                }
                dest.close()
                orig.close()
                tmpzip.delete()
            } catch (e: Exception) {
                dest?.close()
                orig?.close()
                tmpzip?.delete()
                zipfile?.delete()
                showErrorDialog(context, context.getString(R.string.sbackuperror),
                    "Can't crate backup", e)
            }
        }

        private fun DeleteTmp (tempdir: File){
            val tmpdir = File(tempdir, TMPDIR)
            if (tmpdir.exists())
                tmpdir.deleteRecursively()
        }
        @OptIn(ExperimentalStdlibApi::class)
        private fun CreateTmpDir (context: MainActivity, zipfile: DocumentFile, tempdir: File):
                File? {
            var orig: InputStream? = null
            var zip: ZipInputStream? = null
            var digest: ByteArray = ByteArray (MD5LEN)
            try {
                DeleteTmp(tempdir)
                val tmpdir = File(tempdir, TMPDIR)
                if (!tmpdir.mkdirs()) {
                    showErrorDialog(context, context.getString(R.string.srestoreerror),
                        "Can't create temp restore")
                    return null
                }
                orig = context.contentResolver.openInputStream(zipfile.uri)

                if (orig == null){
                    showErrorDialog(context, context.getString(R.string.srestoreerror),
                        "Can't open backup file to restore")
                    tmpdir.deleteRecursively()
                }
                zip = ZipInputStream (orig)
                val data: ByteArray = ByteArray(BUFFSIZE)
                var count = 0
                val md = MessageDigest.getInstance("MD5")
                while (true){
                    val ze = zip.nextEntry
                    if (ze == null)
                        break
                    if (ze.name == DIGESTFILE) {
                        count = zip.read(digest, 0, MD5LEN)
                        if (count != MD5LEN) {
                            showErrorDialog(context, context.getString(R.string.srestoreerror),
                                "Invalid backup file")
                            return null
                        }
                        continue
                    }
                    val out = FileOutputStream (File (tmpdir, ze.name))

                    while (true){
                        count = zip.read(data, 0, BUFFSIZE)
                        if (count == -1)
                            break
                        out.write(data, 0, count)
                        md.update(data, 0, count)
                    }
                    out.close()
                }
                zip.close()
                zip = null
                orig?.close()
                orig = null
                val smd = md.digest().toHexString()
                val dg = String (digest) //digest.toString()
                if ( smd != dg){
                    showErrorDialog(context, context.getString(R.string.srestoreerror),
                        "Invalid backup file")
                    tmpdir.deleteRecursively()
                    return null
                }

                return tmpdir
            }
            catch (e: Exception){
                zip?.close()
                orig?.close()
                showErrorDialog(context, context.getString(R.string.srestoreerror),
                    "Error restoring database", e)
                throw e
            }
            return null
        }
        private const val OLDSUFFIX = ".old"
        fun RestoreDatabase (context: MainActivity, zipfile: DocumentFile, tempdir: File,
                            appdir: String){
            var databasesdir: File? = null
            var tmpdir: File? = null
            val filesdone = ArrayList<File> ()
            try {
                for (f in File(appdir).listFiles()!!) {
                    if (f.name.contains("databases")) {
                        databasesdir = f
                        break;
                    }
                }
                if (databasesdir == null) {
                    showErrorDialog(context, context.getString(R.string.srestoreerror),
                        context.getString(R.string.nodatabasedir))
                    return
                }
                tmpdir = CreateTmpDir(context, zipfile, tempdir)
                if (tmpdir == null) {
                    return
                }
                var haserror = false
                for (fo in tmpdir.listFiles()!!){
                    for (fd in databasesdir.listFiles()!!){
                        if (fo.name == fd.name){
                            val olddest = File (databasesdir, "${fd.name}${OLDSUFFIX}")
                            val dest = File (fd.absolutePath)
                            if (!fd.renameTo(olddest)) {
                                haserror = true
                                break
                            }
                            if (!fo.renameTo(dest)) {
                                haserror = true
                                break
                            }
                            filesdone.add(dest)
                            break
                        }
                    }
                    if (haserror)
                        break
                }
                if (!haserror){
                    for (f in databasesdir.listFiles()!!){
                        if (f.name.endsWith(OLDSUFFIX))
                            f.delete()
                    }
                }
                else { //Rollback
                    for (f in filesdone)
                        f.delete()
                    for (f in databasesdir.listFiles()!!){
                        if (f.name.endsWith(OLDSUFFIX)){
                            val path = f.absolutePath
                            val newpath = path.substring(0, path.lastIndexOf(OLDSUFFIX))
                            val fd = File (newpath)
                            f.renameTo(fd)
                        }
                    }
                    showErrorDialog(context, context.getString(R.string.srestoreerror),
                        "Error restoring files in database")
                }
                tmpdir.deleteRecursively()
            }
            catch (e: Exception){
                DeleteTmp(tempdir)
                for (f in filesdone)
                    f.delete()

                for (f in databasesdir!!.listFiles()!!){
                    if (f.name.endsWith(OLDSUFFIX)){
                        val path = f.absolutePath
                        val newpath = path.substring(0, path.lastIndexOf(OLDSUFFIX))
                        val fd = File (newpath)
                        f.renameTo(fd)
                    }
                }

                showErrorDialog(context, context.getString(R.string.srestoreerror),
                    "Error restoring database", e)
            }
        }
    }
}