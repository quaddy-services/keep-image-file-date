/**
 * 
 */
package de.quaddyservices.image.exif;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;

/**
 * @author User
 * 
 */
public class KeepExifDate {
	private static final String[] PATTERN = new String[] { "yyyyMMdd_HHmm", "yyyyMMdd-HHmm", "yyyy-MM-dd_HHmm",
			"yyyy-MM-dd-HHmm", "yyyy-MM-dd", "yyyyMMdd" };
	private static final Format dateTimeFormat = new SimpleDateFormat(PATTERN[0]);
	private static final Format[] dateTime = new Format[PATTERN.length];

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {

		File tempCurrentDir = new File(".");
		StringBuilder tempUndoList = new StringBuilder();
		System.out.println("Scan " + tempCurrentDir.getAbsolutePath() + "..");
		File[] tempFiles = tempCurrentDir.listFiles();
		for (int i = 0; i < tempFiles.length; i++) {
			File tempFile = tempFiles[i];
			if (isMultimediaFile(tempFile)) {
				String tempNewName = tempFile.getAbsolutePath().replace('\\', '/');
				int tempPos = tempNewName.lastIndexOf("/");
				long tempCreationTime = getCreationTime(tempFile);
				tempNewName = tempNewName.substring(0, tempPos + 1) + dateTimeFormat.format(new Date(tempCreationTime))
						+ "-" + getShortName(tempNewName.substring(tempPos + 1));
				File tempNewFile = new File(tempNewName);
				tempFile.setLastModified(tempCreationTime);
				if (tempNewFile.getName().equalsIgnoreCase(tempFile.getName())) {
					// System.out.println(tempFile.getName() + " is already correct name.");
				} else {
					if (tempNewFile.exists()) {
						System.out.println("WARNING: Delete " + tempNewFile);
						tempNewFile.delete();
					}
					System.out.println(tempFile.getName() + " -> " + tempNewFile.getName());
					tempFile.renameTo(tempNewFile);
					tempUndoList.append("ren \"" + tempNewFile.getName() + "\" \"" + tempFile.getName() + "\"\r\n");
				}
			}
		}
		if (tempUndoList.length() > 0) {
			String tempBatName = "UndoRename-" + dateTime[0].format(new Date()) + ".bat";
			FileWriter tempUndo = new FileWriter(tempBatName);
			tempUndo.write(tempUndoList.toString());
			tempUndo.write("del " + tempBatName + "\r\n");
			tempUndo.write("pause");
			tempUndo.close();
		}
		System.out.println("Wait 10 Seconds to finish...");
		Thread.sleep(10 * 1000);
		System.exit(0);

	}

	private static long getCreationTime(File aFile) {
		long tempCreationTime;
		try {
			Metadata tempMetadata = ImageMetadataReader.readMetadata(aFile);
			//			printInfo(tempMetadata);
			ExifSubIFDDirectory tempExifDirectory = tempMetadata.getDirectory(ExifSubIFDDirectory.class);
			if (tempExifDirectory == null) {
				System.out.println("No Exif information in " + aFile.getName());
				tempCreationTime = aFile.lastModified();
			} else {
				Date tempDate = tempExifDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED);
				if (tempDate == null) {
					tempDate = tempExifDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
				}
				if (tempDate == null) {
					System.out.println("No Date in Exif of " + aFile.getName()
							+ " ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED/.TAG_DATETIME_ORIGINAL");
					tempCreationTime = aFile.lastModified();
				} else {
					tempCreationTime = tempDate.getTime();
				}
			}
		} catch (ImageProcessingException e) {
			System.out.println("Ignore " + aFile.getName() + ":" + e);
			e.printStackTrace(System.out);
			tempCreationTime = aFile.lastModified();
		} catch (IOException e) {
			System.out.println("Ignore " + aFile.getName() + ":" + e);
			e.printStackTrace(System.out);
			tempCreationTime = aFile.lastModified();
		}

		return tempCreationTime;
	}

	private static void printInfo(Metadata aMetadata) {
		Iterable<Directory> tempDirectories = aMetadata.getDirectories();
		for (Directory tempDirectory : tempDirectories) {
			Collection<Tag> tempTags = tempDirectory.getTags();
			for (Tag tempTag : tempTags) {
				Object tempObject = tempDirectory.getObject(tempTag.getTagType());
				if (tempObject != null) {
					System.out.println(tempDirectory + " " + tempTag + "=" + tempObject + " (" + tempObject.getClass()
							+ ")");
				}
			}
		}
	}

	private static boolean isMultimediaFile(File tempFile) {
		String tempLowerName = tempFile.getName().toLowerCase();
		return tempLowerName.endsWith(".jpg") || tempLowerName.endsWith(".avi") || tempLowerName.endsWith(".3gp")
				|| tempLowerName.endsWith(".mov") || tempLowerName.endsWith(".mpo") || tempLowerName.endsWith(".mp4");
	}

	/**
	 * Remove a previously added Date Info.
	 * 
	 * @param aName
	 * @return
	 */
	private static String getShortName(String aName) {
		String tempName = aName;
		for (int i = 0; i < dateTime.length; i++) {
			String tempString = PATTERN[i];
			Format tempFormat = dateTime[i];
			if (tempFormat == null) {
				tempFormat = new SimpleDateFormat(tempString);
				dateTime[i] = tempFormat;
			}
			if (tempName.length() >= tempString.length()) {
				try {
					String tempStart = tempName.substring(0, tempString.length());
					tempFormat.parseObject(tempStart);
					String tempShort = tempName.substring(tempStart.length());
					while (tempShort.startsWith("-")) {
						tempShort = tempShort.substring(1);
					}
					return tempShort;
				} catch (java.text.ParseException e) {
				}
			}
		}
		return tempName;
	}
}
