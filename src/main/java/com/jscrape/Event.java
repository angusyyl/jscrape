package com.jscrape;

/**
 * Entity class defined for future use on data manipulation
 * 
 * @author Angus
 *
 */
public class Event {
	private String artist;
	private String city;
	private String venue;
	private String date;
	private String price;

	public Event(String artist, String city, String venue, String date, String price) {
		super();
		this.artist = artist;
		this.city = city;
		this.venue = venue;
		this.date = date;
		this.price = price;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getVenue() {
		return venue;
	}

	public void setVenue(String venue) {
		this.venue = venue;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}
}
