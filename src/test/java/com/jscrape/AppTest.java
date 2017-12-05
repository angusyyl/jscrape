package com.jscrape;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for App
 * 
 * @author Angus
 *
 */
public class AppTest {

	Scraper scrape;

	@Before
	public void init() {
		scrape = new Scraper();
	}

	@Test
	public void testGetMaxPageNo() {
		String url = "http://www.wegottickets.com/searchresults/all";

		// get max page number
		assertTrue(scrape.scrapeMaxPageNo(url) > 0);
	}

	@Test
	public void testDownloadAndScrape() {
		// test download function works
		int pageNo = 1;

		assertTrue(scrape.downloadFile(String.format("http://www.wegottickets.com/searchresults/page/%d/all", pageNo),
				pageNo));

		// test ten events are downloaded
		for (File pageFile : scrape.getDownloadFiles()) {
			scrape.scrape(pageFile, "UTF-8", "");
		}

		assertEquals(scrape.getEventList().size(), 10);

		// test timestamp file is created
		File timestampFile = new File(scrape.getScrapedDir() + File.separator + "lastScrapeDate");

		scrape.createTimestampFile();

		assertTrue(timestampFile.exists());

		// test output file in JSON format is generated
		File JSONOutputFile = new File(scrape.getScrapedDir() + File.separator + "output.json");

		scrape.generateJSONFile();

		assertTrue(JSONOutputFile.exists());

	}
}
