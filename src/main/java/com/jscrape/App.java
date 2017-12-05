package com.jscrape;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Main application to call scraper It is designed to scrape data once for each
 * day to avoid heavy requests made to WeGotTickets which may block our
 * requests. Previous downloaded page(s) and lastScrapeDate file in
 * scraped_files folder should be deleted for immediate re-run if necessary
 * 
 * @author Angus
 *
 */
public class App {
	private static final Logger logger = LogManager.getLogger("App");

	public static void main(String[] args) throws Exception {
		int maxPageNo = 0;

		String url = "http://www.wegottickets.com/searchresults/all";

		// get max page number
		Scraper scrape = new Scraper();
		maxPageNo = scrape.scrapeMaxPageNo(url);
		logger.info(String.format("Max page no.: %d\n", maxPageNo));

		// only scrape if the last scrape date is one day before
		FileReader fr;
		BufferedReader br;
		long lastTimestamp = 0;

		try {
			fr = new FileReader(scrape.getTimestampFile());
			br = new BufferedReader(fr);
			while (br.ready()) {
				lastTimestamp = Long.parseLong(br.readLine());
			}
		} catch (FileNotFoundException e) {
			logger.info("The first time to scrape");
		}

		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		long todayTimestamp = Long.parseLong(sdf.format(c.getTime()));
		if (todayTimestamp > lastTimestamp) {
			// download page to hard disk
			for (int pageNo = 1; pageNo < maxPageNo; pageNo++) {
				url = String.format("http://www.wegottickets.com/searchresults/page/%d/all", pageNo);
				if (scrape.downloadFile(url, pageNo)) {
					logger.info(String.format("Downloaded page %s\n", url));
				} else {
					logger.info(String.format("Error occurs when downloading %s\n", url));
				}
			}
			scrape.createTimestampFile();

			logger.info(String.format("Today scraped date is %d.", todayTimestamp));

			// scrape for each downloaded page
			for (File pageFile : scrape.getDownloadFiles()) {
				scrape.scrape(pageFile, "UTF-8", "");
			}
			scrape.generateJSONFile();
			logger.info("Generated JSON output file");
		} else {
			logger.info(String.format(
					"Last scraped date is %d. It is designed to scrape data once for each day to avoid heavy requests made to WeGotTickets which may block our requests.",
					lastTimestamp));
			logger.info(
					"Please remove the previous downloaded page(s) and lastScrapeDate file in scraped_files folder for immediate re-run if necessary");
		}
	}
}
