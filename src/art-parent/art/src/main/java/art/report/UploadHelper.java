/*
 * ART. A Reporting Tool.
 * Copyright (C) 2017 Enrico Liboni <eliboni@users.sf.net>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package art.report;

import art.servlets.Config;
import art.utils.ArtUtils;
import art.utils.FinalFilenameValidator;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

/**
 * Provides helper methods for saving uploaded files to the server
 * 
 * @author Timothy Anyona
 */
public class UploadHelper {
	
	private static final Logger logger = LoggerFactory.getLogger(UploadHelper.class);
	
		/**
	 * Saves a file and updates the report template property with the file name
	 *
	 * @param file the file to save
	 * @param templatesPath the directory in which to save the file
	 * @param validExtensions the allowed file extensions
	 * @return an i18n message string if there was a problem, otherwise null
	 * @throws IOException
	 */
	public String saveFile(MultipartFile file, String templatesPath,
			List<String> validExtensions) throws IOException {

		logger.debug("Entering saveFile: templatesPath='{}'", templatesPath);

		logger.debug("file==null = {}", file == null);
		if (file == null) {
			return null;
		}

		logger.debug("file.isEmpty()={}", file.isEmpty());
		if (file.isEmpty()) {
			//can be empty if a file name is just typed
			//or if upload a 0 byte file
			//don't show message in case of file name being typed
			return null;
		}

		//check file size
		long maxUploadSize = Config.getSettings().getMaxFileUploadSizeMB(); //size in MB
		maxUploadSize = maxUploadSize * 1000L * 1000L; //size in bytes

		long uploadSize = file.getSize();
		logger.debug("maxUploadSize={}, uploadSize={}", maxUploadSize, uploadSize);

		if (maxUploadSize >= 0 && uploadSize > maxUploadSize) { //-1 or any negative value means no size limit
			return "reports.message.fileBiggerThanMax";
		}

		String filename = file.getOriginalFilename();
		logger.debug("filename='{}'", filename);
		String extension = FilenameUtils.getExtension(filename);

		if (!ArtUtils.containsIgnoreCase(validExtensions, extension)) {
			return "reports.message.fileTypeNotAllowed";
		}

		if (!FinalFilenameValidator.isValid(filename)) {
			return "reports.message.invalidFilename";
		}

		//save file
		String destinationFilename = templatesPath + filename;
		File destinationFile = new File(destinationFilename);
		file.transferTo(destinationFile);

		return null;
	}
	
}