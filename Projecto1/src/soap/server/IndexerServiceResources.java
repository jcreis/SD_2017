package soap.server;

import static api.soap.IndexerAPI.NAME;
import static api.soap.IndexerAPI.NAMESPACE;
import static javax.ws.rs.core.Response.Status.CONFLICT;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jws.WebService;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.glassfish.jersey.client.ClientConfig;

import api.Document;
import api.Endpoint;
import api.soap.IndexerAPI;
import api.soap.RendezVousAPI;
import storage.*;



@WebService( serviceName = IndexerAPI.NAME,
targetNamespace = IndexerAPI.NAMESPACE,
endpointInterface = IndexerAPI.INTERFACE)

public class IndexerServiceResources implements IndexerAPI{

	static final QName QNAME = new QName( IndexerAPI.NAMESPACE, IndexerAPI.NAME);

	Storage storage = new LocalVolatileStorage();
	String rvURL;

	@Override
	public List<String> search(String keywords) throws InvalidArgumentException {
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

	@Override
	public boolean add(Document doc) throws InvalidArgumentException {

		boolean aux = storage.store(doc.id(), doc);
		if(aux){
			System.out.println("Document added with sucess");
		}
		else{
			System.out.println("Failed to add document");
		}
		return aux;
	}

	@Override
	public boolean remove(String id) throws InvalidArgumentException {

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
			
			if(endpoints[i].getAttributes().get("type").equals("soap")){
				try{
					URL wsURL = new URL(targetURL + "/indexer?wsdl");

					Service service = Service.create( wsURL, QNAME);

					IndexerAPI indexerServ = service.getPort( IndexerAPI.class );

					if(indexerServ.removeDoc(id)){
						aux = true;
					}
				}
				catch(MalformedURLException e){

				}
			}
			else{
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
		}


		return aux;
	}

	@Override
	public boolean removeDoc(String id) throws InvalidArgumentException {
		return storage.remove(id);
	}

	public void setUrl(String rvUrl) {
		this.rvURL = rvUrl;

	}

}
