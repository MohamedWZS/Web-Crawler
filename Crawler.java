package webCrawler;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;

public class Crawler implements Runnable{
	
	// Data-Members
	private static final int MAX_DEPTH = 10;
	private Thread thread;
	private String firstLink;
	public static ArrayList<String> visitedLinks;
	private int threadID;
	private final int threshold = 5000;
	private static int count = 0;
	
	
	public int getSize() {
		return visitedLinks.size();
	}
	
	// Constructor
	public Crawler() {
		
	}
	
	// Constructor
	public Crawler(String link, int num, ArrayList<String> container) {
		System.out.println("crawler created");
		firstLink = link;
		threadID = num;
		visitedLinks = container;
		
		
		thread = new Thread(this);
		thread.start();
	}
	
	// crawl function
	private void crawl(int level, String url) throws URISyntaxException {
		if(level <= MAX_DEPTH) {
			Document doc = request(url);

			// Debug print the doc html
			// System.out.println(doc);
			
			// if not an empty document
			if(doc != null) {
				for(Element link : doc.select("a[href]")) {
					// pick the next link inside 
					String next_link = link.absUrl("href");
					// in case of a link that does not exist in the visited list crawl the link
					next_link = normalize(next_link);
					if(visitedLinks.contains(next_link) == false) {
						crawl(level++, next_link);
					}
				}
			}
		}
	}
	
	private Document request(String url) throws URISyntaxException {
		try {
			// connect the url
			Connection con = Jsoup.connect(url);
			// Execute the request as a GET, and parse the result.
			Document doc = con.get();
			
			// An HTTP status code 200 means success. 
			// The client has requested documents from the server. 
			// The server has replied to the client and given the client the documents.
			if(con.response().statusCode() == 200) {

				// add link if size < threshold
				if (Crawler.visitedLinks.size() <= this.threshold) {		
					synchronized(Crawler.visitedLinks) {
						System.out.println("\n**Crawler Id: " + threadID + " Webpage Received is:" + url);
						
						String title = doc.title();
						//	System.out.println(title);
						
						// normalize
						// url = normalize(url);

						visitedLinks.add(url);
						
						// download the webpages and output the links
						String stringfiedUrl = url.toString();
						downloadPage(stringfiedUrl);
						writeLinks(stringfiedUrl);

						// Debug: live printing of links size
						System.out.println(visitedLinks.size());
						
						// return the doc if it is the case
						return doc;
					}
				}

			}
			
			// if not the case return null
			return null;
		}
		catch(IOException e) {
			return null;
		}
	}
	
	public String normalize(String webPageUrl) throws URISyntaxException {
		
		// case of no url
		if (webPageUrl == null) {
			return null;
		}
		
		// retrieve the ? mark index
		int questionMarkIndex = webPageUrl.indexOf('?');
		if (questionMarkIndex != -1) {
			webPageUrl = webPageUrl.substring(0, questionMarkIndex);
		}
		
		// retrieve the '#' mark index
//		int hashMarkIndex = webPageUrl.indexOf('#');
//		if (questionMarkIndex != -1) {
//			webPageUrl = webPageUrl.substring(0, hashMarkIndex);
//		}
		
		// check for absolute url
		URI absoluteUriChecker = new URI(webPageUrl); 
		
		if(absoluteUriChecker.isAbsolute() == false) {
			throw new URISyntaxException(webPageUrl, "this is not an absolute url");
		}
		
		String webPagePath = absoluteUriChecker.getPath();
		
		if(webPagePath != null) {
			webPagePath = webPagePath.replaceAll("//*/", "/");
			if(webPagePath.length() > 0 && webPagePath.charAt(webPagePath.length() - 1) == '/') {
				webPagePath = webPagePath.substring(0, webPagePath.length() - 1);
			}
		}
		
		String resultUri = new URI(absoluteUriChecker.getScheme(), absoluteUriChecker.getUserInfo(), absoluteUriChecker.getHost(), absoluteUriChecker.getPort(), webPagePath, absoluteUriChecker.getQuery(), absoluteUriChecker.getFragment()).toString();
		
		return resultUri;
	}
	
	public void writeLinks(String webPageUrl) {
		try {
		      FileWriter myWriter = new FileWriter("D:\\\\CUFE\\\\year 3\\\\Term 2\\\\APT\\\\ECLIPSE WORK SPACE\\\\SearchEngine\\\\src\\\\webCrawler\\\\outLinks.txt", true);
		      BufferedWriter bw = new BufferedWriter(myWriter);
		      PrintWriter pw = new PrintWriter(bw);
		      pw.println(webPageUrl);
//		      pw.println(url);
		      System.out.println("Successfully wrote to the file.");
		      pw.flush();
		    } catch (IOException e) {
		      System.out.println("An error occurred.");
		      e.printStackTrace();
		    }
	}
	
	
	public void downloadPage(String webPageUrl) {
		try {
			
			// Create URL object
            URL url = new URL(webPageUrl);
            BufferedReader readr = 
              new BufferedReader(new InputStreamReader(url.openStream()));
            
            // Enter filename in which you want to download
            BufferedWriter writer = 
              new BufferedWriter(new FileWriter("D:\\CUFE\\year 3\\Term 2\\APT\\ECLIPSE WORK SPACE\\SearchEngine\\src\\webCrawler\\Downloads\\Download" + Crawler.count + ".html"));
            
            // increase the count
            Crawler.count += 1;
            
            // read each line from stream till end
            String line;
            while ((line = readr.readLine()) != null) {
                writer.write(line);
            }
  
            readr.close();
            writer.close();
            System.out.println("Successfully Downloaded.");
		}
		// Exceptions
        catch (MalformedURLException mue) {
            System.out.println("Malformed URL Exception raised");
        }
        catch (IOException ie) {
            System.out.println("IOException raised");
        }
	}

	public Thread getThread() {
		return thread;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			crawl(1, firstLink);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException {
		
		// list contains all the crawlers threads
		ArrayList<Crawler> bots = new ArrayList<Crawler>();
		
		// list will contain all the crawled urls
		ArrayList<String> container = new ArrayList<String>();
		
		// list will contain all the seeds from seed list file
		ArrayList<String> seeds = new ArrayList<String>();
		
		// read seedList file
		File file = new File("D:\\CUFE\\year 3\\Term 2\\APT\\ECLIPSE WORK SPACE\\SearchEngine\\src\\webCrawler\\seedList.txt"); //creates a new file instance 

		//reads the file 
		FileReader fr = new FileReader(file);
		
		//creates a buffering character input stream  
		BufferedReader br=new BufferedReader(fr);  
		
		String line;
		while((line=br.readLine())!=null)   {
			seeds.add(line);
		}
		
		//closes the stream and release the resources 
		fr.close(); 
		
		System.out.println("Contents of Seed File: ");
		
		int seedCounter = 1;
		for (String link : seeds) {
			System.out.println(link);
			bots.add(new Crawler(link, seedCounter, container));
			seedCounter += 1;
		}
			
		for(Crawler c : bots) {
			try {
				c.getThread().join();
			}
			catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println(new Crawler().getSize());
		
		for(String s : container) {
			System.out.println(s);
		}
		
	}

}


//bots.add(new Crawler("https://www.geeksforgeeks.org/", 1, container));
//bots.add(new Crawler("https://www.npr.org", 2, container));
//bots.add(new Crawler("https://www.nytimes.com", 3, container));
//bots.add(new Crawler("https://en.wikipedia.org/wiki/Mohamed_Salah", 4, container));