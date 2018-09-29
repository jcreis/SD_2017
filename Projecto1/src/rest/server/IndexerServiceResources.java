package rest.server;


import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.glassfish.jersey.client.ClientConfig;

import api.*;
import api.rest.IndexerServerAPI;
import api.soap.IndexerAPI;
import api.soap.IndexerAPI.InvalidArgumentException;

import static javax.ws.rs.core.Response.Status.*;

import storage.*;
/**
 * Implementacao do servidor de rendezvous em REST 
 */
@Path("/indexer")
public class IndexerServiceResources implements IndexerServerAPI{

	Storage storage = new LocalVolatileStorage();
	String rvURL;

	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> search( @QueryParam("query") String keywords) {


		String[] kw = new String[keywords.length()]; 
		kw = keywords.split("\\+");

		List<String> keywordList = new ArrayList<String>();

		for(int i = 0; i<kw.length; i++){
			keywordList.add(kw[i]);
		}

		List<Document> docList = storage.search(keywordList);

		List<String> urlList = new ArrayList<String>();
		for(int j=0; j<docList.size(); j++){
			urlList.add(docList.get(j).getUrl());
		}

		return urlList;
	}

	@POST
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public void add(@PathParam("id")String id, Document doc) {

		boolean aux = storage.store(id, doc);
		if(aux){
			System.out.println("Document added with sucess");
		}
		else{
			System.out.println("Failed to add document");
			throw new WebApplicationException(CONFLICT);
		}
	}




	@DELETE
	@Path("/{id}")
	public void remove(@PathParam("id") String id) {


		ClientConfig config = new ClientConfig();
		Client client = ClientBuilder.newClient(config);
		URI rvAddress = UriBuilder.fromUri(rvURL).build();
		WebTarget target = client.target(rvAddress);
		Endpoint[] endpoints = null;

		try{ 
			endpoints = target.path("/")
					.request()
					.accept(MediaType.APPLICATION_JSON)
					.get(Endpoint[].class);	
		}
		catch(ProcessingException e){

		}
		catch (Exception e1) {

		} 

		boolean aux = false;
		for(int i=0; i<endpoints.length; i++){

			String targetURL = endpoints[i].getUrl();
			if(endpoints[i].getAttributes().get("type").equals("rest")){

				URI targetAddress = UriBuilder.fromUri(targetURL).build();
				WebTarget target2 = client.target(targetAddress);

				try{ 
					Response response = target2.path("/indexer/remove/" + id)
							.request()
							.delete();	
					if(response.getStatus() == 204){
						aux = true;
					}
				}
				catch(ProcessingException e){
				}
			}
			else{
				try{
					URL wsURL = new URL(targetURL + "/indexer?wsdl");
					final QName QNAME = new QName( IndexerAPI.NAMESPACE, IndexerAPI.NAME);
					Service service = Service.create( wsURL, QNAME);

					IndexerAPI indexerServ = service.getPort( IndexerAPI.class );
					
					if(indexerServ.removeDoc(id)){
						aux = true;
					}
				}
				catch(MalformedURLException e2){
					
				} 
				catch (InvalidArgumentException e3) {
					
				}
			}
		}
		
		if(!aux)
			throw new WebApplicationException(CONFLICT);
	}

	@DELETE
	@Path("/remove/{id}")
	public void removeDoc(@PathParam("id") String id){
		if( !storage.remove(id) )
			throw new WebApplicationException(NOT_FOUND);
	}

	public void setUrl(String rvURL) {
		this.rvURL = rvURL;
	}

}



