package fedora.utilities;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
	public static final int BUFFER = 2048;
	
	public static void copy(InputStream source, OutputStream destination) throws IOException {
    	int count;
    	byte data[] = new byte[BUFFER];
    	BufferedOutputStream dest = new BufferedOutputStream(destination, BUFFER);
    	while ((count = source.read(data, 0, BUFFER)) != -1) {
              dest.write(data, 0, count);
    	}
       dest.flush();
       dest.close();
    }
	
	public static void copy(File source, File destination) throws IOException {
		if (!destination.exists() && destination.isDirectory()) {
			destination.mkdirs();
		}
		if (source.isDirectory()) {
			File[] children = source.listFiles();
			for (int i = 0; i < children.length; i++) {
				copy(new File(source, children[i].getName()), 
						new File(destination, children[i].getName()));
			}
		} else {
			InputStream in = new FileInputStream(source);
	    	OutputStream out = new FileOutputStream(destination);
	    	copy(in, out);
	    	out.close();
	    	in.close();
		}
    }
	
	public static boolean delete(File file) {
        boolean result = true;

        if (file == null) {
        	return false;
        }
        if (file.exists()) {
        	if (file.isDirectory()) {
                // 1. delete content of directory:
                File[] children = file.listFiles();
                for (int i = 0; i < children.length; i++) { //for each file:
                    File child = children[i];
                    result = result && delete(child);
                }//next file
        	}
        	result = result && file.delete();
        } //else: input directory does not exist or is not a directory
        return result;
    }
	
	/**
	 * 
	 * @param file File or directory to delete
	 * @return 
	 */
	public static boolean delete(String file) {
        return delete(new File(file));
    }
}
