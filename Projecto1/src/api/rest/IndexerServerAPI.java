package api.rest;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;

import api.Document;

public interface IndexerServerAPI {
	
	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> search( @QueryParam("query") String keywords);

	@POST
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public void add(@PathParam("id")String id, Document doc);


	@DELETE
	@Path("/{id}")
	public void remove(@PathParam("id") String id);

	@DELETE
	@Path("/remove/{id}")
	public void removeDoc(@PathParam("id") String id);
		
	
	public void setUrl(String rvURL);
}
