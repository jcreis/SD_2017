package proxy.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.oauth.OAuth10aService;

import api.Endpoint;
import rest.server.IndexerServiceResources;

public abstract class ProxyServer
{
	
	private static String endpointID;
	private static String rvUrl;
	private static ProxyServerResources proxyResources = new ProxyServerResources();
	private static final int MULTICAST_PORT = 9090;
	public static String secret = "secret";
	
	public static void main(String[] args) throws Exception {
		int port = 8081;

		if( args.length > 0){
			secret = args[0];
			rvUrl = args[1];
		}
			//port = Integer.parseInt(args[0]);

		URI baseUri = UriBuilder.fromUri("https://0.0.0.0/").port(port).build();

		ResourceConfig config = new ResourceConfig();
		config.register( proxyResources );
		JdkHttpServerFactory.createHttpServer(baseUri, config, SSLContext.getDefault());
		System.err.println("SSL REST ProxyServer ready @ " + baseUri 
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
				//indexResources.setUrl(rvUrl);
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
						
						URI hostUri = UriBuilder.fromUri("https://" + InetAddress.getLocalHost().getHostAddress()).port(8081).build();
						Endpoint endpoint = new Endpoint(hostUri.toString(), Collections.emptyMap());
						String endpointID = endpoint.generateId();
						
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
			Client client = ClientBuilder.newBuilder().hostnameVerifier(new InsecureHostnameVerifier()).build();

			URI hostURI = UriBuilder.fromUri("https://" + InetAddress.getLocalHost().getHostAddress()).port(8081).build();

			URI rvURI = UriBuilder.fromUri(rvAddress).build();

			WebTarget target = client.target( rvURI );

			HashMap<String, Object> att = new HashMap<String, Object>();
			att.put("type", "rest");
			Endpoint endpoint = new Endpoint(hostURI.toString(), att);

			//Endpoint endpoint = new Endpoint(hostURI.toString(), Collections.emptyMap());

			endpointID = endpoint.generateId();

			Response response = target.path("/" + endpointID).queryParam("secret", secret)
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
	
	static public class InsecureHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}
}