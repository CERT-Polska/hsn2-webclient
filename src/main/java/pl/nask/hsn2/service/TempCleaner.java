/*
 * Copyright (c) NASK, NCSC
 * 
 * This file is part of HoneySpider Network 2.1.
 * 
 * This is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.nask.hsn2.service;

import java.io.File;
import java.io.FileFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TempCleaner implements Runnable{
	private static final Logger LOGGER = LoggerFactory.getLogger(TempCleaner.class);
	private static final long INTERVAL = 15 * 60 * 1000;
	private static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));
	private static final String FILE_PREFIX = "htmlunit";

	@Override
	public final void run() {
		LOGGER.info("TempCleaner started.");
		while(true){
			int i = clearTemp(INTERVAL);
			LOGGER.info(i +" temp files deleted");
			try {
				Thread.sleep(INTERVAL);
			} catch (InterruptedException e) {
				LOGGER.info("TempCleaner closed.");
				return;
			}
		}
	}

	final int clearTemp(long interval){
		int i = 0;
		for (File file : TMP_DIR.listFiles(new HtmlUnitFileFilter(interval))){
			boolean isDel = file.delete();
			if (isDel){
				i++;
			}
			else{
				LOGGER.warn("Can not delete temp file: "+ file.getName());
			}
		}
		return i;
	}

	private static class HtmlUnitFileFilter implements FileFilter{

		private long interval;

		public HtmlUnitFileFilter(long interval) {
			this.interval = interval;
		}

		@Override
		public boolean accept(File file) {
			boolean isHtmlUnitFile = file.getName().startsWith(FILE_PREFIX);
			if (isHtmlUnitFile && !file.isDirectory()){
				long currentTime = System.currentTimeMillis();
				return file.lastModified() < currentTime - interval;
			}
			return false;
		}
	}
}
