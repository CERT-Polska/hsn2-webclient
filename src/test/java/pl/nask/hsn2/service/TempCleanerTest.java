package pl.nask.hsn2.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TempCleanerTest {
	private static final String FILE_PREFIX = "htmlunit-test";
	private String lastFilePath;
	private int delCount;
	
	@BeforeTest
	public void beforeTest() throws IOException {
		for (int i = 0; i < 3; i++){
			File file = File.createTempFile(FILE_PREFIX, null);
			file.setLastModified(0);
			file.deleteOnExit();
		}
		File lastFile = File.createTempFile(FILE_PREFIX, null);
		lastFile.deleteOnExit();
		lastFilePath = lastFile.getPath();
		delCount = new TempCleaner().clearTemp(900000);
	}
	
	@Test
	public void oldFileTest() {
		Assert.assertEquals(delCount, 3, "Not all files was deleted.");
	}
	
	@Test(dependsOnMethods="oldFileTest")
	public void newFileTest() {
		Assert.assertTrue(Files.exists(Paths.get(lastFilePath)), "Young file was deleted.");
	}
	
}
