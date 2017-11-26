package com.gmail.mattdiamond98.proxyengine;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.imageio.ImageIO;

public class ProxyEngine {
	
	public static final List<String> CARD_NAMES = new ArrayList<>();
	
	public static final int CARD_WIDTH = 223;
	public static final int CARD_HEIGHT = 311;
	
	public static final File DECK_FILE = new File("deck.txt");
	
	public static Map<String, Image> cardImages = new HashMap<>();
	
	public static void main(String[] args) throws MalformedURLException, IOException {
		System.out.println();
		System.out.println("Beginning execution...");
		
		loadCardsFromFile();
		fetchCardImages();
		
		System.out.println("Writing to files...");
		for (int i = 0; i < CARD_NAMES.size(); i+= 8) {
			saveSheetToFile(CARD_NAMES.subList(i, i+8 > CARD_NAMES.size() ? CARD_NAMES.size() : i + 8),
					new File("cards/list" + (i/8+1) + ".png"));
		}
		System.out.println("Task completed");
	}
	
	public static void loadCardsFromFile() throws IOException {
		if (!DECK_FILE.exists()) {
			DECK_FILE.createNewFile();
		}
		System.out.println("Loading deck...");
		Scanner scan = new Scanner(DECK_FILE);
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.length() > 0 && line.contains(" ")) {
				String cardName = line.substring(line.indexOf(" ")+1);
				int number = Integer.parseInt(line.substring(0, line.indexOf(" ")));
				for (int i = 0; i < number; i++) {
					addCardToList(cardName);
				}
			}
		}
		scan.close();
	}
	
	public static void addCardToList(String cardName) {
		if (cardName.contains("//")) {
			addCardToList(cardName.split("//")[0]);
			addCardToList(cardName.split("//")[1]);
		}
		else if (cardName.length() > 0){
			CARD_NAMES.add(cardName);
		}
	}
	
	public static void fetchCardImages() throws MalformedURLException, IOException {
		System.out.println("Fetching card images...");
		for (String name : CARD_NAMES) {
			if (!cardImages.containsKey(name)) {
				cardImages.put(name, getCardImageByName(name));
				System.out.println(">> Fetched image for: " + name);
			}
		}
	}
	
	/*
	 * Saves a set of eight cards to a file, in two rows of four.
	 */
	public static void saveSheetToFile(List<String> cardNames, File savefile) throws IOException {
		BufferedImage sheet = new BufferedImage(CARD_WIDTH * 4, CARD_HEIGHT * 2, BufferedImage.TYPE_INT_ARGB);
		Graphics g = sheet.getGraphics();
		for (int i = 0; i < cardNames.size(); i++) {
			Image image = cardImages.get(cardNames.get(i));
			g.drawImage(image, CARD_WIDTH * (i % 4), CARD_HEIGHT * (i / 4), null);
		}
		
		saveImageToFile(sheet, savefile);
	}
	
	public static void saveImageToFile(BufferedImage image, File savefile) throws IOException {
		if (!savefile.exists())
			savefile.createNewFile();
		ImageIO.write(image, "png", savefile);
	}
	
	public static int getCardIdByName(String name) throws MalformedURLException, IOException, NumberFormatException {
		URLConnection con = new URL("http://gatherer.wizards.com/Pages/Search/Default.aspx?action=advanced&name=+["+ name.replace(" ", "]+[").replace("'", "%27") +"]").openConnection();
		con.connect();
		InputStream is = con.getInputStream();
		is.close();
		String id = con.getURL().toString().split("=")[1];
		return Integer.parseInt(id);
	}
	
	/*
	 * If card starts with *, then get the card from mythicspoiler.com (Spoiled card that has not been released yet)
	 */
	public static Image getCardImageByName(String name) throws MalformedURLException, IOException {
		if (name.startsWith("*")) {
			URL url = new URL("http://mythicspoiler.com/emn/cards/" + name.toLowerCase().replace(" ", "").replace("*", "").replace(",", "") + ".jpg");
			BufferedImage image = ImageIO.read(url);
			return image.getScaledInstance(CARD_WIDTH, CARD_HEIGHT, Image.SCALE_SMOOTH);
		} else {
			try {
				return getCardImageById(getCardIdByName(name));
			} catch (NumberFormatException e) {
				String urlSource = getUrlSource("http://gatherer.wizards.com/Pages/Search/Default.aspx?action=advanced&name=+["+ name.replace(" ", "]+[").replace("'", "%27") +"]");
				String cardId = urlSource.substring(urlSource.indexOf("multiverseid=") + "multiverseid=".length());
					cardId = cardId.substring(0, cardId.indexOf("\""));
				return getCardImageById(Integer.parseInt(cardId));
			}
		}
	}
	
	public static BufferedImage getCardImageById(int id) throws MalformedURLException, IOException {
		return ImageIO.read(new URL("http://gatherer.wizards.com/Handlers/Image.ashx?multiverseid="+id+"&type=card"));
	}
	
	public static String getUrlSource(String url) throws IOException {
        URLConnection yc = new URL(url).openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream(), "UTF-8"));
        String inputLine;
        StringBuilder a = new StringBuilder();
        while ((inputLine = in.readLine()) != null)
            a.append(inputLine);
        in.close();
        return a.toString();
    }
}
