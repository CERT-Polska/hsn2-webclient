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
	public void run() {
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
	
	int clearTemp(long interval){
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
	
	private class HtmlUnitFileFilter implements FileFilter{
		
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
