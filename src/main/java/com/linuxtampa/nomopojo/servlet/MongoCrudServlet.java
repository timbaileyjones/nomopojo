package com.linuxtampa.nomopojo.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.codecs.Encoder;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;
import org.glassfish.grizzly.http.server.Request;
import org.springframework.web.servlet.handler.MappedInterceptor;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import com.mongodb.client.model.Filters.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOptions;

/**
 * Servlet implementation class MongoCrudServlet
 */
@WebServlet("/v0")
public class MongoCrudServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Logger log = Logger.getLogger(MongoCrudServlet.class);
	private StringBuilder content = new StringBuilder();

	private MongoClientURI uri = null;
	private MongoClient client = null;
	private MongoDatabase db = null;
	static private String databaseConnectionConfig = null;

	public static void main(String[] args) {

	}

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MongoCrudServlet() {
		super();

		uri = new MongoClientURI("mongodb://localhost:27017/myme_db");
		client = new MongoClient(uri);
		db = client.getDatabase(uri.getDatabase());

	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {

		PrintWriter writer = response.getWriter();

		Map<String, String[]> inMap = new HashMap<String,String[]>(req.getParameterMap());
		String pathInfo = req.getPathInfo();
		String syniverseBillingId = null;
		int statusCode = 500;

		try {
			response.setHeader("Content-type", "application/json");
			if (pathInfo == null || pathInfo.length() < 2)
				throw new ServletException(
						"PathInfo requires at least two path components after " + req.getServletPath());

			while (pathInfo.startsWith("/"))
				pathInfo = pathInfo.substring(1);

			String urlComponents[] = pathInfo.split("/");
			if (urlComponents.length < 2)
				throw new ServletException(
						"PathInfo requires at least two path components after " + req.getServletPath());
			syniverseBillingId = urlComponents[0];
			String collectionName = urlComponents[1];
			FindOptions findOptions = new FindOptions();

			Bson filter = null;
			int limit = -1;
			int skip = -1;

			String[] filterString = inMap.get("filter");
			if (filterString != null) {
				log.info("filterString: " + filterString[0]);
				filter = Filters.where(filterString[0]);
				log.info("filter JS form: " + filter);
				inMap.remove("filter");
			}

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

			MongoCollection<Document> collection = db.getCollection(collectionName);
			FindIterable<Document> find = null;
			find = filter == null ? collection.find() : collection.find(filter);
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
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
