/**
 * 
 */
package de.quaddyservices.image.exif;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
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
		long tempNextInfoPrint = System.currentTimeMillis() + 5000;
		File tempCurrentDir = new File(".");
		StringBuilder tempUndoList = new StringBuilder();
		System.out.println("Scan " + tempCurrentDir.getAbsolutePath() + "..");
		File[] tempFiles = tempCurrentDir.listFiles();
		for (int i = 0; i < tempFiles.length; i++) {
			if (System.currentTimeMillis() > tempNextInfoPrint) {
				System.out.println(new Date() + ": " + i + "/" + tempFiles.length);
				tempNextInfoPrint = System.currentTimeMillis() + 5000;
			}
			File tempFile = tempFiles[i];
			if (isMultimediaFile(tempFile)) {
				String tempNewName = tempFile.getAbsolutePath().replace('\\', '/');
				int tempPos = tempNewName.lastIndexOf("/");
				long tempCreationTime = getCreationTime(tempFile);
				tempNewName = tempNewName.substring(0, tempPos + 1) + dateTimeFormat.format(new Date(tempCreationTime))
						+ "-" + getShortName(tempNewName.substring(tempPos + 1));
				File tempNewFile = new File(tempNewName);
				BasicFileAttributeView attributes = Files.getFileAttributeView(Paths.get(tempFile.toURI()),
						BasicFileAttributeView.class);
				FileTime time = FileTime.fromMillis(tempCreationTime);
				BasicFileAttributes tempAttributes = attributes.readAttributes();
				FileTime tempCurrentLastModifiedTime = tempAttributes.lastModifiedTime();
				FileTime tempCurrentCreationTime = tempAttributes.creationTime();
				boolean tempSetTimes = false;
				if (!time.equals(tempCurrentLastModifiedTime)) {
					System.out.println(
							tempFile.getName() + " LastModifiedTime: " + tempCurrentLastModifiedTime + " -> " + time);
					tempSetTimes = true;
				}
				if (!time.equals(tempCurrentCreationTime)) {
					System.out.println(tempFile.getName() + " CreationTim: " + tempCurrentCreationTime + " -> " + time);
					tempSetTimes = true;
				}
				if (tempSetTimes) {
					attributes.setTimes(time, null, time);
				}
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
			// printInfo(tempMetadata);

			List<Date> tempDates = new ArrayList<Date>();
			BasicFileAttributeView attributes = Files.getFileAttributeView(Paths.get(aFile.toURI()),
					BasicFileAttributeView.class);
			BasicFileAttributes tempAttributes = attributes.readAttributes();
			FileTime tempCurrentLastModifiedTime = tempAttributes.lastModifiedTime();
			FileTime tempCurrentCreationTime = tempAttributes.creationTime();

			tempDates.add(new Date(tempCurrentLastModifiedTime.toMillis()));
			tempDates.add(new Date(tempCurrentCreationTime.toMillis()));

			ExifSubIFDDirectory tempExifSubIFDDirectory = tempMetadata.getDirectory(ExifSubIFDDirectory.class);
			if (tempExifSubIFDDirectory == null) {
				System.out.println("No Exif information in " + aFile.getName());
			} else {
				Date tempDate = tempExifSubIFDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
				if (tempDate != null && tempDate.getTime() > 1000) {
					tempDates.add(new Date(tempDate.getTime()));
				}
				tempDate = tempExifSubIFDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED);
				if (tempDate != null && tempDate.getTime() > 1000) {
					tempDates.add(new Date(tempDate.getTime()));
				}
			}
			ExifIFD0Directory tempExifIFD0Directory = tempMetadata.getDirectory(ExifIFD0Directory.class);
			if (tempExifIFD0Directory == null) {
				System.out.println("No tempExifIFD0Directory information in " + aFile.getName());
			} else {
				Date tempDate = tempExifIFD0Directory.getDate(ExifIFD0Directory.TAG_DATETIME);
				if (tempDate != null && tempDate.getTime() > 1000) {
					tempDates.add(new Date(tempDate.getTime()));
				}
			}
			Collections.sort(tempDates);
			tempCreationTime = tempDates.get(0).getTime();
		} catch (ImageProcessingException e) {
			System.out.println("Ignore " + aFile.getName() + ":" + e);
			e.printStackTrace(System.out);
			tempCreationTime = aFile.lastModified();
		} catch (IOException e) {
			System.out.println("Ignore " + aFile.getName() + ":" + e);
			e.printStackTrace(System.out);
			tempCreationTime = aFile.lastModified();
		} catch (NoClassDefFoundError e) {
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
					System.out.println(tempDirectory + " " + tempTag + " TagType=" + tempTag.getTagType() + "="
							+ tempObject + " (" + tempObject.getClass() + ")");
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
