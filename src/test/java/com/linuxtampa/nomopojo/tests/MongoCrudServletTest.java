package com.linuxtampa.nomopojo.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.linuxtampa.nomopojo.servlet.MongoCrudServlet;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;

public class MongoCrudServletTest {

	public MongoCrudServletTest() {

	}

	@Mock
	HttpServletRequest request;
	@Mock
	HttpServletResponse response;
	@Mock
	HttpSession session;
	@Mock
	RequestDispatcher rd;

	private MongoClientURI uri = null;
	private MongoClient client = null;
	private MongoDatabase db = null;
	private final List<BasicDBObject> zipDocuments = new ArrayList<>();

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		// connect unit test to local mongo instance
		uri = new MongoClientURI("mongodb://localhost:27017/nomopojo");
		client = new MongoClient(uri);
		db = client.getDatabase(uri.getDatabase());

		File testdata = fetchTestData("http://media.mongodb.org/zips.json");
		loadTestDataIntoMongo(testdata);
	}

	private void loadTestDataIntoMongo(File testdata) throws IOException {

		System.out.println("loadTestDataIntoMongo(" + testdata.getName() + ") invoked");
		long startTS = System.currentTimeMillis();
		MongoCollection<BasicDBObject> zips = db.getCollection("zips", BasicDBObject.class);
		zips.drop();

		long dropTS = System.currentTimeMillis();

		List<String> zipLines = Files.readAllLines(testdata.toPath());
		zipDocuments.clear();
		for (String zipLine : zipLines) {
			BasicDBObject doc = (BasicDBObject) JSON.parse(zipLine);
			zipDocuments.add(doc);
		}
		zips.insertMany(zipDocuments);

		long insertTS = System.currentTimeMillis();

		System.out.println("    took " + (dropTS - startTS) + "ms to drop zips collection");
		System.out.println("    took " + (insertTS - dropTS) + "ms to insert " + zipDocuments.size()
				+ " documents to zips collection");
		System.out.println("    took " + ((1000.0 * zipDocuments.size()) / (insertTS - dropTS)) + " rows per second");

	}

	private File fetchTestData(String testdataLocation) throws IOException {

		BufferedReader in = null;
		FileWriter fw = null;
		URL testdataUrl = new URL(testdataLocation);
		File testdata = new File(testdataUrl.getFile().replaceAll("/", ""));
		try {

			if (testdata.exists() == false) {
				in = new BufferedReader(new InputStreamReader(testdataUrl.openStream()));
				fw = new FileWriter(testdata);

				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					System.out.println(line);
					sb.append(line);
					sb.append("\n");
				}
				fw.write(sb.toString());
				in.close();
				fw.close();
			}

		} catch (Exception ex) {
			testdata.delete();
		} finally {
			if (fw != null) {
				fw.close();
			}
			if (in != null) {
				in.close();
			}
		}
		return testdata;
	}
	@Test
	public void testChangePartialUpdate() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
	
		when(response.getWriter()).thenReturn(pw);
		when(request.getPathInfo()).thenReturn("/zips/33617");
		@SuppressWarnings("serial")
		HashMap<String, String[]> parameterMap = new HashMap<String, String[]>() {
			{
				put("city", new String[] { "TEMPLE TERRACE" });
			}
		};
		when(request.getParameterMap()).thenReturn(parameterMap);

		new MongoCrudServlet().doPut(request, response);

		String result = sw.getBuffer().toString().trim();
		System.out.println("Json Result As String is : " + result.length() + " characters long");
		System.out.println("first few lines of Json Result:\n" + result.substring(0, 400));


		BasicDBList json = (BasicDBList) JSON.parse(result);

	}

	@Test
	public void testGetZipsWithLimitAndTwoDifferentSkips() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		//
		// do first request with limit=50, skip=500
		//
		when(response.getWriter()).thenReturn(pw);
		when(request.getPathInfo()).thenReturn("/zips");
		int limit = 50;
		@SuppressWarnings("serial")
		HashMap<String, String[]> parameterMap = new HashMap<String, String[]>() {
			{
				put("limit", new String[] { "50" });
				put("skip", new String[] { "500" });
			}
		};
		when(request.getParameterMap()).thenReturn(parameterMap);

		new MongoCrudServlet().doGet(request, response);

		String result = sw.getBuffer().toString().trim();
		System.out.println("Json Result As String is : " + result.length() + " characters long");
		System.out.println("first few lines of Json Result:\n" + result.substring(0, 400));


		BasicDBList json = (BasicDBList) JSON.parse(result);
		assertTrue(String.format("should have gotten %d records, got %d instead", limit, json.size()),
				limit == json.size());
		//
		// do second request with limit=50, skip=501
		//
		sw = new StringWriter();
		pw = new PrintWriter(sw);
		when(response.getWriter()).thenReturn(pw);

		parameterMap.put("skip", new String[] { "501" });
		new MongoCrudServlet().doGet(request, response);

		String result2 = sw.getBuffer().toString().trim();
		System.out.println("Json Result As String is : " + result.length() + " characters long");
		System.out.println("first few lines of Json Result:\n" + result.substring(0, 400));

		BasicDBList json2 = (BasicDBList) JSON.parse(result2);
		assertTrue(String.format("should have gotten %d records, got %d instead", limit, json2.size()),
				limit == json2.size());

		assertFalse("should have gotten different results with different skips", json.equals(json2));

		assertTrue("json.size() should be " + limit + ", got " + json.size() + " instead", limit == json.size());
	}

	@Test
	public void testGetZipsWithLimit() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		when(response.getWriter()).thenReturn(pw);
		when(request.getPathInfo()).thenReturn("/zips");
		int limit = 50;
		@SuppressWarnings("serial")
		HashMap<String, String[]> parameterMap = new HashMap<String, String[]>() {
			{
				put("limit", new String[] { "50" });
			}
		};
		when(request.getParameterMap()).thenReturn(parameterMap);

		new MongoCrudServlet().doGet(request, response);

		String result = sw.getBuffer().toString().trim();
		System.out.println("Json Result As String is : " + result.length() + " characters long");
		System.out.println("first few lines of Json Result:\n" + result.substring(0, 400));

		BasicDBList json = (BasicDBList) JSON.parse(result);
		assertTrue("json.size() should be " + limit + ", got " + json.size() + " instead", limit == json.size());
	}

	@Test
	public void testGetZipsForCitiesAndStates() throws Exception {
		int florida = testGetZipsForStateAndCity("FL", null);
		int tampa = testGetZipsForStateAndCity("FL", "TAMPA");
		int california = testGetZipsForStateAndCity("CA", null);
		int sandiego = testGetZipsForStateAndCity("CA", "SAN DIEGO");

		assertTrue(String.format("Tampa (%d) should be fewer than Florida (%d)", tampa, florida), tampa < florida);
		assertTrue(String.format("San Diego (%d) should be fewer than California (%d)", sandiego, california),
				sandiego < california);

		assertTrue(true);
	}

	public int testGetZipsForStateAndCity(final String stateCode, final String city) throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		when(response.getWriter()).thenReturn(pw);
		when(request.getPathInfo()).thenReturn("/zips");
		@SuppressWarnings("serial")
		HashMap<String, String[]> parameterMap = new HashMap<String, String[]>() {
			{
				put("state", new String[] { stateCode });
				if (city != null)
					put("city", new String[] { city });
			}
		};
		when(request.getParameterMap()).thenReturn(parameterMap);

		new MongoCrudServlet().doGet(request, response);

		String result = sw.getBuffer().toString().trim();
		System.out.println("Json Result As String is : " + result.length() + " characters long");
		System.out.println("first few lines of Json Result:\n" + result.substring(0, 400));

		BasicDBList json = (BasicDBList) JSON.parse(result);
		if (city == null) {
			assertTrue(stateCode + "should have at least 700 zip codes", json.size() > 700);
			System.out.println(
					stateCode + " has " + json.size() + " zip codes, out of a total of " + zipDocuments.size());
		} else {
			assertTrue(city + ", " + stateCode + "should have at least 10 zip codes", json.size() > 10);
			System.out.println(city + ", " + stateCode + " has " + json.size() + " zip codes, out of a total of "
					+ zipDocuments.size());

		}
		assertTrue(true);
		return json.size();
	}

	@Test
	public void testGetAllZips() throws Exception {

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		when(response.getWriter()).thenReturn(pw);
		when(request.getPathInfo()).thenReturn("/zips");
		when(request.getRequestDispatcher("")).thenReturn(rd);

		new MongoCrudServlet().doGet(request, response);

		String result = sw.getBuffer().toString().trim();
		System.out.println("Json Result As String is : " + result.length() + " characters long");
		System.out.println("first few lines of Json Result:\n" + result.substring(0, 400));

		BasicDBList json = (BasicDBList) JSON.parse(result);
		assertEquals(String.format("Size of input array %d must match size of parsed array %d", zipDocuments.size(),
				json.size()), zipDocuments.size(), json.size());
		System.out.println("Input array and output array are both + " + zipDocuments.size());

		assertTrue(true);
	}
	/*
	 * @Test public void testGetAllSongs() throws Exception {
	 * 
	 * when(request.getParameter("user")).thenReturn("abhinav");
	 * when(request.getParameter("password")).thenReturn("passw0rd");
	 * when(request.getParameter("rememberMe")).thenReturn("Y");
	 * when(request.getPathInfo()).thenReturn("/12345/songs/test");
	 * when(request.getSession()).thenReturn(session);
	 * when(request.getRequestDispatcher("")).thenReturn(rd);
	 * 
	 * StringWriter sw = new StringWriter(); PrintWriter pw = new
	 * PrintWriter(sw);
	 * 
	 * when(response.getWriter()).thenReturn(pw);
	 * 
	 * new MongoCrudServlet().doPost(request, response);
	 * 
	 * // Verify the session attribute value //
	 * verify(session).setAttribute("user", "abhinav");
	 * 
	 * verify(rd).forward(request, response);
	 * 
	 * String result = sw.getBuffer().toString().trim();
	 * 
	 * System.out.println("Result: " + result);
	 * 
	 * assertEquals("Get successfull...", result); }
	 */
}
