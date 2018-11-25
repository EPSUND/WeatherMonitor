package weather;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

@SuppressWarnings("serial")
public class WeatherMonitorServlet extends HttpServlet {
	private static String WUNDERGROUND_KEY = "ca53772b6a78c29a";//Replace with your own
	private static String WEATHER_DATA_FORMAT = ".xml";
	private static String WEATHER_URL = "http://api.wunderground.com/api/" + WUNDERGROUND_KEY + "/geolookup/conditions/q/";
	private static final Logger logger = Logger.getLogger(WeatherMonitorServlet.class.getName());
	
	/**
	  * Gets the visitors location in longitude and latitude and uses Wunderground to get the weather at that location
	  */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		prepareWeatherResponse(request, response);
	}

	  /**
	   * Gets the visitors location in longitude and latitude and uses Wunderground to get the weather at that location
	   */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		prepareWeatherResponse(request, response);
	  }
	
	/**
	 * Prepare a response containing weather data
	 * @param request The request
	 * @param response The response that will have weather data added to it
	 * @throws IOException
	 */
	private void prepareWeatherResponse(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String visitorLatLong = request.getHeader("X-AppEngine-CityLatLong");
		
        String[] weatherData = getWeatherData(WEATHER_URL + visitorLatLong + WEATHER_DATA_FORMAT);
        
        response.setContentType("text/plain");
        for(String line : weatherData)
        	response.getWriter().println(line);
	}
	
	/**
	 * Gets weather data from wunderground and parses it 
	 * @param weatherURLString The URL of the weather data as a string
	 * @return The weather data
	 */
	private String[] getWeatherData(String weatherURLString)
	{
		String[] weatherData = new String[]{null, null, null, null};
		InputStream weatherStream = null;
		
		try
        {
			//Get an input stream to the weather data
        	URL weatherURL = new URL(weatherURLString);
        	URLConnection conn = weatherURL.openConnection();
        	weatherStream = conn.getInputStream();
			//Parse the weather data
        	weatherData = parseWeatherXML(weatherStream);
        }
        catch(Exception e)
        {
        	logger.warning("Could not get weather data");
        }
		finally
		{
			if(weatherStream != null)
			{
				try {
					weatherStream.close();
				} catch (IOException e) {
					logger.warning("Could not close weather input stream");
				}
			}
		}
		
		return weatherData;
	}
	
	/**
	 * Parses the weather XML data retrived from wunderground
	 * @param weatherData The weather data in a input stream
	 * @return The parsed weather data
	 */
	private String[] parseWeatherXML(InputStream weatherData)
	{
		String[] weatherStrings = new String[]{null, null, null, null};
		
		try
		{
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(weatherData);
			
			NodeList observations = doc.getElementsByTagName("current_observation");//Should only be one
			Node observationNode = observations.item(0);
			Element observation = (Element)observationNode;
			
			NodeList observationLocs = observation.getElementsByTagName("observation_location");//Should only be one
			Node observationLocNode = observationLocs.item(0);
			Element observationLoc = (Element)observationLocNode;
			
			weatherStrings[0] = observationLoc.getElementsByTagName("city").item(0).getTextContent();
			weatherStrings[1] = observation.getElementsByTagName("temp_c").item(0).getTextContent();
			weatherStrings[2] = observation.getElementsByTagName("icon").item(0).getTextContent();
			weatherStrings[3] = observation.getElementsByTagName("icon_url").item(0).getTextContent();
		}
		catch(Exception e)
		{
			logger.warning("Could not parse weather data");
		}
		
		return weatherStrings;
	}
}
