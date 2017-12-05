package com.jscrape;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Scrapes concert information on WeGotTickets and returns the data in JSON
 * format
 * 
 * @author Angus
 *
 */
public class Scraper {
	private static final Logger logger = LogManager.getLogger("Scraper");

	private Document doc;

	// scraped pages to local hard disk
	private List<File> downloadFiles = new ArrayList<File>();

	// storing the last date of scrape
	File timestampFile = null;

	// output file in JSON format
	File JSONOutputFile = null;

	// application directory
	File scrapedDir = null;

	// evnet list in JSON object format
	List<JSONObject> eventList = new ArrayList<JSONObject>();

	/**
	 * Constructor
	 */
	public Scraper() {
		init();
	}

	/**
	 * initialize path
	 */
	private void init() {
		String userDir = System.getProperty("user.dir");

		scrapedDir = new File(userDir + File.separator + "scraped_files");

		if (!scrapedDir.exists()) {
			if (scrapedDir.mkdir()) {
			} else {
				logger.error(String.format("Failed to create folder at %s", scrapedDir.toString()));
			}
		}
		timestampFile = new File(scrapedDir + File.separator + "lastScrapeDate");
		JSONOutputFile = new File(scrapedDir + File.separator + "output.json");
	}

	/**
	 * Start scrape
	 * 
	 * @param inputFile
	 * @param charsetName
	 * @param baseUri
	 */
	public void scrape(File inputFile, String charsetName, String baseUri) {
		try {
			doc = Jsoup.parse(inputFile, charsetName, baseUri);
		} catch (IOException e) {
			logger.error(String.format("The file could not be found, or read, or if the charsetName is invalid.",
					inputFile.toURI()));
		}

		try {
			Elements events = doc.select("div.content.block-group.chatterbox-margin");

			for (Element event : events) {
				String artist = null;
				String city = null;
				String venue = null;
				String date = null;
				String price = null;

				artist = event.select("h2 > a.event_link").text();

				Element venueBlockElement = event.select("div.block.diptych.chatterbox-margin > div.venue-details")
						.first();
				String venueBlock = venueBlockElement.child(0).text();
				int semicolonIndex = venueBlock.indexOf(":");
				if (semicolonIndex != -1) {
					city = venueBlock.substring(0, semicolonIndex);
					venue = venueBlock.substring(semicolonIndex + 1);
				} else {
					venue = venueBlock;
				}
				date = venueBlockElement.child(1).text();

				Element priceElement = event.select("div.block.diptych.text-right > div.searchResultsPrice > strong")
						.first();

				if (priceElement != null) {
					price = priceElement.text().replace("£", "");
				}

				// logger.debug(String.format("Artist: %s", artist));
				// logger.debug(String.format("City: %s", city));
				// logger.debug(String.format("Venue: %s", venue));
				// logger.debug(String.format("Date: %s", date));
				// logger.debug(String.format("Price: %s", price));

				JSONObject eventJSONObj = new JSONObject();
				if (artist == null) {
					eventJSONObj.put("artist", artist);
				} else {
					eventJSONObj.put("artist", artist.trim());
				}
				if (city == null) {
					eventJSONObj.put("city", city);
				} else {
					eventJSONObj.put("city", city.trim());
				}
				if (venue == null) {
					eventJSONObj.put("venue", venue);
				} else {
					eventJSONObj.put("venue", venue.trim());
				}
				if (date == null) {
					eventJSONObj.put("date", date);
				} else {
					eventJSONObj.put("date", date.trim());
				}
				if (price == null) {
					eventJSONObj.put("price", price);
				} else {
					eventJSONObj.put("price", Double.parseDouble(price.trim()));
				}

				eventList.add(eventJSONObj);
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generate output file in JSON format
	 */
	public void generateJSONFile() {
		BufferedWriter bw = null;

		try {
			bw = new BufferedWriter(new FileWriter(JSONOutputFile));
			for (JSONObject event : eventList) {
				bw.append(event.toJSONString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Scrape the maximum page number
	 * 
	 * @param url
	 * @return
	 */
	public int scrapeMaxPageNo(String url) {
		try {
			doc = Jsoup.connect(url).get();
			Elements pageLinkElements = doc.select(".pagination_link");

			if (pageLinkElements != null) {
				return Integer.parseInt(pageLinkElements.get(pageLinkElements.size() - 1).html());
			} else {
				return 0;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Download the online page
	 * 
	 * @param url
	 * @param pageNo
	 * @return
	 */
	public boolean downloadFile(String url, int pageNo) {
		final String downloadFilePath = String.format("page%d", pageNo);
		Path path = Paths.get(scrapedDir.toString());
		path = path.resolve(downloadFilePath);

		logger.info(String.format("File downloaded at %s\n", path.toString()));

		File outFile = new File(path.toString());
		try {
			if (outFile.createNewFile()) {
				CloseableHttpClient httpclient = HttpClients.createDefault();
				HttpGet httpGet = new HttpGet(url);
				CloseableHttpResponse response = httpclient.execute(httpGet);

				try {
					// logger.info(response.getStatusLine());
					HttpEntity entity = response.getEntity();
					if (entity != null) {
						OutputStream outstream = new FileOutputStream(outFile);
						entity.writeTo(outstream);
						downloadFiles.add(outFile);
						outstream.close();
						return true;
					} else {
						logger.warn("Response entity is null");
					}
				} finally {
					response.close();
				}
			} else {
				logger.warn(String.format("File %s already exists\n", downloadFilePath));
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Create file to store the last scrape date
	 */
	public void createTimestampFile() {
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String timestamp = sdf.format(c.getTime());

		OutputStream os = null;
		PrintWriter pw = null;
		try {
			os = new FileOutputStream(timestampFile);
			pw = new PrintWriter(os);
			pw.write(timestamp);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				pw.close();
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get the list of download pages
	 * 
	 * @return
	 */
	public List<File> getDownloadFiles() {
		return downloadFiles;
	}

	/**
	 * Get the file that stores the last scrape date
	 * 
	 * @return
	 */
	public File getTimestampFile() {
		return timestampFile;
	}

	public List<JSONObject> getEventList() {
		return eventList;
	}

	public File getScrapedDir() {
		return scrapedDir;
	}
}
