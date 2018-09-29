package rest.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.client.Entity;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import api.Endpoint;
import storage.LocalVolatileStorage;

public class IndexerServiceServer {

	private static String endpointID;
	private static String rvUrl;
	private static IndexerServiceResources indexResources = new IndexerServiceResources();
	private static final int MULTICAST_PORT = 9090;

	public static void main(String[] args) throws Exception {
		int port = 8081;

		if( args.length > 0)
			rvUrl = args[0];
		//port = Integer.parseInt(args[0]);

		URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(port).build();

		ResourceConfig config = new ResourceConfig();
		config.register( indexResources );

		JdkHttpServerFactory.createHttpServer(baseUri, config);
		System.err.println("REST IndexerServiceServer ready @ " + baseUri 
				+ " :local IP = " + InetAddress.getLocalHost().getHostAddress());

		if(rvUrl != null){
			int aux = registerEndpoint(rvUrl);
			if(aux == 204){
				System.out.println(aux + ": endpoint " + rvUrl + "added.");
			}
		}

		// multicast


		final InetAddress address = InetAddress.getByName( "230.0.0.0" ) ;

		if( ! address.isMulticastAddress()) {
			System.out.println( "Use range : 224.0.0.0 -- 239.255.255.255");
		}
		MulticastSocket socket = new MulticastSocket() ;

		boolean executed = false;
		for(int i=0; !executed && i<3; i++){
			try{
				socket.setSoTimeout(5000);

				byte[] input = ("rendezvous").getBytes();
				DatagramPacket packet = new DatagramPacket( input, input.length ) ;
				packet.setAddress( address ) ;
				packet.setPort( MULTICAST_PORT ) ;
				socket.send( packet ) ;


				byte[] buffer = new byte[65536];
				DatagramPacket rvURLPacket = new DatagramPacket(buffer, buffer.length);
				socket.receive(rvURLPacket);
				rvUrl = new String(rvURLPacket.getData(), 0, rvURLPacket.getLength());

				int tmp = registerEndpoint(rvUrl);
				indexResources.setUrl(rvUrl);
				if(tmp == 204){
					executed = true;
					System.out.println(tmp + ": endpoint " + rvUrl + " added.");
				}

			}

			catch(SocketTimeoutException e) {
				if( i < 2) {
					try { // wait some time
						Thread.sleep( 5000);
					} catch( InterruptedException e1) {
						// do nothing
					}
				}
			}

		}

		t1.start();
	}





	static Thread t1 = new Thread(new Runnable() {			
		public void run() {
			while(true){
				
				try{
					
					final InetAddress mcAddress = InetAddress.getByName( "230.0.0.0" ) ;

					if( ! mcAddress.isMulticastAddress()) {
						System.out.println( "Use range : 224.0.0.0 -- 239.255.255.255");
					}
					MulticastSocket socket1 = new MulticastSocket() ;
					
					try{
						socket1.setSoTimeout(5000);
						
						URI hostUri = UriBuilder.fromUri("http://" + InetAddress.getLocalHost().getHostAddress()).port(8081).build();
						Endpoint endpoint = new Endpoint(hostUri.toString(), Collections.emptyMap());
						String endpointID = endpoint.generateId();
						System.out.println(endpointID);
						byte[] input = ("alive/" + endpointID).getBytes();
						DatagramPacket heartbeatPacket = new DatagramPacket( input, input.length ) ;
						heartbeatPacket.setAddress( mcAddress ) ;
						heartbeatPacket.setPort( MULTICAST_PORT ) ;
						socket1.send( heartbeatPacket ) ;
					}
					catch(SocketTimeoutException e){
					}
				}
				catch (IOException e1) {
				}
				
				// Re send every 3 seconds
				try {
					Thread.sleep(3000);
				} 
				catch (InterruptedException e3) {
				}
			}
		}
	});






	public static int registerEndpoint(String rvAddress){
		try{
			ClientConfig config = new ClientConfig();
			Client client = ClientBuilder.newClient(config);

			URI hostURI = UriBuilder.fromUri("http://" + InetAddress.getLocalHost().getHostAddress()).port(8081).build();

			URI rvURI = UriBuilder.fromUri(rvAddress).build();

			WebTarget target = client.target( rvURI );

			HashMap<String, Object> att = new HashMap<String, Object>();
			att.put("type", "rest");
			Endpoint endpoint = new Endpoint(hostURI.toString(), att);

			//Endpoint endpoint = new Endpoint(hostURI.toString(), Collections.emptyMap());

			endpointID = endpoint.generateId();

			Response response = target.path("/" + endpointID)
					.request()
					.post( Entity.entity( endpoint, MediaType.APPLICATION_JSON));
			System.out.println(response.getStatus());
			return response.getStatus();
		}
		catch(ProcessingException e){
			return 0;
		} 
		catch (Exception e) {
			System.out.println("erro");
			return 0;
		} 
	}
}
