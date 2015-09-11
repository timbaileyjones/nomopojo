package com.linuxtampa.nomopojo.servlet;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

/**
 * Servlet implementation class MongoCrudServlet
 */
@WebServlet("/v0")
public class MongoCrudServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Logger log = Logger.getLogger(MongoCrudServlet.class);

	private MongoClientURI uri = null;
	private MongoClient client = null;
	private MongoDatabase db = null;

	public static void main(String[] args) {

	}

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MongoCrudServlet() {
		super();

		uri = new MongoClientURI("mongodb://localhost:27017/nomopojo");
		client = new MongoClient(uri);
		db = client.getDatabase(uri.getDatabase());

	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {

		Map<String, String[]> inMap = new HashMap<String, String[]>(req.getParameterMap());
		String pathInfo = req.getPathInfo();
		int statusCode = 500;

		System.out.println("pathInfo: " + pathInfo);
		try {
			response.setHeader("Content-type", "application/json");

			if (pathInfo == null || pathInfo.length() < 2)
				throw new ServletException(
						"PathInfo requires at least two path components after " + req.getServletPath());

			while (pathInfo.startsWith("/"))
				pathInfo = pathInfo.substring(1);

			String urlComponents[] = pathInfo.split("[/\\?]");
			System.out.println("urlComponents: " + Arrays.toString(urlComponents));
			if (urlComponents.length < 1)
				throw new ServletException(
						"PathInfo requires at least two path components after " + req.getServletPath());
			String collectionName = urlComponents[0];

			int limit = -1;
			int skip = -1;

			String[] skipString = inMap.get("skip");
			if (skipString != null) {
				skip = Integer.decode(skipString[0]);
				inMap.remove("skip");
			}

			String[] limitString = inMap.get("limit");
			if (limitString != null) {
				limit = Integer.decode(limitString[0]);
				inMap.remove("limit");
			}
			Bson filter = null;
			for (Entry<String, String[]> e : inMap.entrySet()) {
				String field = e.getKey().trim();
				String array[] = e.getValue();
				if (array.length == 0)
					continue;
				String expression = array[0];
				if (expression.length() == 0)
					continue;
				Bson f = null;
				if (expression.startsWith("=")) {
					expression = expression.substring(1);
					f = Filters.eq(field, expression);
				} else if (expression.startsWith("<=")) {
					expression = expression.substring(2).trim();
					f = Filters.lte(field, expression);
				} else if (expression.startsWith(">=")) {
					expression = expression.substring(2).trim();
					f = Filters.gte(field, expression);
				} else if (expression.startsWith("<")) {
					expression = expression.substring(1).trim();
					f = Filters.lt(field, expression);
				} else if (expression.startsWith(">")) {
					expression = expression.substring(1).trim();
					f = Filters.gt(field, expression);
				} else {
					f = Filters.eq(field, expression);
				}
				filter = (filter == null) ? f : Filters.and(filter, f);
			}

			MongoCollection<Document> collection = db.getCollection(collectionName);

			FindIterable<Document> find = null;
			find = collection.find();
			if (filter != null)
				find.filter(filter);
			if (limit > -1) {
				log.info("applying limit of " + limit);
				find.limit(limit);
			}
			if (skip > -1) {
				find.skip(skip);
				log.info("applying skip of " + skip);
			}

			MongoCursor<Document> cursor = find.iterator();
			Writer w = response.getWriter();
			JsonWriterSettings writerSettings = new JsonWriterSettings(true);

			try {
				w.write('[');
				while (cursor.hasNext()) {
					Document doc = cursor.next();

					w.write(doc.toJson(writerSettings));
					if (cursor.hasNext())
						w.write(',');
				}
				w.write(']');
				response.setStatus(200);
			} finally {
				cursor.close();
			}
		} catch (Exception ex) {
			response.setStatus(statusCode);
			ex.printStackTrace(response.getWriter());
			// we only want to see server errors (500+). Bad Requests are
			// 400-499 and are not this servlet's fault
			if (statusCode >= 500) {
				System.err.println("Returned Server ERROR " + 500);
				ex.printStackTrace(System.err);
			}
			throw new ServletException("caught " + ex, ex);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO implement ADD function
		
	}
	/**
	 * @see HttpServlet#doPut(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO implement UPDATE (especially the partial replace)
		
	}

}
